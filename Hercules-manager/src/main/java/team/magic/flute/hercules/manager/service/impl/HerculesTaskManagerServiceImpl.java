package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.common.status.TaskStatus;
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
import team.magic.flute.hercules.manager.vo.SubmitOnceTypeTaskRequestVO;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
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
    public BaseResponse<HerculesRunnableTaskInfo> submitOnceTask(SubmitOnceTypeTaskRequestVO submitTaskRequestVO) {
        HerculesTaskInfo taskInfo = submitTaskRequestVO.parse2TaskInfo(runnerEnv.getHttpEncryptKey());
        String pluginGroup = submitTaskRequestVO.getPluginGroup();
        String pluginHandle = submitTaskRequestVO.getPluginHandle();
        PluginResourceInfo resourceInfo = pluginManagerService.searchPlugin(pluginHandle,pluginGroup);
        if(resourceInfo == null || resourceInfo.getResources()==null || resourceInfo.getResources().isEmpty()){
            return BaseResponse.fail(StrFormat.format("Tasks may not be submitted unless the corresponding plugin is registered.PluginGroup[{}],PluginHandle[{}]",pluginGroup,pluginHandle));
        }
        if(executorInfoService.getAllAvailableExecutorRegion().contains(taskInfo.getExecutorRegion())){
            Collection<String> whiteList = executorInfoService.getPluginHandleWhiteListByExecutorRegion(taskInfo.getExecutorRegion());
            if(whiteList == null || whiteList.isEmpty() || whiteList.contains(taskInfo.getPluginHandle())){
                executorTasksService.save(taskInfo);
                HerculesTaskInfo info = executorTasksService.getById(taskInfo.getId());
                return BaseResponse.success(DTOConvertUtils.parse2RunnableTaskInfo(info,true,runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()));
            }else{
                return BaseResponse.fail(StrFormat.format("Executor whitelist settings do not allow the execution of plugin [{}]! Whitelist information: [{}].",taskInfo.getPluginHandle(),whiteList));
            }
        }else{
            return BaseResponse.fail("No executor is available to handle this business scenario["+taskInfo.getExecutorRegion()+"]");
        }
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
