package team.magic.flute.hercules.manager.entity.cron.impl;

import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.manager.dao.po.HerculesCronJobs;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.entity.cron.CronTaskContext;
import team.magic.flute.hercules.manager.entity.cron.TaskUpdateInfo;
import team.magic.flute.hercules.manager.global.Constant;
import team.magic.flute.hercules.manager.util.CronUtil;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Data
@Accessors(chain = true)
@Slf4j
public class BasicTimeRangContext implements CronTaskContext {
    private Map<String,Object> baseParam;
    private String dateBeginValName = Constant.CRON_TASK_BEGIN_TIME;
    private String dateEndValName = Constant.CRON_TASK_END_TIME;
    private String snapshotBegin;
    private Long snapshotInitBackOffSecond;

    @Override
    public TaskUpdateInfo parseTaskContext(HerculesCronJobs cronJob) throws Exception{
        LocalDateTime snapshot = cronJob.getSnapshot();
        LocalDateTime oldSnapshot = snapshot;
        LocalDateTime now = LocalDateTime.now();
        if(snapshot==null){
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime dateTime = now;
            if(snapshotBegin!=null){
                dateTime = LocalDateTime.parse(snapshotBegin, formatter);
            }else if(snapshotInitBackOffSecond!=null){
                dateTime = dateTime.minusSeconds(snapshotInitBackOffSecond);
            }
            snapshot = dateTime;
            return new TaskUpdateInfo()
                    .setId(cronJob.getJobId())
                    .setSnapshotBeforeTrigger(null)
                    .setSnapshotAfterTrigger(snapshot);
        }

        if(Boolean.TRUE.equals(cronJob.getSupportMisFire())){
            return processMisFireJob(cronJob, snapshot, now, oldSnapshot);
        }else{
            return processNoMisFireJob(cronJob, snapshot, now, oldSnapshot);
        }
    }

    public TaskUpdateInfo processNoMisFireJob(HerculesCronJobs cronJob, LocalDateTime snapshot, LocalDateTime now, LocalDateTime oldSnapshot) throws ParseException {
        List<HerculesTaskInfo> tasks = new ArrayList<>();
        for(int i=0;i<cronJob.getMisFireParallelism();i++){
            if(now.isBefore(snapshot)){
                log.info("The current time [{}] is earlier than the snapshot time [{}], skipping this scheduling.",now,snapshot);
                return new TaskUpdateInfo();
            }
            LocalDateTime nextProcessTime = CronUtil.getNextTriggerTime(cronJob.getCronExpression(),
                    snapshot,
                    false);
            if(nextProcessTime==null) {
                log.warn("Unable to infer the next scheduled time, scheduled task information [{}].", JacksonUtils.writeValueAsString(cronJob));
                break;
            }
            if(snapshot.isBefore(now) && now.isBefore(nextProcessTime)){
                log.info("The scheduling compensation has met the termination condition. Current time [{}], snapshot time [{}], next scheduling time [{}].",now,snapshot,nextProcessTime);
                break;
            }
            HerculesTaskInfo taskInfo = getHerculesTaskInfo(cronJob, snapshot, nextProcessTime, nextProcessTime);
            tasks.add(taskInfo);
            snapshot = nextProcessTime;
        }
        return  new TaskUpdateInfo()
                .setId(cronJob.getJobId())
                .setSnapshotBeforeTrigger(oldSnapshot)
                .setSnapshotAfterTrigger(snapshot)
                .setDispatchTasks(tasks);
    }

    /**
     * Update to the current time upon each dispatch.
     * @param cronJob
     * @param snapshot
     * @param now
     * @param oldSnapshot
     * @return
     * @throws ParseException
     */
    public TaskUpdateInfo processMisFireJob(HerculesCronJobs cronJob, LocalDateTime snapshot, LocalDateTime now, LocalDateTime oldSnapshot) throws ParseException {
        LocalDateTime nextProcessTime = CronUtil.getNextTriggerTime(cronJob.getCronExpression(),
                snapshot,
                true);
        LocalDateTime previousProcessTime = CronUtil.getPreviousTriggerTime(cronJob.getCronExpression(),
                snapshot,
                true);
        if(nextProcessTime==null || previousProcessTime==null){
            log.warn("Unable to infer the next scheduled time, scheduled task information [{}].", JacksonUtils.writeValueAsString(cronJob));
            return new TaskUpdateInfo();
        } else if(oldSnapshot.isAfter(previousProcessTime)){
            log.info("The scheduled trigger time has not yet been reached. Expected trigger time: [{}], current time: [{}].",previousProcessTime,now);
            return new TaskUpdateInfo();
        }
        HerculesTaskInfo taskInfo = getHerculesTaskInfo(cronJob, snapshot, previousProcessTime, nextProcessTime);
        return new TaskUpdateInfo()
                .setId(cronJob.getJobId())
                .setSnapshotBeforeTrigger(snapshot)
                .setSnapshotAfterTrigger(nextProcessTime)
                .setDispatchTasks(Collections.singletonList(taskInfo));
    }

    private HerculesTaskInfo getHerculesTaskInfo(HerculesCronJobs cronJob, LocalDateTime snapshot, LocalDateTime previousProcessTime, LocalDateTime nextProcessTime) {
        Map<String,Object> context = baseParam!=null?new HashMap<>(baseParam):new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        context.put(dateBeginValName, snapshot.format(formatter));
        context.put(dateEndValName, previousProcessTime.format(formatter));
        return cronJob.parse2TaskInfo(nextProcessTime,JacksonUtils.writeValueAsString(context));
    }


}
