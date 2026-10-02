package team.magic.flute.hercules.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.HerculesHttpCompressType;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.util.*;
import team.magic.flute.hercules.manager.config.RunnerEnv;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.service.HerculesExecutorInfoService;
import team.magic.flute.hercules.manager.service.HerculesExecutorTasksService;
import team.magic.flute.hercules.manager.util.DTOConvertUtils;
import team.magic.flute.hercules.manager.vo.FinishOneTaskRequestVO;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static team.magic.flute.hercules.common.global.Constant.*;

/**
 * If the Executor itself cannot access any external infrastructure
 * (such as MQ or Database), it can use this set of interfaces to
 * obtain and execute tasks.
 */
@RestController
@RequestMapping("/taskDispatch")
@Slf4j
public class TaskDispatchController {
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
                .last("LIMIT "+fetchLimit));
        if(taskInfoList.isEmpty()){
            log.warn("Unable to retrieve task information.range[{}],executorId[{}],executorRegion[{}]", JacksonUtils.writeValueAsString(fetchRange),executorId,executorRegion);
            return new ArrayList<>();
        }
        return taskInfoList.stream().map(x-> DTOConvertUtils.parse2RunnableTaskInfo(x,encrypt, runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()))
                .collect(Collectors.toList());
    }

    @GetMapping("/tryFetchTasksWithByteArray")
    public ResponseEntity<byte[]> tryFetchTasksWithByteArray(@RequestParam("executorId") String executorId,
                                                             @RequestParam("executorRegion") String executorRegion,
                                                             @RequestParam(value = "fetchLimit",required = false) Integer fetchLimit){
        if(executorInfoService.getById(executorId)==null){
            return  ResponseEntity.badRequest()
                    .body(StrFormat.format("The executor does not exist or has not reported any information. ExecutorId = [{}]",executorId).getBytes(StandardCharsets.UTF_8));
        }
        if(fetchLimit==null || fetchLimit<=0){
            fetchLimit = 10;
        }
        List<HerculesRunnableTaskInfo> fetchTasks = fetchTasks(executorId, executorRegion, fetchLimit, false);
        byte[] data = new byte[0];
        HttpHeaders headers;
        byte[] compressed;
        if(!fetchTasks.isEmpty()){
            data = ForyUtils.serialize(fetchTasks);
            compressed = BinaryCompressUtils.compress(data, HerculesHttpCompressType.ZSTD);
            int originalCompressedLength = compressed.length;
            String aesKey = runnerEnv.getHttpEncryptKey();
            String aesIV = AESUtils.generateIV();
            compressed = AESUtils.encrypt(compressed, aesKey, aesIV);
            headers = getHttpHeaders(data, compressed,aesIV,originalCompressedLength);
        }else{
            compressed = data;
            headers = getHttpHeaders(data, compressed,null,null);
        }
        return new ResponseEntity<>(compressed, headers, HttpStatus.OK);
    }

    private static HttpHeaders getHttpHeaders(byte[] data, byte[] compressed,String aesIV,Integer originalCompressedLength) {
        HttpHeaders headers = new HttpHeaders();
        if(aesIV!=null){
            headers.set(HERCULES_BINARY_RESP_AES_IV,aesIV);
            headers.set(HERCULES_BINARY_RESP_ORIGINALS_SIZE, String.valueOf(data.length));
            headers.set(HERCULES_BINARY_RESP_COMPRESSED_SIZE, String.valueOf(originalCompressedLength));
            headers.set(HERCULES_BINARY_RESP_COMPRESS_TYPE, HerculesHttpCompressType.ZSTD.name());
        }else{
            headers.set(HERCULES_BINARY_RESP_ORIGINALS_SIZE, String.valueOf(data.length));
            headers.set(HERCULES_BINARY_RESP_COMPRESSED_SIZE, String.valueOf(compressed.length));
            headers.set(HERCULES_BINARY_RESP_COMPRESS_TYPE, HerculesHttpCompressType.ZSTD.name());
        }
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", "attachment");
        headers.setContentLength(compressed.length);
        return headers;
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

    @PutMapping("/tryLockOneTask")
    public BaseResponse<Boolean> tryLockOneTask(@RequestParam("executorId") String executorId,
                                                @RequestParam("executorRegion") String executorRegion,
                                                @RequestParam("taskId") String taskId){
        if(executorInfoService.getById(executorId)==null){
            return BaseResponse.fail(StrFormat.format("The executor does not exist or has not reported any information. ExecutorId = [{}]",executorId));
        }
        LambdaUpdateWrapper<HerculesTaskInfo> updateWrapper = new LambdaUpdateWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getId,taskId)
                .eq(HerculesTaskInfo::getStatus, TaskStatus.INIT.name())
                .eq(HerculesTaskInfo::getExecutorRegion,executorRegion)
                .eq(HerculesTaskInfo::isEnable,true)
                .isNull(HerculesTaskInfo::getOwnerId)
                .set(HerculesTaskInfo::getOwnerId,executorId)
                .set(HerculesTaskInfo::getStatus,TaskStatus.RUNNING.name());
        return BaseResponse.success(executorTasksService.update(updateWrapper));
    }

    @PutMapping("/finishOneTask")
    public BaseResponse<Boolean> finishOneTask(@RequestBody FinishOneTaskRequestVO requestVO){
        HerculesTaskInfo herculesTaskInfo = new HerculesTaskInfo()
                .setId(requestVO.getTaskId())
                .setCheckPointInfo(requestVO.getCheckPointInfo())
                .setStatus(TaskStatus.SUCCESS.name());
        return BaseResponse.success(executorTasksService.updateById(herculesTaskInfo));
    }

    @PutMapping("/abandonOneTask")
    public BaseResponse<Boolean> abandonOneTask(@RequestParam("executorId") String executorId,
                                                @RequestParam("taskId") String taskId){
        if(executorInfoService.getById(executorId)==null){
            return BaseResponse.fail(StrFormat.format("The executor does not exist or has not reported any information. ExecutorId = [{}]",executorId));
        }
        LambdaUpdateWrapper<HerculesTaskInfo> updateWrapper = new LambdaUpdateWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getId,taskId)
                .eq(HerculesTaskInfo::getOwnerId,executorId)
                .eq(HerculesTaskInfo::getStatus, TaskStatus.RUNNING.name())
                .set(HerculesTaskInfo::getCheckPointInfo,null)
                .set(HerculesTaskInfo::getStatus,TaskStatus.INIT.name());
        return BaseResponse.success(executorTasksService.update(updateWrapper));
    }

    @PutMapping("/failOneTask")
    public BaseResponse<Boolean> failOneTask(@RequestParam("executorId") String executorId,
                                             @RequestParam("taskId") String taskId){
        if(executorInfoService.getById(executorId)==null){
            return BaseResponse.fail(StrFormat.format("The executor does not exist or has not reported any information. ExecutorId = [{}]",executorId));
        }
        HerculesTaskInfo taskInfo = executorTasksService.getById(taskId);
        if(taskInfo==null){
            return BaseResponse.fail(StrFormat.format("Task does not exist!TaskId = [{}]",taskId));
        }
        LambdaUpdateWrapper<HerculesTaskInfo> updateWrapper = new LambdaUpdateWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getId,taskId)
                .eq(HerculesTaskInfo::getOwnerId,executorId)
                .eq(HerculesTaskInfo::getStatus, TaskStatus.RUNNING.name())
                .set(HerculesTaskInfo::getCheckPointInfo,null)
                .set(HerculesTaskInfo::getStatus,TaskStatus.FAILED.name());
        return BaseResponse.success(executorTasksService.update(updateWrapper));
    }

}
