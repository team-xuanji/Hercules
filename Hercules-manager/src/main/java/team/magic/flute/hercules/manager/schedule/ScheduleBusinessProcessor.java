package team.magic.flute.hercules.manager.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.manager.config.RecoverTableNameHandle;
import team.magic.flute.hercules.manager.dao.po.HerculesCronJobs;
import team.magic.flute.hercules.manager.dao.po.HerculesExecutorInfo;
import team.magic.flute.hercules.manager.dao.po.HerculesFailedTaskPo;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.entity.cron.CronTaskContext;
import team.magic.flute.hercules.manager.entity.cron.TaskUpdateInfo;
import team.magic.flute.hercules.manager.global.RecoverEventLevel;
import team.magic.flute.hercules.manager.service.*;
import team.magic.flute.hercules.manager.vo.AsyncRetryOneTaskRequestVO;
import team.magic.flute.hercules.manager.vo.HerculesRecoverTaskInfoVO;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static team.magic.flute.hercules.common.global.Constant.CRON_JOB_MAX_BUCKET_SIZE;

@Component
@Slf4j
public class ScheduleBusinessProcessor {

    @Autowired
    private HerculesConJobsService cronJobService;
    @Autowired
    private HerculesExecutorTasksService tasksService;
    @Autowired
    private HerculesExecutorInfoService herculesExecutorInfoService;
    @Autowired
    private HerculesFailedTaskService failedTaskService;
    @Autowired
    private ManagerInstanceCoordinator managerInstanceCoordinator;
    @Autowired
    private HerculesTaskManagerService taskManagerService;
    @Autowired
    private HerculesExecutorTasksService herculesExecutorTasksService;



    public void removeTooOldExecutorInfo(){
        try{
            log.info("Start removing expired ExecutorInfo");
            herculesExecutorInfoService.remove(new LambdaQueryWrapper<HerculesExecutorInfo>()
                    .lt(HerculesExecutorInfo::getUpdateTime, LocalDateTime.now().minusSeconds(180))
            );
        } catch (Exception e) {
            log.error("Failed to remove expired ExecutorInfo!");
            log.error(e.getMessage(),e);
        }
    }


