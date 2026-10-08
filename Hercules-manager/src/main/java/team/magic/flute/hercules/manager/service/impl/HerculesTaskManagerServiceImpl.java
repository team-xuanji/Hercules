package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.status.TaskType;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.common.util.StrFormat;
import team.magic.flute.hercules.manager.config.RecoverConfig;
import team.magic.flute.hercules.manager.config.RecoverTableNameHandle;
import team.magic.flute.hercules.manager.config.RunnerEnv;
import team.magic.flute.hercules.manager.dao.po.HerculesFailedTaskPo;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.global.RecoverEventLevel;
import team.magic.flute.hercules.manager.service.*;
import team.magic.flute.hercules.manager.util.DTOConvertUtils;
import team.magic.flute.hercules.manager.vo.AsyncRetryOneTaskRequestVO;
import team.magic.flute.hercules.manager.vo.HerculesRecoverTaskInfoVO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static team.magic.flute.hercules.common.status.TaskType.FORWARD;

@Service
@Slf4j
public class HerculesTaskManagerServiceImpl implements HerculesTaskManagerService {
    @Autowired
    private HerculesExecutorTasksService executorTasksService;
    @Autowired
    private HerculesFailedTaskService failedTaskService;
    @Autowired
    private HerculesExecutorInfoService executorInfoService;
    @Autowired
    private HerculesPluginManagerService  pluginManagerService;
    @Autowired
    private RunnerEnv runnerEnv;


    @Override
    public BaseResponse<HerculesRunnableTaskInfo> submitOnceTask(HerculesRunnableTaskInfo submitTaskRequest) {
        long chainDepth = resolveChainDepth(submitTaskRequest);
        if(chainDepth > runnerEnv.getMaxChainDepth()){
            return BaseResponse.fail(StrFormat.format(
                    "Chain depth limit exceeded: this forward would reach depth [{}], max allowed is [{}], parent task [{}]. Submission rejected to stop runaway chain derivation.",
                    chainDepth, runnerEnv.getMaxChainDepth(), submitTaskRequest.getFromSourceId()));
        }
        HerculesTaskInfo taskInfo = DTOConvertUtils.parse2TaskInfo(submitTaskRequest,runnerEnv.getHttpEncryptKey(),(int)chainDepth);
        String pluginGroup = submitTaskRequest.getPluginGroup();
        String pluginHandle = submitTaskRequest.getPluginHandle();
        String executorRegion = submitTaskRequest.getExecutorRegion();
        PluginResourceInfo resourceInfo = pluginManagerService.searchPlugin(pluginHandle,pluginGroup);
        if(resourceInfo == null || resourceInfo.getResources()==null || resourceInfo.getResources().isEmpty()){
            return BaseResponse.fail(StrFormat.format("Tasks may not be submitted unless the corresponding plugin is registered.PluginGroup[{}],PluginHandle[{}]",pluginGroup,pluginHandle));
        }
        if(executorInfoService.getAllAvailableExecutorRegion().contains(executorRegion)){
            Collection<String> whiteList = executorInfoService.getPluginHandleWhiteListByExecutorRegion(taskInfo.getExecutorRegion());
            if(whiteList == null || whiteList.isEmpty() || whiteList.contains(taskInfo.getPluginHandle())){
                try{
                    executorTasksService.save(taskInfo);
                }catch(DuplicateKeyException ex){
                    log.warn(ex.getMessage(),ex);
                }
                HerculesTaskInfo info = executorTasksService.getById(taskInfo.getId());
                return BaseResponse.success(DTOConvertUtils.parse2RunnableTaskInfo(info,true,runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()));
            }else{
                return BaseResponse.fail(StrFormat.format("Executor whitelist settings do not allow the execution of plugin [{}]! Whitelist information: [{}].",taskInfo.getPluginHandle(),whiteList));
            }
        }else{
            return BaseResponse.fail("No executor is available to handle this business scenario["+taskInfo.getExecutorRegion()+"]");
        }
    }

