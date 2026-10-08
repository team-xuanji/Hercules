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
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.EnumResponseType;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.http.PluginDesc;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.common.plugin.TaskPlugin;
import team.magic.flute.hercules.common.status.TaskExecutionContext;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.status.TaskType;
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
import java.util.concurrent.*;

@Service
@Slf4j
public class ExecutorProcessHandleImpl implements ExecutorProcessHandle, Closeable {
    @Autowired
    private RunnerEnv runnerEnv;
    private ThreadPoolExecutor executor;
    private volatile HikariDataSource dataSource;
    @Autowired
    private HerculesManagerApi managerApi;
    @Autowired
    private DownloaderManager downloaderManager;
    @Resource(name = "OSS_FILE_STORAGE")
    private FileStorage fileStorage;

    private Cache<String,String> pluginVersionCache;

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
        Map<String,TaskPlugin> subPluginMap = new ConcurrentHashMap<>();
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
        initPlugin(pluginResourceInfo.getPluginGroup());
        pluginVersionCache.put(pluginResourceInfo.getPluginGroup(),pluginResourceInfo.getVersionId());
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
        try{
            rebuildDuckdb();
            Integer maxRetryTimes = taskInfo.getMaxRetryTimes();
            if(maxRetryTimes==null){
                maxRetryTimes = 3;
                taskInfo.setMaxRetryTimes(maxRetryTimes);
            }
            Map<String, TaskPlugin> pluginMap = taskPluginContext.getLatestPluginMap(taskInfo.getPluginGroup());
            TaskPlugin plugin = pluginMap.get(taskInfo.getPluginHandle());
            if(plugin!=null && queueCapacity()>0){
                executor.execute(()->process(plugin,taskInfo));
            } else {
                String tooBusyLogMsg = StrFormat.format("The executor is busy and cannot process new tasks. Task ID[{}]",taskInfo.getId());
                String pluginNotFoundMsg = StrFormat.format("Plugin[{}] not found and cannot process new tasks. Task ID[{}]",taskInfo.getPluginHandle(),taskInfo.getId());
                String msg = plugin==null?pluginNotFoundMsg:tooBusyLogMsg;
                log.warn(msg);
                tryDomain(maxRetryTimes,()->abandon(taskInfo),ExecutorTaskOps.ABANDON);
            }
        } catch (Exception e){
            log.error("Task execution failed,try abandon this task,taskId=[{}]",taskInfo.getId(),e);
            abandon(taskInfo);
        }
    }

    private void rebuildDuckdb() {
        if(runnerEnv.isEnableDuckdb() && System.currentTimeMillis()>duckdbConnectionTTL && aliveAbleSlot()==totalSlot()){
            rebuildDuckdbDataSource();
        }
    }

    private void tryForward(HerculesRunnableTaskInfo forwardRequest, Set<String> forwardInfoCache){
        BaseResponse<HerculesRunnableTaskInfo> resp =  managerApi.submitOnceTask(forwardRequest);
        if(!Objects.equals(200,resp.getCode())){
            throw new RuntimeException("Chain call failed!"+resp.getMsg());
        }
        forwardInfoCache.add(forwardRequest.getId());
    }

    private <E> E tryDomain(int maxRetryTimes, Callable<E> task,ExecutorTaskOps taskDomain) throws Exception {
        Exception lastException = new RuntimeException();
        for(int i=0;i<maxRetryTimes;i++){
            try{
                return task.call();
            }catch (Exception e){
                lastException = e;
                log.error("Task execution failed,domain=[{}], retry count [{}]",taskDomain,i);
                log.error(e.getMessage(),e);
                if(i==maxRetryTimes-1){
                    throw e;
                }
                TimeUnit.SECONDS.sleep(1);
            }
        }
        throw lastException;
    }

    private TaskExecutionContext buildTaskExecutionContext(HerculesRunnableTaskInfo taskInfo) throws Exception {
        TaskExecutionContext taskExecutionContext = new TaskExecutionContext();
        if(runnerEnv.isEnableDuckdb()){
            Connection duckdbConnection = dataSource.getConnection();
            taskExecutionContext.setDuckdbConnection(duckdbConnection);
        }
        taskExecutionContext.setId(taskInfo.getId());
        taskExecutionContext.setExecutionContext(taskInfo.getContext());
        return taskExecutionContext;
    }

    private void process(TaskPlugin plugin,HerculesRunnableTaskInfo taskInfo) {
        boolean processSuccess = false;
        TaskExecutionContext taskExecutionContext = null;
        int maxRetryTimes = taskInfo.getMaxRetryTimes();
        try{
            taskExecutionContext = buildTaskExecutionContext(taskInfo);
            TaskExecutionContext finalTaskExecutionContext = taskExecutionContext;
            processSuccess = tryDomain(maxRetryTimes,()-> executePlugin(finalTaskExecutionContext,taskInfo,plugin),ExecutorTaskOps.PROCESS);
            Set<String> forwardInfoCache = new HashSet<>();
            if(taskExecutionContext.getForwardRequest()!=null && !taskExecutionContext.getForwardRequest().isEmpty()){
                tryDomain(maxRetryTimes,()->tryForwardTask(finalTaskExecutionContext,forwardInfoCache),ExecutorTaskOps.FORWARD);
            }
            tryDomain(maxRetryTimes,()->success(finalTaskExecutionContext),ExecutorTaskOps.FINISH);
        }catch(Exception e){
            log.error("Task execution failed, taskId [{}]",taskInfo.getId(),e);
            if(!processSuccess){
                // try one last time.
                try {
                    tryDomain(maxRetryTimes,()->failed(taskInfo),ExecutorTaskOps.FAIL);
                    tryAsyncRecover(taskInfo, e);
                }catch(Exception ex) {
                    log.error("Can not report task status.Task is Dead,TaskId [{}]",taskInfo.getId(),ex);
                }
            }else{
                log.error("Task executed but can not report info into managerApi.Task is Dead,TaskId [{}]",taskInfo.getId(),e);
            }
        }finally {
            if(runnerEnv.isEnableDuckdb() && taskExecutionContext!=null){
                cleanUpDuckdbConnection(taskExecutionContext);
            }
            IOUtils.closeQuietly(taskExecutionContext);
        }
    }

    private boolean tryForwardTask(TaskExecutionContext taskExecutionContext,Set<String> forwardInfoCache) {
        if(taskExecutionContext!=null && taskExecutionContext.getForwardRequest()!=null){
            for (HerculesRunnableTaskInfo request : taskExecutionContext.getForwardRequest()) {
                if(StringUtils.isBlank(request.getFromSourceId())){
                    request.setFromSourceId(taskExecutionContext.getId());
                }
                if(request.getFromType() == null){
                    request.setFromType(TaskType.FORWARD);
                }
                /*
                 * Blank (empty) ids also derive the default: an explicit "" would
                 * otherwise be submitted as-is and collide across distinct forwards.
                 * */
                if(StringUtils.isBlank(request.getId())){
                    request.setId(request.buildDefaultUniId());
                }
                if(!forwardInfoCache.contains(request.getId())){
                    tryForward(request, forwardInfoCache);
                }
            }
        }
        return true;
    }

    private void tryAsyncRecover(HerculesRunnableTaskInfo taskInfo, Exception e) {
        if(StringUtils.isNotBlank(taskInfo.getAsyncRecoverContext()) && !"{}".equals(taskInfo.getAsyncRecoverContext().trim())){
            BaseResponse<PluginResourceInfo> response = managerApi.asyncRerunOneTask(new AsyncRetryOneTaskRequestVO()
                    .setTaskId(taskInfo.getId())
                    .setErrorMessage(Objects.toString(e.getMessage()).substring(0,500)));
            if(response.getCode()!= EnumResponseType.SUCCESS.getCode()){
                log.error("Asynchronous retry failed! Task ID [{}], Error message [{}]", taskInfo.getId(),response.getMsg());
            }
        }
    }

    private boolean executePlugin(TaskExecutionContext taskExecutionContext,
                               HerculesRunnableTaskInfo taskInfo,
                               TaskPlugin plugin) throws Exception {
        try{
            plugin.execute(taskExecutionContext);
            return true;
        } catch (Exception e) {
            log.error("Task execution failed, Plugin ID[{}]",taskInfo.getId(),e);
            throw e;
        }
    }

    private void cleanUpDuckdbConnection(TaskExecutionContext taskExecutionContext) {
        try{
            Connection duckdbConnection = taskExecutionContext.getDuckdbConnection();
            taskExecutionContext.setDuckdbConnection(null);
            if(duckdbConnection!=null && !duckdbConnection.isClosed()){
                duckdbConnection.close();
            }
        }catch(Exception e){
            // The JNI of duckdb is not necessarily stable. So do not use try-with-resource.
            log.error("DuckDB close failed, rebuilding", e);
            rebuildDuckdbDataSource();
        }
    }

    private boolean abandon(HerculesRunnableTaskInfo taskInfo) {
        HerculesRunnableTaskInfo oldInfo =  searchTask(taskInfo.getId());
        if(oldInfo==null){
            throw new IllegalArgumentException("Task ["+taskInfo.getId()+"] is not found!");
        }
        if(TaskStatus.INIT.name().equals(oldInfo.getStatus())){
            return true;
        }
        BaseResponse<Boolean> result = managerApi.abandonOneTask(
                taskInfo.getId(),
                runnerEnv.getRunnerInstanceId(),
                ExecutorInfoUtils.getExecutorSign(runnerEnv.getRunnerIdentityId(),taskInfo.getId(), ExecutorTaskOps.ABANDON)
        );
        if(result.getCode()!=200 || !Boolean.TRUE.equals(result.getData())){
            throw new RuntimeException(result.getMsg());
        }
        return true;
    }

    private boolean failed(HerculesRunnableTaskInfo taskInfo) {
        HerculesRunnableTaskInfo oldInfo =  searchTask(taskInfo.getId());
        if(oldInfo==null){
            throw new IllegalArgumentException("Task ["+taskInfo.getId()+"] is not found!");
        }
        if(TaskStatus.FAILED.name().equals(oldInfo.getStatus())){
            return true;
        }
        BaseResponse<Boolean> result = managerApi.failOneTask(
                taskInfo.getId(),
                runnerEnv.getRunnerInstanceId(),
                ExecutorInfoUtils.getExecutorSign(runnerEnv.getRunnerIdentityId(),taskInfo.getId(), ExecutorTaskOps.FAIL));
        if(result.getCode()!=200 || !Boolean.TRUE.equals(result.getData())){
            throw new RuntimeException(result.getMsg());
        }
        return true;
    }

    private HerculesRunnableTaskInfo searchTask(String taskId){
        BaseResponse<HerculesRunnableTaskInfo> result = managerApi.checkTaskStatus(taskId);
        if(result.getCode()!=200){
            throw new RuntimeException(result.getMsg());
        }
        return result.getData();
    }

    private boolean success(TaskExecutionContext taskExecutionContext) {
        HerculesRunnableTaskInfo taskInfo =  searchTask(taskExecutionContext.getId());
        if(taskInfo==null){
            throw new IllegalArgumentException("Task ["+taskExecutionContext.getId()+"] is not found!");
        }
        if(TaskStatus.SUCCESS.name().equals(taskInfo.getStatus())){
            return true;
        }
        BaseResponse<Boolean> result = managerApi.finishOneTask(new FinishOneTaskRequestVO()
                .setTaskId(taskExecutionContext.getId())
                .setCheckPointInfo(taskExecutionContext.getCheckPointResult())
                .setExecutorId(runnerEnv.getRunnerInstanceId())
                .setPassSign(ExecutorInfoUtils.getExecutorSign(runnerEnv.getRunnerIdentityId(),taskExecutionContext.getId(), ExecutorTaskOps.FINISH))
        );
        if(result.getCode()!=200 || !Boolean.TRUE.equals(result.getData())){
            String msg = StrFormat.format("Can not set task status to success,error msg = [{}],current task status = [{}],task id = [{}]",result.getMsg(),taskInfo.getStatus(),taskInfo.getId());
            throw new RuntimeException(msg);
        }
        return true;
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