    /**
     * Clean up tasks that have been running for more than 10 minutes and whose ownerId does not correspond to a running executor.
     */
    @Transactional(rollbackFor = Exception.class)
    public void changeDeadTaskToCancelled() {
        List<HerculesTaskInfo> deadTasks = tasksService.list(new LambdaQueryWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getStatus, TaskStatus.RUNNING.name())
                .lt(HerculesTaskInfo::getUpdateTime, LocalDateTime.now().minusSeconds(600))
                .last(" limit 100")
        );
        if(deadTasks.isEmpty()){
            log.info("No dead tasks found");
            return;
        }
        List<HerculesExecutorInfo> executorInfos = herculesExecutorInfoService.getAllExecutorInfo();
        Set<String> existsExecutorIds = executorInfos.stream().map(HerculesExecutorInfo::getExecutorId).collect(Collectors.toSet());
        if(existsExecutorIds.isEmpty()){
            log.warn("No executor id found!Do nothing!");
            return;
        }
        deadTasks = deadTasks.stream().filter(x->!existsExecutorIds.contains(x.getOwnerId())).collect(Collectors.toList());
        Set<String> change2InitIds = new HashSet<>();
        Set<String> change2CancelledIds = new HashSet<>();
        if(!deadTasks.isEmpty()){
            for (HerculesTaskInfo deadTask : deadTasks) {
                if(deadTask.getAsyncRecoverContext()!=null){
                    /*
                    * The asyncRetryOneTask method registers information with the fault-tolerant table;
                    * here, a transaction is needed to ensure as far as possible that duplicate records
                    * are not inserted.
                    * */
                    BaseResponse<HerculesRecoverTaskInfoVO> result =  taskManagerService.asyncRetryOneTask(new AsyncRetryOneTaskRequestVO()
                            .setTaskId(deadTask.getId())
                            .setErrorMessage("DEAD_TASK"));
                    if(!Objects.equals(200,result.getCode())){
                        change2InitIds.add(deadTask.getId());
                        log.warn("task recover failed,msg [{}],deadTaskInfo[{}]",result.getMsg(), JacksonUtils.writeValueAsString(deadTask));
                    } else {
                        change2CancelledIds.add(deadTask.getId());
                    }
                } else {
                    change2InitIds.add(deadTask.getId());
                }
            }

            tasksService.update(new LambdaUpdateWrapper<HerculesTaskInfo>()
                    .in(HerculesTaskInfo::getId,change2CancelledIds)
                    .set(HerculesTaskInfo::getStatus, TaskStatus.CANCELLED.name())
                    .set(HerculesTaskInfo::getOwnerId, null)
                    .set(HerculesTaskInfo::getCheckPointInfo, null)
            );

            tasksService.update(new LambdaUpdateWrapper<HerculesTaskInfo>()
                    .in(HerculesTaskInfo::getId,change2InitIds)
                    .set(HerculesTaskInfo::getStatus, TaskStatus.INIT.name())
                    .set(HerculesTaskInfo::getOwnerId, null)
                    .set(HerculesTaskInfo::getCheckPointInfo, null)
            );
        }
    }

    /**
     * Directly clean up tasks that are completed and older than 30 days.
     * Clean up tasks that have failed and are older than 90 days.
     */
    public void removeTooOldTask() {
        List<HerculesTaskInfo> tooOldFiledTasks = Optional.ofNullable(tasksService.list(new LambdaQueryWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getStatus, TaskStatus.FAILED.name())
                .lt(HerculesTaskInfo::getUpdateTime, LocalDateTime.now().minusDays(90))
                .last(" limit 100")
        )).orElse(new ArrayList<>());
        if(!tooOldFiledTasks.isEmpty()){
            tasksService.removeBatchByIds(tooOldFiledTasks.stream()
                    .map(HerculesTaskInfo::getId)
                    .collect(Collectors.toList()));
        }

        List<HerculesTaskInfo> tooOldSuccessTasks = tasksService.list(new LambdaQueryWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getStatus, TaskStatus.SUCCESS.name())
                .lt(HerculesTaskInfo::getUpdateTime, LocalDateTime.now().minusDays(30))
                .last(" limit 100")
        );
        if(!tooOldSuccessTasks.isEmpty()){
            tasksService.removeBatchByIds(tooOldSuccessTasks.stream()
                    .map(HerculesTaskInfo::getId)
                    .collect(Collectors.toList()));
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void recoverEvents(RecoverEventLevel eventLevel){
        try{
            RecoverTableNameHandle.setSuffix(eventLevel.name());
            List<String> runnerIds = managerInstanceCoordinator.getAllRunners();
            String currentRunnerId = managerInstanceCoordinator.getCurrentRunnerId();
            int bucketIndex = runnerIds.indexOf(currentRunnerId);
            int totalRunners = runnerIds.size();
            if(totalRunners<1 || bucketIndex==-1){
                return;
            }
            int subBucketSize = (CRON_JOB_MAX_BUCKET_SIZE+totalRunners-1) / totalRunners;
            int begin = (bucketIndex * subBucketSize) + 1;
            int end = Math.min(((bucketIndex + 1) * subBucketSize) + 1, CRON_JOB_MAX_BUCKET_SIZE+1);
            if(begin>CRON_JOB_MAX_BUCKET_SIZE){
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            List<HerculesFailedTaskPo> tasks = failedTaskService.list(new LambdaQueryWrapper<HerculesFailedTaskPo>()
                    .le(HerculesFailedTaskPo::getNextProcessTime,now)
                    .ge(HerculesFailedTaskPo::getBucketId,begin)
                    .lt(HerculesFailedTaskPo::getBucketId,end)
                    .last(" LIMIT 10")
            );
            Collection<HerculesTaskInfo> newTasks = tasks.stream()
                    .map(HerculesFailedTaskPo::getTaskInfo)
                    .filter(Objects::nonNull)
                    .peek(x-> x.setId(null))
                    .collect(Collectors.toList());
            if(!newTasks.isEmpty()){
                if(herculesExecutorTasksService.saveBatch(newTasks)){
                    failedTaskService.removeBatchByIds(tasks.stream().map(HerculesFailedTaskPo::getId).collect(Collectors.toList()));
                }else{
                    log.error("Failed to recover failed tasks! Unable to restore failed tasks to normal tasks!");
                }
            }
        }finally {
            RecoverTableNameHandle.remove();
        }
    }


    public void cronJobProcess() throws Exception {
        List<String> runnerIds = managerInstanceCoordinator.getAllRunners();
        String currentRunnerId = managerInstanceCoordinator.getCurrentRunnerId();
        int bucketIndex = runnerIds.indexOf(currentRunnerId);
        int totalRunners = runnerIds.size();
        if(totalRunners<1 || bucketIndex==-1){
            return;
        }
        int subBucketSize = (CRON_JOB_MAX_BUCKET_SIZE +totalRunners-1) / totalRunners;
        int begin = (bucketIndex * subBucketSize) + 1;
        int end = Math.min(((bucketIndex + 1) * subBucketSize) + 1, CRON_JOB_MAX_BUCKET_SIZE+1);
        if(begin>CRON_JOB_MAX_BUCKET_SIZE){
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        LambdaQueryWrapper<HerculesCronJobs> queryWrapper = new LambdaQueryWrapper<HerculesCronJobs>()
                .eq(HerculesCronJobs::getEnable,true)
                .le(HerculesCronJobs::getBegin,now)
                .le(HerculesCronJobs::getSnapshot,now)
                .ge(HerculesCronJobs::getEnd,now)
                .ge(HerculesCronJobs::getBucketId,begin)
                .lt(HerculesCronJobs::getBucketId,end)
                .last(" LIMIT 10");
        Collection<String> allAvailableExecutorRegion = herculesExecutorInfoService.getAllAvailableExecutorRegion();
        List<HerculesCronJobs> cronJobs =  cronJobService.list(queryWrapper);
        for (HerculesCronJobs cronJob : cronJobs) {
            String executorRegion = cronJob.getExecutorRegion();
            if(!allAvailableExecutorRegion.contains(executorRegion)){
                log.warn("No executor is available to execute this business group [{}].",executorRegion);
                continue;
            }
            Collection<String> pluginHandleWhiteList =  herculesExecutorInfoService.getPluginHandleWhiteListByExecutorRegion(executorRegion);
            if(pluginHandleWhiteList!=null && !pluginHandleWhiteList.isEmpty() && !pluginHandleWhiteList.contains(cronJob.getPluginHandle())){
                log.error("The executor has set a plugin whitelist and does not allow execution of this plugin! Executor: {}, Task: {}, Whitelist: {}, Plugin: {}",executorRegion,cronJob.getJobId(),pluginHandleWhiteList,cronJob.getPluginHandle());
                continue;
            }
            if(cronJob.getMaxRetryTimes()==null){
                cronJob.setMaxRetryTimes(5);
            }
            if(cronJob.getMisFireParallelism()==null){
                cronJob.setMisFireParallelism(5);
            }
            CronTaskContext cronTaskContext = cronJob.getContext();
            TaskUpdateInfo taskUpdateInfo = cronTaskContext.parseTaskContext(cronJob);
            if(taskUpdateInfo.getId()==null){
                return;
            }
            if(taskUpdateInfo.getDispatchTasks()!=null && !taskUpdateInfo.getDispatchTasks().isEmpty()){
                List<HerculesTaskInfo> exists =tasksService.listByIds(taskUpdateInfo.getDispatchTasks().stream().map(HerculesTaskInfo::getId)
                        .filter(Objects::nonNull).collect(Collectors.toList()));
                Set<String> existsIds = exists.stream().map(HerculesTaskInfo::getId).collect(Collectors.toSet());
                Collection<HerculesTaskInfo> newTasks = taskUpdateInfo.getDispatchTasks().stream()
                        .filter(x->!existsIds.contains(x.getId()))
                        .collect(Collectors.toList());
                if(!newTasks.isEmpty()){
                    tasksService.saveBatch(newTasks);
                }
            }
            if(StringUtils.isNotBlank(taskUpdateInfo.getCheckpointAfterTrigger()) || taskUpdateInfo.getSnapshotAfterTrigger()!=null){
                // Update scheduling task status using optimistic locking.
                LambdaUpdateWrapper<HerculesCronJobs> updateWrapper = new LambdaUpdateWrapper<>();
                if(taskUpdateInfo.getSnapshotAfterTrigger()!=null){
                    if(taskUpdateInfo.getSnapshotBeforeTrigger()==null){
                        updateWrapper.isNull(HerculesCronJobs::getSnapshot);
                    }else{
                        updateWrapper.eq(HerculesCronJobs::getSnapshot,taskUpdateInfo.getSnapshotBeforeTrigger());
                    }
                    if(taskUpdateInfo.getCheckpointAfterTrigger()!=null){
                        if(taskUpdateInfo.getCheckpointBeforeTrigger()==null){
                            updateWrapper.isNull(HerculesCronJobs::getCheckpoint);
                        }else{
                            updateWrapper.eq(HerculesCronJobs::getCheckpoint,taskUpdateInfo.getCheckpointBeforeTrigger());
                        }
                        updateWrapper.set(HerculesCronJobs::getCheckpoint,taskUpdateInfo.getCheckpointAfterTrigger());
                    }else{
                        updateWrapper.set(HerculesCronJobs::getCheckpoint,null);
                    }
                    updateWrapper.set(HerculesCronJobs::getSnapshot,taskUpdateInfo.getSnapshotAfterTrigger());
                }
                if(!cronJobService.update(updateWrapper)){
                    throw new IllegalStateException("Another executor instance has updated the scheduling task first. Please try again later!");
                }
            }
        }
    }

}