    /**
     * Derive the forward-chain depth for a submission: parent depth + 1 for
     * FORWARD tasks, 0 otherwise. Depth is always derived here — never taken
     * from the request body — so a forged depth cannot bypass the limit.
     * A FORWARD task whose parent is missing still counts as one hop.
     *
     * <p>Returns long: a parent row poisoned to Integer.MAX_VALUE (manual DB edit,
     * bad migration) must not overflow the +1 back into negative and slip past
     * the limit. A negative parent depth is likewise corrupt — report a depth
     * beyond the limit so the submission is rejected instead of propagating it.
     */
    private long resolveChainDepth(HerculesRunnableTaskInfo request){
        if(!FORWARD.equals(request.getFromType()) || StringUtils.isBlank(request.getFromSourceId())){
            return 0;
        }
        HerculesTaskInfo parent = executorTasksService.getById(request.getFromSourceId());
        if(parent==null){
            return 1;
        }
        int parentDepth = parent.getChainDepth()!=null?parent.getChainDepth():0;
        if(parentDepth < 0){
            return (long) runnerEnv.getMaxChainDepth() + 1;
        }
        return (long) parentDepth + 1;
    }

    @Override
    public BaseResponse<HerculesRunnableTaskInfo> getTaskInfo(String taskId) {
        HerculesTaskInfo taskInfo = executorTasksService.getById(taskId);
        if(taskInfo!=null){
            return BaseResponse.success(DTOConvertUtils.parse2RunnableTaskInfo(taskInfo,true,runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()));
        }
        return BaseResponse.fail();
    }

    @Override
    public BaseResponse<HerculesRunnableTaskInfo> rerunTask(String taskId) {
        HerculesTaskInfo taskInfo = executorTasksService.getById(taskId);
        if(taskInfo==null){
            return BaseResponse.fail("Task no longer exists!");
        }
        LambdaUpdateWrapper<HerculesTaskInfo> updateWrapper = new LambdaUpdateWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getId,taskId)
                .eq(HerculesTaskInfo::getStatus, TaskStatus.FAILED.name())
                .set(HerculesTaskInfo::getOwnerId,null)
                .set(HerculesTaskInfo::getStatus,TaskStatus.INIT.name())
                .set(HerculesTaskInfo::getCheckPointInfo,null);
        boolean updateSuccess = executorTasksService.update(updateWrapper);
        if(updateSuccess){
            return BaseResponse.success(DTOConvertUtils.parse2RunnableTaskInfo(executorTasksService.getById(taskId),true,runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()));
        }else {
            return BaseResponse.fail("Unable to update Task status, please check if Task is currently running.");
        }
    }

    @Override
    public BaseResponse<HerculesRecoverTaskInfoVO> asyncRetryOneTask(AsyncRetryOneTaskRequestVO requestVO) {
        try{
            String taskId = requestVO.getTaskId();
            String errorMessage = requestVO.getErrorMessage();
            HerculesTaskInfo taskInfo = executorTasksService.getById(taskId);
            if(taskInfo==null){
                return BaseResponse.fail("Task no longer exists!");
            }
            String status = taskInfo.getStatus();
            TaskStatus taskStatus = TaskStatus.valueOf(status);
            List<TaskStatus> canRetryTaskStatus = Collections.singletonList(TaskStatus.RUNNING);
            if(canRetryTaskStatus.contains(taskStatus) && taskInfo.getAsyncRecoverContext()!=null){
                HerculesFailedTaskPo failedTaskPo = taskInfo.convert2FailedTaskPo(errorMessage);
                if(failedTaskPo==null){
                    // Reached the limit, no need to retry anymore.
                    return BaseResponse.success(new HerculesRecoverTaskInfoVO(new HerculesFailedTaskPo(),null,null));
                }
                RecoverEventLevel suffix = RecoverConfig.getRecoverStrategy(failedTaskPo.getNextProcessTime());
                RecoverTableNameHandle.setSuffix(suffix.name());
                failedTaskService.save(failedTaskPo);
                HerculesFailedTaskPo result= failedTaskService.getById(failedTaskPo.getId());
                return BaseResponse.success(new HerculesRecoverTaskInfoVO(result,runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()));
            }else{
                return BaseResponse.fail("Can only asynchronously retry running tasks that are about to fail and have async retry configuration!");
            }
        }finally {
            RecoverTableNameHandle.remove();
        }
    }

}
