package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.status.TaskType;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.manager.config.RunnerEnv;
import team.magic.flute.hercules.manager.dao.po.HerculesCronJobs;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.exception.InternalError;
import team.magic.flute.hercules.manager.service.HerculesConJobsService;
import team.magic.flute.hercules.manager.service.HerculesCronJobManagerService;
import team.magic.flute.hercules.manager.service.HerculesExecutorTasksService;
import team.magic.flute.hercules.manager.util.DTOConvertUtils;
import team.magic.flute.hercules.manager.vo.CronJobDefineRequestVO;
import team.magic.flute.hercules.manager.vo.HerculesCronJobVO;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class HerculesCronJobManagerServiceImpl implements HerculesCronJobManagerService {

    @Autowired
    private HerculesExecutorTasksService executorTasksService;

    @Autowired
    private HerculesConJobsService cronJobService;

    @Autowired
    private RunnerEnv runnerEnv;

    @Override
    public BaseResponse<HerculesCronJobVO> createOrUpdateCronJob(CronJobDefineRequestVO requestVO) {
        HerculesCronJobs cronJob = requestVO.parse2CronJob(true);
        cronJobService.saveOrUpdate(cronJob);
        HerculesCronJobs cronJobs = cronJobService.getById(cronJob.getJobId());
        return BaseResponse.success(new HerculesCronJobVO(cronJobs,runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()));
    }

    @Override
    public BaseResponse<Boolean> enableCronJob(String id) {
        HerculesCronJobs cronJob = cronJobService.getById(id);
        if(cronJob==null){
            throw new InternalError("Scheduled task does not exist, ID="+id);
        }
        HerculesCronJobs updateEntity = new HerculesCronJobs()
                .setJobId(cronJob.getJobId())
                .setEnable(true);
        cronJobService.updateById(updateEntity);
        return BaseResponse.success(true);
    }

    @Override
    public BaseResponse<Boolean> disableCronJob(String id) {
        HerculesCronJobs cronJob = cronJobService.getById(id);
        if(cronJob==null){
            throw new InternalError("Scheduled task does not exist, ID="+id);
        }
        HerculesCronJobs updateEntity = new HerculesCronJobs()
                .setJobId(cronJob.getJobId())
                .setEnable(false);
        cronJobService.updateById(updateEntity);
        return BaseResponse.success(true);
    }

    @Override
    public BaseResponse<Boolean> deleteCronJob(String id) {
        HerculesCronJobs cronJob = cronJobService.getById(id);
        if(cronJob==null){
            throw new InternalError("Scheduled task does not exist, ID="+id);
        }
        if(cronJob.getEnable()){
            throw new InternalError("Scheduled task is running, cannot delete, ID="+id);
        }
        return BaseResponse.success(cronJobService.removeById(id));
    }

    @Override
    public BaseResponse<List<HerculesRunnableTaskInfo>> showTopNCronTask(String id, int topN) {
        HerculesCronJobs cronJob = cronJobService.getById(id);
        if(cronJob==null){
            throw new InternalError("Scheduled task does not exist, ID="+id);
        }
        List<HerculesTaskInfo> tasks =  executorTasksService.list(new LambdaQueryWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getSourceId,id)
                .in(HerculesTaskInfo::getFromType, TaskType.FROM_CRON.name(),TaskType.ASYNC_RECOVER.name())
                .last(" limit "+topN)
        );
        return BaseResponse.success(
                tasks
                        .stream()
                        .map(x-> DTOConvertUtils.parse2RunnableTaskInfo(x,true,runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()))
                        .collect(Collectors.toList())
        );
    }
}
