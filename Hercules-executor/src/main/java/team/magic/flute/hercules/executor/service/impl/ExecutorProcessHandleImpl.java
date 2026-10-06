package team.magic.flute.hercules.executor.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.common.classloader.LocalJarURLStreamHandlerFactory;
import team.magic.flute.hercules.common.global.ExecutorTaskOps;
import team.magic.flute.hercules.common.http.PluginDesc;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.EnumResponseType;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.common.plugin.TaskPlugin;
import team.magic.flute.hercules.common.status.TaskExecutionContext;
import team.magic.flute.hercules.common.util.ExecutorInfoUtils;
import team.magic.flute.hercules.common.util.StrFormat;
import team.magic.flute.hercules.executor.api.HerculesManagerApi;
import team.magic.flute.hercules.executor.config.RunnerEnv;
import team.magic.flute.hercules.executor.config.TaskPluginContext;
import team.magic.flute.hercules.executor.download.Downloader;
import team.magic.flute.hercules.executor.download.DownloaderManager;
import team.magic.flute.hercules.executor.service.ExecutorProcessHandle;
import team.magic.flute.hercules.executor.storage.FileStorage;
import team.magic.flute.hercules.executor.vo.AsyncRetryOneTaskRequestVO;
import team.magic.flute.hercules.executor.vo.FinishOneTaskRequestVO;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class ExecutorProcessHandleImpl implements ExecutorProcessHandle, Closeable {
    @Autowired
    private RunnerEnv runnerEnv;
    private ThreadPoolExecutor executor;
    private HikariDataSource dataSource;
    @Autowired
    private HerculesManagerApi managerApi;
    @Autowired
    private DownloaderManager downloaderManager;
    @Resource(name = "OSS_FILE_STORAGE")
    private FileStorage fileStorage;

    private Cache<String,String> pluginVersionCache;
    private final Set<String> processInfoCache = ConcurrentHashMap.newKeySet();

    private TaskPluginContext taskPluginContext;

    private long duckdbConnectionTTL = System.currentTimeMillis();


    @PostConstruct
    public void init(){
        taskPluginContext = new TaskPluginContext();
        executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(runnerEnv.getTotalSlot());
        pluginVersionCache = Caffeine.newBuilder()
                /*
                * Set expiration after a fixed time since the last write or access.
                * Check every 20 seconds whether the plugin needs to be updated.
                * Why do it this way? Because there's no message queue,
                * so there's no way to broadcast notifications.
                * I didn't want to simulate the broadcast effect by frequently scanning the
                * database at high intervals. So I just went with this lazy loading approach.
                * */
                .expireAfterWrite(20,TimeUnit.SECONDS)
                // Initial cache space size
                .initialCapacity(10)
                // Maximum number of cache entries
                .maximumSize(20000)
                .build();
        //If the DuckDB connection pool is managed by a Spring-Bean, it may conflict with MyBatis MySQL.
        if(runnerEnv.isEnableDuckdb()){
            dataSource = initDuckdbDataSource();
            duckdbConnectionTTL = System.currentTimeMillis()+3600*8*1000;
        }
    }

    private HikariDataSource initDuckdbDataSource(){
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:duckdb:"+runnerEnv.getDuckdbPath()); // OR :memory:
        ds.setDriverClassName("org.duckdb.DuckDBDriver");
        ds.setMaximumPoolSize(1); // DuckDB Limitations,Do not change this
        ds.setMinimumIdle(1);  // DuckDB Limitations,Do not change this
        ds.setConnectionTimeout(7200000); // Wait up to 2 hours.
        ds.addDataSourceProperty("memory_limit",runnerEnv.getDuckdbMemGBSize()+"GB");
        ds.addDataSourceProperty("threads",runnerEnv.getDuckdbMemGBSize()*2);
        ds.addDataSourceProperty("max_temp_directory_size",runnerEnv.getDuckdbSpillGBSize()+"GB");
        ds.addDataSourceProperty("temp_directory",runnerEnv.getDuckdbSpillPath());
        ds.addDataSourceProperty("preserve_insertion_order","false");
        return ds;
    }

    public synchronized void loadPlugin(String pluginGroup) throws IOException {
        if(pluginVersionCache.getIfPresent(pluginGroup)!=null){
            return;
        }
        BaseResponse<PluginResourceInfo> pluginResource = managerApi.getPluginResource(pluginGroup,null);
        if(pluginResource==null || pluginResource.getData()==null){
            log.warn("Plugin [{}] has no Jar resource.", pluginGroup);
            return;
        }
        PluginResourceInfo pluginResourceInfo = pluginResource.getData();
        String oldVersionId = taskPluginContext.getPluginGroupLatestVersion(pluginResourceInfo.getPluginGroup());
        if(Objects.equals(oldVersionId,pluginResourceInfo.getVersionId())){
            log.warn("The plugin [{}] does not require resource updates.", pluginGroup);
            pluginVersionCache.put(pluginGroup,pluginResourceInfo.getVersionId());
            return;
        }
        log.info("Loading plugin [{}]",pluginGroup);
        Map<String,TaskPlugin> subPluginMap = new HashMap<>();
        Map<String,URI> pluginInfos = pluginResource.getData().getResources();
        log.info("Plugin group [{}] Jar resource download started", pluginResourceInfo.getPluginGroup());
        URL[] urls;
        if(pluginResourceInfo.isCanDownloadByHttp()){
            // If the remote OSS supports HTTP downloads, use HTTP for downloading.
            File tempDir = createTempDir(pluginResourceInfo.getPluginGroup());
            urls = pluginInfos.entrySet()
                    .stream()
                    .map(e -> this.downloadToLocal(e, tempDir))
                    .toArray(URL[]::new);
        }else{
            // If HTTP download is not supported, then it can only be downloaded directly from the designated OSS.
            urls = pluginInfos.entrySet()
                    .stream()
                    .map(this::downloadToLocalByOss)
                    .toArray(URL[]::new);
        }
        log.info("Plugin group [{}] Jar resource download completed.", pluginResourceInfo.getPluginGroup());
        URLClassLoader classLoader = new URLClassLoader(urls, getClass().getClassLoader(),new LocalJarURLStreamHandlerFactory());
        for (TaskPlugin plugin : ServiceLoader.load(TaskPlugin.class, classLoader)) {
            log.info("Plugin group [{}] JAR resource download completed, successfully loaded plugin [{}]", pluginResourceInfo.getPluginGroup(),plugin.getName());
            subPluginMap.put(plugin.getName(),plugin);
        }
        taskPluginContext.addContextDetail(pluginResourceInfo,classLoader,subPluginMap);
        cleanUpOldPlugin(pluginResourceInfo.getPluginGroup());
        pluginVersionCache.put(pluginResourceInfo.getPluginGroup(),pluginResourceInfo.getVersionId());
        initPlugin(pluginResourceInfo.getPluginGroup());
    }

    @Override
    public Map<String, PluginDesc> showCurrentLoadPlugin() {
        Map<String,PluginDesc> resultMap = new HashMap<>();
        for (String pluginGroup : taskPluginContext.getPluginGroups()) {
            Map<String, TaskPlugin> pluginInfoMap = taskPluginContext.getLatestPluginMap(pluginGroup);
            if (pluginInfoMap != null) {
                for (TaskPlugin taskPlugin : pluginInfoMap.values()) {
                    resultMap.put(taskPlugin.getName(),
                            new PluginDesc()
                                    .setPluginGroup(pluginGroup)
                                    .setPluginHandle(taskPlugin.getName())
                    );
                }
            }
        }
        return resultMap;
    }

    private void initPlugin(String pluginGroup){
        taskPluginContext.initLatestPluginMap(pluginGroup);
    }

    private void cleanUpOldPlugin(String pluginGroup){
        taskPluginContext.closeOldPlugins(pluginGroup);
    }

    /**
     * Reconstruct the DuckDB data source. This operation should be used with caution.
     * Generally, it is only used to clean up DuckDB connections that have encountered
     * JNI exceptions or DuckDB connections whose metadata has been corrupted by users.
     */
    private synchronized void rebuildDuckdbDataSource(){
        if(dataSource!=null){
            dataSource.close();
            if(dataSource.isClosed()){
                /*
                 * Why do we do this?
                 * Because based on practical experience,
                 * some users often register messy information in DuckDB and fail to clean it up.
                 * Over time, this affects others' usage.
                 * So, we simply restart it periodically to avoid such issues.
                 * */
                dataSource = initDuckdbDataSource();
                duckdbConnectionTTL = System.currentTimeMillis()+3600*8*1000;
                log.info("DuckDB connection pool restarted successfully, next restart in 8 hours");
            } else {
                log.warn("Unable to close the DuckDB connection pool; attempting to force close.");
                IOUtils.closeQuietly(dataSource);
                this.dataSource = initDuckdbDataSource();
                duckdbConnectionTTL = System.currentTimeMillis()+3600*8*1000;
            }
        }
    }

    @Override
    public void handle(HerculesRunnableTaskInfo taskInfo) {
        if(runnerEnv.isEnableDuckdb() && System.currentTimeMillis()>duckdbConnectionTTL && aliveAbleSlot()==totalSlot()){
            synchronized (this){
                if(runnerEnv.isEnableDuckdb() && System.currentTimeMillis()>duckdbConnectionTTL && aliveAbleSlot()==totalSlot()){
                    rebuildDuckdbDataSource();
                }
            }
        }
        if(aliveAbleSlot()>0 || executor.getQueue().size()<totalSlot()*3){
            synchronized (this) {
                if(aliveAbleSlot()>0 || executor.getQueue().size()<totalSlot()*3){
                    executor.execute(()->process(dataSource,
                            taskInfo,
                            taskPluginContext.getLatestPluginMap(taskInfo.getPluginGroup()))
                    );
                } else {
                    log.warn("The executor is busy and cannot process new tasks. Task ID[{}]",taskInfo.getId());
                    abandon(taskInfo);
                }
            }
        } else {
            log.warn("The executor is busy and cannot process new tasks. Task ID[{}]",taskInfo.getId());
            abandon(taskInfo);
        }
    }

    private void tryForward(HerculesRunnableTaskInfo forwardRequest, boolean forwardRequestMustWait){
        if(forwardRequestMustWait){
            BaseResponse<HerculesRunnableTaskInfo> resp =  managerApi.submitOnceTask(forwardRequest);
            if(!Objects.equals(200,resp.getCode())){
                throw new RuntimeException("Chain call failed!"+resp.getMsg());
            }
        }else{
            try {
                BaseResponse<HerculesRunnableTaskInfo> resp =  managerApi.submitOnceTask(forwardRequest);
                if(resp.getCode()!=200){
                    log.warn("Chain call failed!"+resp.getMsg());
                }
            }catch (Exception e){
                log.error("Chain call encountered an issue!",e);
            }
        }
    }

    private void process(DataSource duckDbDataSource,HerculesRunnableTaskInfo taskInfo,Map<String, TaskPlugin> pluginMap){
        if(processInfoCache.add(taskInfo.getId()) && pluginMap.get(taskInfo.getPluginHandle())!=null){
            log.info("Starting to process the task,id[{}],businessKey[{}]",taskInfo.getId(),taskInfo.getExecutorRegion());
            try(TaskExecutionContext taskExecutionContext = new TaskExecutionContext()){
                taskExecutionContext.setId(taskInfo.getId());
                taskExecutionContext.setExecutionContext(taskInfo.getContext());
                Integer maxRetryTimes = taskInfo.getMaxRetryTimes();
                if(maxRetryTimes==null){
                    maxRetryTimes = 3;
                }
                for(int i=0;i<maxRetryTimes;i++){
                    try{
                        if(runnerEnv.isEnableDuckdb()){
                            processWithDuckdb(duckDbDataSource,taskExecutionContext,taskInfo,pluginMap);
                        }else{
                            processWithoutDuckdb(taskExecutionContext,taskInfo,pluginMap);
                        }
                        if(taskExecutionContext.getForwardRequest()!=null){
                            for (HerculesRunnableTaskInfo request : taskExecutionContext.getForwardRequest()) {
                                tryForward(request,taskExecutionContext.isForwardRequestMustWait());
                            }
                        }
                        break;
                    }catch (Exception e){
                        log.error("Task execution failed, retry count [{}]",i);
                        log.error(e.getMessage(),e);
                        TimeUnit.SECONDS.sleep(1);
                        if(i==maxRetryTimes-1){
                            throw e;
                        }
                    }
                }
            }catch (Exception e){
                log.error("Task execution failed, Task ID[{}]",taskInfo.getId(),e);
                if(StringUtils.isNotBlank(taskInfo.getAsyncRecoverContext()) && !"{}".equals(taskInfo.getAsyncRecoverContext().trim())){
                    BaseResponse<PluginResourceInfo> response = managerApi.asyncRerunOneTask(new AsyncRetryOneTaskRequestVO()
                            .setTaskId(taskInfo.getId())
                            .setErrorMessage(Objects.toString(e.getMessage()).substring(0,500)));
                    if(response.getCode()!= EnumResponseType.SUCCESS.getCode()){
                        log.error("Asynchronous retry failed! Task ID [{}], Error message [{}]",taskInfo.getId(),response.getMsg());
                    }
                }
                failed(taskInfo);
            }finally {
                processInfoCache.remove(taskInfo.getId());
            }
        }else{
            if(pluginMap.get(taskInfo.getPluginHandle())==null){
                log.warn("Task [{}] cannot find plugin information, skipping this process. Attempting to call plugin [{}], current available plugin list [{}]",taskInfo.getId(),taskInfo.getPluginHandle(),pluginMap.keySet());
            }else{
                log.warn("Task [{}] is currently being processed by another executor, skipping this round of processing.",taskInfo.getId());
            }
        }
    }

    private void processWithoutDuckdb(TaskExecutionContext taskExecutionContext,
                                      HerculesRunnableTaskInfo taskInfo,
                                      Map<String, TaskPlugin> pluginMap) throws Exception {
        TaskPlugin plugin = pluginMap.get(taskInfo.getPluginHandle());
        if(plugin==null){
            log.warn("The plugin corresponding to [{}] was not retrieved!",taskInfo.getPluginHandle());
            abandon(taskInfo);
        }else{
            plugin.execute(taskExecutionContext);
            success(taskExecutionContext);
        }
    }

    private void processWithDuckdb(DataSource duckDbDataSource,
                                   TaskExecutionContext taskExecutionContext,
                                   HerculesRunnableTaskInfo taskInfo,
                                   Map<String, TaskPlugin> pluginMap) throws Exception {
        Connection duckdbConnection = null;
        try{
            duckdbConnection = duckDbDataSource.getConnection();
            taskExecutionContext.setDuckdbConnection(duckdbConnection);
            TaskPlugin plugin = pluginMap.get(taskInfo.getPluginHandle());
            if(plugin==null){
                log.warn("The plugin corresponding to [{}] was not retrieved!",taskInfo.getPluginHandle());
                abandon(taskInfo);
                return;
            }
            plugin.execute(taskExecutionContext);
            success(taskExecutionContext);
        } finally {
            if(duckdbConnection!=null){
                try{
                    duckdbConnection.close();
                }catch(Exception e){
                    // The JNI of duckdb is not necessarily stable. So do not use try-with-resource.
                    log.error("DuckDB close failed, rebuilding", e);
                    //todo: Doing this will cause other tasks submitted in parallel
                    // (which have already obtained references to the old dataSource) to fail.
                    // Perhaps the dataSource should not be passed in the method.
                    // But let's leave it as is for now and deal with it later.
                    rebuildDuckdbDataSource();
                }
            }
        }
    }

    private void abandon(HerculesRunnableTaskInfo taskInfo) {
        BaseResponse<Boolean> result = managerApi.abandonOneTask(
                taskInfo.getId(),
                runnerEnv.getRunnerInstanceId(),
                ExecutorInfoUtils.getExecutorSign(runnerEnv.getRunnerIdentityId(),taskInfo.getId(), ExecutorTaskOps.ABANDON)
        );
        if(result.getCode()!=200 || !Boolean.TRUE.equals(result.getData())){
            throw new RuntimeException(result.getMsg());
        }
    }

    private void failed(HerculesRunnableTaskInfo taskInfo) {
        BaseResponse<Boolean> result = managerApi.failOneTask(
                taskInfo.getId(),
                runnerEnv.getRunnerInstanceId(),
                ExecutorInfoUtils.getExecutorSign(runnerEnv.getRunnerIdentityId(),taskInfo.getId(), ExecutorTaskOps.FAIL));
        if(result.getCode()!=200 || !Boolean.TRUE.equals(result.getData())){
            throw new RuntimeException(result.getMsg());
        }
    }

    private void success(TaskExecutionContext taskExecutionContext) {
        BaseResponse<Boolean> result = managerApi.finishOneTask(new FinishOneTaskRequestVO()
                .setTaskId(taskExecutionContext.getId())
                .setCheckPointInfo(taskExecutionContext.getCheckPointResult())
                .setExecutorId(runnerEnv.getRunnerInstanceId())
                .setPassSign(ExecutorInfoUtils.getExecutorSign(runnerEnv.getRunnerIdentityId(),taskExecutionContext.getId(), ExecutorTaskOps.FINISH))
        );
        if(result.getCode()!=200 || !Boolean.TRUE.equals(result.getData())){
            throw new RuntimeException(result.getMsg());
        }
    }

    @Override
    public long totalSlot() {
        return runnerEnv.getTotalSlot();
    }

    @Override
    public long aliveAbleSlot() {
        return Math.max(0,totalSlot()-executor.getActiveCount());
    }

    @Override
    public void close() throws IOException {
        executor.shutdownNow();
    }

    /**
     * Buffer headroom for one fetch round: free slots plus the unfilled part
     * of the bounded internal queue ({@code 3 x totalSlot}).
     */
    @Override
    public long queueCapacity(){
        return Math.max(0,allowedMaxQueueSize()+totalSlot()-executor.getActiveCount());
    }

    /**
     * Saturated = pool fully active AND the queue already more than half full.
     * The consumer stretches its polling watermark on this signal, before the
     * headroom actually runs out.
     */
    @Override
    public boolean isNowBusy(){
        return aliveAbleSlot()==0 && (queueCapacity()*1.0/allowedMaxQueueSize())>0.5;
    }

    /** Upper bound of the executor's internal queue, expressed in slot units. */
    private long allowedMaxQueueSize(){
        return totalSlot()*3;
    }

    private File createTempDir(String pluginGroup) throws IOException {
        String prefix = StrFormat.format("hercules_{}", pluginGroup);
        Path tempPath = Files.createTempDirectory(prefix);
        File tempDir = tempPath.toFile();
        tempDir.deleteOnExit();
        return tempDir;
    }

    private URL downloadToLocalByOss(Map.Entry<String, URI> entry){
        try{
            String filename = entry.getKey();
            URI srcURI = entry.getValue();
            String randPrefix = UUID.randomUUID().toString().replace("-","");
            File tempFile = Files.createTempFile(randPrefix+filename,"").toFile();
            fileStorage.download2LocalFile(srcURI.getPath(),tempFile);
            tempFile.deleteOnExit();
            return tempFile.toURI().toURL();
        }catch (Exception e){
            log.error("download file error，source file={}，target file={}", entry.getValue(), entry.getKey());
            throw new RuntimeException(e);
        }
    }

    private URL downloadToLocal(Map.Entry<String, URI> entry, File dstDir) {
        String filename = entry.getKey();
        URI srcURI = entry.getValue();
        try {
            Downloader channel = downloaderManager.getDownloader(srcURI.getScheme());
            File localFile = channel.downloadToLocal(srcURI, dstDir, filename);
            localFile.deleteOnExit();
            log.info("download file success，source file={}，target file={}", srcURI, localFile);
            try {
                return localFile.toURI().toURL();
            } catch (MalformedURLException e) {
                log.error("file transform fail，source file={}", localFile);
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            log.error("download file error，source file={}，target file={}", srcURI, dstDir);
            throw new RuntimeException(e);
        }
    }

}
