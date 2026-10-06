package team.magic.flute.hercules.executor.schedule;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.magic.flute.hercules.common.executor.ExecutorCurrentLoadPluginInfo;
import team.magic.flute.hercules.common.executor.HerculesExecutorHeartbeatInfo;
import team.magic.flute.hercules.common.global.ExecutorTaskOps;
import team.magic.flute.hercules.common.http.*;
import team.magic.flute.hercules.common.util.ExecutorInfoUtils;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.executor.api.HerculesManagerApi;
import team.magic.flute.hercules.executor.config.RunnerEnv;
import team.magic.flute.hercules.executor.service.ExecutorProcessHandle;
import team.magic.flute.hercules.executor.vo.ExecutorInfoReportRequestVO;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import static team.magic.flute.hercules.common.global.Constant.BATCH_FETCH_MAX_SIZE;

/**
 * Task Consumer
 *
 * <p>This component is responsible for consuming and processing tasks from the task queue.
 * It runs as a scheduled service that periodically polls for available tasks and dispatches
 * them to the execution engine.
 *
 * <p>Key responsibilities include:
 * <ul>
 *   <li>Polling for available tasks based on business key and executor capacity</li>
 *   <li>Managing task execution lifecycle and status updates</li>
 *   <li>Coordinating with the ExecutorProcessHandle for task processing</li>
 *   <li>Handling task failures and cleanup operations</li>
 * </ul>
 *
 * <p>The consumer operates with a configurable execution interval and uses locking
 * to ensure thread-safe operation in multi-instance deployments.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Component
@Slf4j
@EnableAsync
public class TaskConsumer {

    @Autowired
    private RunnerEnv runnerEnv;
    @Autowired
    private HerculesManagerApi managerApi;
    @Autowired
    private ExecutorProcessHandle executorProcessHandle;
    private final Lock taskScheduleLock = new ReentrantLock();
    private final Lock reportLock = new ReentrantLock();
    private volatile boolean reportSuccess = false;
    private volatile long watermark = System.currentTimeMillis();


    @Async
    @SneakyThrows(Exception.class)
    @Scheduled(fixedRate = 10000,initialDelay = 1000)
    public void reportInfo(){
        if(reportLock.tryLock()){
            try{
                HerculesExecutorHeartbeatInfo requestVO = new HerculesExecutorHeartbeatInfo()
                        .setExecutorId(runnerEnv.getRunnerInstanceId())
                        .setExecutorIdentityId(runnerEnv.getRunnerIdentityId())
                        .setExecutorRegion(runnerEnv.getExecutorRegion())
                        .setExecutorRegionDesc(runnerEnv.getExecutorRegionDesc())
                        .setExecutorMaxSlot(runnerEnv.getTotalSlot())
                        .setExecutorAvailableSlot((int) executorProcessHandle.aliveAbleSlot())
                        .setExecutorLoadPluginInfo(new ExecutorCurrentLoadPluginInfo()
                                .setPluginMap(executorProcessHandle.showCurrentLoadPlugin())
                        )
                        .setEnableDuckdb(runnerEnv.isEnableDuckdb())
                        .setExecutorPluginHandleWhiteList(runnerEnv.getPluginWhiteList());
                log.debug("Start reporting executor info.info: {}", JacksonUtils.writeValueAsString(requestVO));
                ExecutorInfoReportRequestVO reportRequestVO = new ExecutorInfoReportRequestVO();
                reportRequestVO.fillRequestVO(requestVO,runnerEnv.getHttpEncryptKey());
                BaseResponse<Boolean>  result = managerApi.reportExecutorInfo(reportRequestVO);
                if(Boolean.TRUE.equals(result.getData()) && !reportSuccess){
                    reportSuccess = true;
                }
            } catch (Exception e) {
                log.error("Failed to report Executor information!",e);
            }finally {
                reportLock.unlock();
            }
        }else {
            log.debug("Failed to acquire the lock, skipping execution.");
        }
    }

    /**
     * Main task consumption method that runs periodically.
     *
     * <p>This method is scheduled to run every 10 seconds with an initial delay of 30 seconds.
     * It uses a lock to ensure only one instance processes tasks at a time, preventing
     * concurrent execution issues in distributed environments.
     *
     * <p>The method will:
     * <ul>
     *   <li>Attempt to acquire a lock for exclusive task processing</li>
     *   <li>Poll for available tasks if lock is acquired</li>
     *   <li>Dispatch tasks to the execution engine</li>
     *   <li>Handle any exceptions that occur during processing</li>
     * </ul>
     *
     * @throws Exception if task processing encounters an error
     */
    @Async
    @SneakyThrows(Exception.class)
    @Scheduled(fixedRate = 10000,initialDelay = 30000)
    public void run(){
        if(reportSuccess && taskScheduleLock.tryLock()){
            try{
                log.debug("Start executing the task.");
                process();
            }catch (Exception e){
                log.error("Task execution encountered an exception!");
                log.error(e.getMessage(),e);
                throw e;
            }finally {
                taskScheduleLock.unlock();
            }
        }else{
            log.debug("Failed to acquire the lock, skipping execution.");
        }
    }

    private void process() throws IOException {
        log.info("Start consuming task, consumer group [{}]",runnerEnv.getExecutorRegion());
        if(System.currentTimeMillis() < watermark){
            return;
        }
        if(executorProcessHandle.queueCapacity()>0){
            int fetchSize = Math.min(Math.toIntExact(executorProcessHandle.queueCapacity()),BATCH_FETCH_MAX_SIZE);
            TaskFetchResult taskInfoResp = managerApi.tryFastFetchTasks(
                    runnerEnv.getExecutorRegion(),
                    runnerEnv.getRunnerInstanceId(),
                    fetchSize,
                    runnerEnv.getHttpEncryptKey(),
                    ExecutorInfoUtils.getExecutorSign(runnerEnv.getRunnerIdentityId(),runnerEnv.getRunnerInstanceId(),ExecutorTaskOps.FETCH));
            if(taskInfoResp.isEmpty()){
                log.info("There are no tasks to process.");
                // When unable to obtain a task, reduce the fetch frequency.
                watermark = System.currentTimeMillis() + 30_000;
                return;
            }
            if(executorProcessHandle.isNowBusy()){
                log.info("This executor is too busy!");
                // When executor is too busy, reduce the fetch frequency.
                watermark = System.currentTimeMillis() + 30_000;
            }
            List<String> pluginWhiteList =  runnerEnv.getPluginWhiteList();
            List<HerculesRunnableTaskInfo> filteredTask = new ArrayList<>();
            for (HerculesRunnableTaskInfo taskInfo : taskInfoResp.getTaskInfoList()) {
                if(CollectionUtils.isNotEmpty(pluginWhiteList) && !pluginWhiteList.contains(taskInfo.getPluginHandle())){
                    log.error("The executor is configured with a whitelist, " +
                            "plugin [{}] cannot be loaded and executed by this executor. " +
                            "Whitelist information: [{}].",taskInfo.getPluginHandle(),runnerEnv.getPluginWhiteList());
                }else {
                    filteredTask.add(taskInfo);
                }
            }
            /*
             * If a cross-domain request occurs, it means that after our heartbeat report,
             * the Manager has not yet had time to re-partition the partition information for data pulling.
             * Due to some engineering compromises, for pull tasks whose pull partition information
             * cannot be calculated for the time being, the Manager will allow this data fetch task to
             * fetch data from all partitions, and then rely on the lock interface to ensure the
             * correctness of data processing.
             * At this point, to reduce the probability of conflicts, we had better process each
             * element separately.
             * */
            if(!filteredTask.isEmpty()){
                process(filteredTask,!taskInfoResp.isCrossPartition());
            }
        }
    }

    private void handleOneTask(HerculesRunnableTaskInfo taskInfo) throws IOException {
        /*
         * The group/handle mapping is validated at lock time: a task whose
         * pluginHandle has drifted from its group is cancelled by the manager
         * (PLUGIN_DRIFT) before it ever reaches an executor, so the mapping
         * carried here is trustworthy for every task that was locked
         * successfully. Mappings may still change while the task runs; old
         * classloaders are retained for in-flight tasks, so execution
         * completes on the version that was current at load time.
         * */
        executorProcessHandle.loadPlugin(taskInfo.getPluginGroup());
        /*
         * This is an asynchronous thread pool, and exception handling needs to be implemented internally.
         * Since there is no message queue and no complete ACK mechanism,
         * the manager layer must serve as a fallback to modify the status of stalled tasks to CANCELLED-STATE.
         * */
        executorProcessHandle.handle(taskInfo);
    }

    private void process(List<HerculesRunnableTaskInfo> filteredTask, boolean batchProcess) throws IOException {
        if(batchProcess){
            List<String> ids = filteredTask.stream().map(HerculesRunnableTaskInfo::getId).collect(Collectors.toList());
            BaseResponse<BatchTaskLockProcessResult> resp = managerApi.tryLockBatchTask(new BatchLockRequest()
                    .setExecutorId(runnerEnv.getRunnerInstanceId())
                    .setExecutorRegion(runnerEnv.getExecutorRegion())
                    .setPassSign(ExecutorInfoUtils.getExecutorSign(runnerEnv.getRunnerIdentityId(),ExecutorInfoUtils.buildSignSubject(ids), ExecutorTaskOps.LOCK))
                    .setTaskIds(ids));
            if(resp.getCode()!=200){
                log.error("Unable to lock the task, skipping execution. Response body[{}]", JacksonUtils.writeValueAsString(resp));
                return;
            }
            BatchTaskLockProcessResult data = resp.getData();
            for (HerculesRunnableTaskInfo taskInfo : filteredTask) {
                TaskInfoLockResult lockResult = Optional.ofNullable(data.getLockResults())
                        .orElse(new HashMap<>()).get(taskInfo.getId());
                if (TaskInfoLockResult.SUCCESS == lockResult) {
                    handleOneTask(taskInfo);
                } else {
                    log.warn("Unable to lock the task[{}], result [{}], skipping execution.", taskInfo.getId(), lockResult);
                }
            }
        }else{
            for (HerculesRunnableTaskInfo taskInfo : filteredTask) {
                BaseResponse<Boolean> result = managerApi.tryLockOneTask(
                        runnerEnv.getExecutorRegion(),
                        runnerEnv.getRunnerInstanceId(),
                        taskInfo.getId(),
                        ExecutorInfoUtils.getExecutorSign(runnerEnv.getRunnerIdentityId(),taskInfo.getId(), ExecutorTaskOps.LOCK)
                );
                if(result.getCode()!=200){
                    log.error("Unable to lock the task, skipping execution. Response body[{}]", JacksonUtils.writeValueAsString(result));
                    continue;
                }
                if(Boolean.TRUE.equals(result.getData())){
                    handleOneTask(taskInfo);
                } else {
                    log.warn("Unable to retrieve the task, skipping execution.");
                }
            }
        }

    }

}
