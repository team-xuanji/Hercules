package team.magic.flute.hercules.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.common.util.StrFormat;
import team.magic.flute.hercules.common.util.Tuple2;
import team.magic.flute.hercules.manager.config.RunnerEnv;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.service.HerculesExecutorInfoService;
import team.magic.flute.hercules.manager.service.HerculesExecutorTasksService;
import team.magic.flute.hercules.manager.util.DTOConvertUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * If the Executor itself cannot access any external infrastructure
 * (such as MQ or Database), it can use this set of interfaces to
 * obtain and execute tasks.
 */
@RestController
@RequestMapping("/taskDispatch")
@Slf4j
@Profile({"qa","local","dev"})
public class TaskDispatchQAController {
    @Autowired
    private HerculesExecutorTasksService executorTasksService;

    @Autowired
    private HerculesExecutorInfoService executorInfoService;

    @Autowired
    private RunnerEnv runnerEnv;

    private List<HerculesRunnableTaskInfo> fetchTasks(String executorId,
                      String executorRegion,
                      Integer fetchLimit,
                      boolean encrypt){
        Tuple2<Integer,Integer> fetchRange = executorInfoService.getConsumeRange(executorId,executorRegion);
        int begin = fetchRange.getKey();
        int end = fetchRange.getValue();
        List<HerculesTaskInfo> taskInfoList = executorTasksService.list(new LambdaQueryWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getStatus, TaskStatus.INIT.name())
                .eq(HerculesTaskInfo::getExecutorRegion,executorRegion)
                .eq(HerculesTaskInfo::isEnable,true)
                .ge(HerculesTaskInfo::getBucketId,begin)
                .lt(HerculesTaskInfo::getBucketId,end)
                .isNull(HerculesTaskInfo::getOwnerId)
                .orderByAsc(HerculesTaskInfo::getInsertTime)
                .last("LIMIT "+fetchLimit));
        if(taskInfoList.isEmpty()){
            log.warn("Unable to retrieve task information.range[{}],executorId[{}],executorRegion[{}]", JacksonUtils.writeValueAsString(fetchRange),executorId,executorRegion);
            return new ArrayList<>();
        }
        return taskInfoList.stream().map(x-> DTOConvertUtils.parse2RunnableTaskInfo(x,encrypt, runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()))
                .collect(Collectors.toList());
    }


    @GetMapping("/tryFetchTasks")
    public BaseResponse<Collection<HerculesRunnableTaskInfo>> tryFetchTasks(@RequestParam("executorId") String executorId,
                                                                            @RequestParam("executorRegion") String executorRegion,
                                                                            @RequestParam(value = "fetchLimit",required = false) Integer fetchLimit){
        if(executorInfoService.getById(executorId)==null){
            return BaseResponse.fail(StrFormat.format("The executor does not exist or has not reported any information. ExecutorId = [{}]",executorId));
        }
        if(fetchLimit==null || fetchLimit<=0){
            fetchLimit = 10;
        }
        return BaseResponse.success(fetchTasks(executorId, executorRegion, fetchLimit, true));
    }


}
