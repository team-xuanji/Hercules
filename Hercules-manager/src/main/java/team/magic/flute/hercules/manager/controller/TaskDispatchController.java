package team.magic.flute.hercules.manager.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import team.magic.flute.hercules.common.global.ExecutorTaskOps;
import team.magic.flute.hercules.common.http.*;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.util.*;
import team.magic.flute.hercules.manager.config.RunnerEnv;
import team.magic.flute.hercules.manager.dao.po.HerculesExecutorInfo;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.service.HerculesExecutorInfoService;
import team.magic.flute.hercules.manager.service.HerculesExecutorTasksService;
import team.magic.flute.hercules.manager.service.HerculesPluginManagerService;
import team.magic.flute.hercules.manager.util.DTOConvertUtils;
import team.magic.flute.hercules.common.http.BatchLockRequest;
import team.magic.flute.hercules.common.http.BatchTaskLockProcessResult;
import team.magic.flute.hercules.manager.vo.FinishOneTaskRequestVO;
import team.magic.flute.hercules.manager.vo.PluginRegisterImplVO;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
    @Autowired
    private HerculesPluginManagerService pluginManagerService;


    private TaskFetchResult fetchTasks(String executorId,
                      String executorRegion,
                      Integer fetchLimit,
                      boolean encrypt){
        Tuple2<Integer,Integer> fetchRange = executorInfoService.getConsumeRange(executorId,executorRegion);
        int begin = fetchRange.getKey();
        int end = fetchRange.getValue();
        boolean crossAllBucket = begin==1 && end == TASK_MAX_BUCKET_SIZE+1;
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
            return new TaskFetchResult();
        }
        List<HerculesRunnableTaskInfo> data =  taskInfoList.stream().map(x-> DTOConvertUtils.parse2RunnableTaskInfo(x,encrypt, runnerEnv.getHttpEncryptKey(), AESUtils.generateIV()))
                .collect(Collectors.toList());
        return new TaskFetchResult()
                .setCrossPartition(crossAllBucket)
                .setTaskInfoList(data);
    }

    @GetMapping("/tryFetchTasksWithByteArray")
    public ResponseEntity<byte[]> tryFetchTasksWithByteArray(@RequestParam("executorId") String executorId,
                                                             @RequestParam("executorRegion") String executorRegion,
                                                             @RequestParam(value = "fetchLimit",required = false) Integer fetchLimit,
                                                             @RequestParam("passSign") String passSign){
        if (isInvalidExecutorOp(executorId,executorId,passSign,ExecutorTaskOps.FETCH,executorRegion)) {
            return  ResponseEntity.badRequest()
                    .body("Invalid executor identity signature.".getBytes(StandardCharsets.UTF_8));

        }
        if(fetchLimit==null || fetchLimit<=0){
            fetchLimit = 10;
        }
        TaskFetchResult fetchTasks = fetchTasks(executorId, executorRegion, fetchLimit, false);
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

    @PutMapping("/tryLockOneTask")
    public BaseResponse<Boolean> tryLockOneTask(@RequestParam("executorId") String executorId,
                                                @RequestParam("passSign") String passSign,
                                                @RequestParam("executorRegion") String executorRegion,
                                                @RequestParam("taskId") String taskId){
        BaseResponse<BatchTaskLockProcessResult> processResult = tryLockBatchTask(new BatchLockRequest()
                .setExecutorId(executorId)
                .setExecutorRegion(executorRegion)
                .setPassSign(passSign)
                .setTaskIds(Lists.newArrayList(taskId)));
        if(processResult.getCode()== EnumResponseType.SUCCESS.getCode() && processResult.getData()!=null){
            BatchTaskLockProcessResult data = processResult.getData();
            TaskInfoLockResult lockResult = data.getLockResults().get(taskId);
            if (lockResult == null)
                // This should not happen in theory, because there is no corresponding logic for the tryLockBatchTask method.
                return BaseResponse.fail("Unknown Error!");
            switch (lockResult){
                case NOT_EXIST:
                    return BaseResponse.fail(StrFormat.format("Task does not exist! TaskId = [{}]", taskId));
                case PLUGIN_DRIFT:
                    return BaseResponse.fail(StrFormat.format(
                            "PLUGIN_DRIFTED: task [{}] cancelled, plugin handle not registered under its group.", taskId));
                case NOT_CLAIMED:
                    return BaseResponse.success(false);
                case REGION_MISMATCH:
                    return BaseResponse.fail(StrFormat.format("Region mismatch, task [{}] lock refused.", taskId));
                case SUCCESS:
                    return BaseResponse.success(true);
                default:
                    return BaseResponse.fail(StrFormat.format("Unexpected lock result [{}] for task [{}].", lockResult, taskId));
            }
        }else{
            return BaseResponse.fail(processResult.getMsg());
        }
    }

    /**
     * Batch-lock entry point. Verifies the executor signature (bound to the
     * sorted task-id subject, see {@code ExecutorInfoUtils.buildSignSubject}),
     * classifies every requested id, cancels tasks whose pluginHandle has
     * drifted from its group, and claims the remainder with a single
     * conditional UPDATE. One {@link TaskInfoLockResult} is reported per
     * requested id; a null {@code lockResults} map is only possible for an
     * empty request. Signing scheme: ADR-0001.
     */
    @PutMapping("/tryLockBatchTask")
    public BaseResponse<BatchTaskLockProcessResult> tryLockBatchTask(@Valid @RequestBody BatchLockRequest batchLockRequest){
        String executorId = batchLockRequest.getExecutorId();
        String passSign = batchLockRequest.getPassSign();
        String executorRegion = batchLockRequest.getExecutorRegion();
        String subject = ExecutorInfoUtils.buildSignSubject(batchLockRequest.getTaskIds());
        if (isInvalidExecutorOp(executorId,subject,passSign,ExecutorTaskOps.LOCK,executorRegion)) {
            return BaseResponse.fail("Invalid executor identity signature.");
        }
        boolean allSuccess = false;
        Set<String> taskIds = Optional.ofNullable(batchLockRequest.getTaskIds())
                .orElse(new ArrayList<>())
                .stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if(taskIds.isEmpty()){
            log.info("taskIds is empty.");
            return BaseResponse.success(new BatchTaskLockProcessResult()
                    .setAllSuccess(allSuccess));
        }
        List<HerculesTaskInfo> taskInfo = executorTasksService.list(new LambdaQueryWrapper<HerculesTaskInfo>()
                .in(HerculesTaskInfo::getId, taskIds));
        Map<String,TaskInfoLockResult> missMatchTasks = findMissMatchTasks(taskIds,taskInfo,executorRegion);
        Map<String, TaskInfoLockResult> tryLockResults = new HashMap<>(missMatchTasks);
        List<HerculesTaskInfo> inRegionTaskInfo = taskInfo.stream()
                .filter(x->!missMatchTasks.containsKey(x.getId()))
                .collect(Collectors.toList());
        if(inRegionTaskInfo.isEmpty()){
            return BaseResponse.success(new BatchTaskLockProcessResult()
                    .setAllSuccess(allSuccess)
                    .setLockResults(tryLockResults));
        }
        Set<String> pluginDriftedTasks = cancelIfDriftedTask(inRegionTaskInfo);
        pluginDriftedTasks.forEach(id -> tryLockResults.put(id, TaskInfoLockResult.PLUGIN_DRIFT));
        Set<String> correctTaskIds = inRegionTaskInfo
                .stream()
                .map(HerculesTaskInfo::getId)
                .filter(id ->!pluginDriftedTasks.contains(id))
                .collect(Collectors.toSet());
        if(!correctTaskIds.isEmpty()){
            Map<String,TaskInfoLockResult> lockResult = lockBatchTask(correctTaskIds,executorRegion,executorId);
            tryLockResults.putAll(lockResult);
            if(tryLockResults.values().stream().allMatch(r -> r == TaskInfoLockResult.SUCCESS)){
                allSuccess = true;
            }
        }
        return BaseResponse.success(new BatchTaskLockProcessResult()
                .setAllSuccess(allSuccess)
                .setLockResults(tryLockResults));
    }


    @PutMapping("/finishOneTask")
    public BaseResponse<Boolean> finishOneTask(@Valid @RequestBody FinishOneTaskRequestVO requestVO){
        if (isInvalidExecutorOp(requestVO.getExecutorId(),requestVO.getTaskId(),requestVO.getPassSign(),ExecutorTaskOps.FINISH,null)) {
            return BaseResponse.fail("Invalid executor identity signature.");
        }
        LambdaUpdateWrapper<HerculesTaskInfo> updateWrapper = new LambdaUpdateWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getId,requestVO.getTaskId())
                .eq(HerculesTaskInfo::getOwnerId,requestVO.getExecutorId())
                .eq(HerculesTaskInfo::getStatus, TaskStatus.RUNNING.name())
                .set(HerculesTaskInfo::getCheckPointInfo,requestVO.getCheckPointInfo())
                .set(HerculesTaskInfo::getStatus,TaskStatus.SUCCESS.name());
        return BaseResponse.success(executorTasksService.update(updateWrapper));
    }

    @PutMapping("/abandonOneTask")
    public BaseResponse<Boolean> abandonOneTask(@RequestParam("executorId") String executorId,
                                                @RequestParam("taskId") String taskId,
                                                @RequestParam("passSign") String passSign){
        if (isInvalidExecutorOp(executorId,taskId,passSign,ExecutorTaskOps.ABANDON,null)) {
            return BaseResponse.fail("Invalid executor identity signature.");
        }
        LambdaUpdateWrapper<HerculesTaskInfo> updateWrapper = new LambdaUpdateWrapper<HerculesTaskInfo>()
                .eq(HerculesTaskInfo::getId,taskId)
                .eq(HerculesTaskInfo::getOwnerId,executorId)
                .eq(HerculesTaskInfo::getStatus, TaskStatus.RUNNING.name())
                .set(HerculesTaskInfo::getCheckPointInfo,null)
                .set(HerculesTaskInfo::getStatus,TaskStatus.INIT.name())
                .set(HerculesTaskInfo::getOwnerId,null);
        return BaseResponse.success(executorTasksService.update(updateWrapper));
    }

    @PutMapping("/failOneTask")
    public BaseResponse<Boolean> failOneTask(@RequestParam("executorId") String executorId,
                                             @RequestParam("taskId") String taskId,
                                             @RequestParam("passSign") String passSign){
        if (isInvalidExecutorOp(executorId,taskId,passSign,ExecutorTaskOps.FAIL,null)) {
            return BaseResponse.fail("Invalid executor identity signature.");
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

    /**
     * Region check semantics: an executor's region is fixed at deployment and
     * immutable for the instance's lifetime — a restart mints a new executorId
     * and identityId and re-registers. This check binds operations to the region
     * the instance was deployed as. Who may deploy an executor as region X is
     * governed by config management / infra, same division as the admin plane.
     *
     * <p>{@code subject} is the operation subject the signature is bound to:
     * the taskId for task ops, the executorId for FETCH (which has no task).
     * It is logged as "subject" rather than "task" so fetch rejections are not
     * misread.
     */
    private boolean isInvalidExecutorOp(String executorId,
                                        String subject,
                                        String passSign,
                                        ExecutorTaskOps ops,
                                        String executorRegion){
        HerculesExecutorInfo executor = executorInfoService.getById(executorId);
        if (executor == null) {
            log.warn("[INVALID_EXECUTOR_OP] Unknown executor [{}] attempted op [{}] subject [{}].",
                    executorId, ops, subject);
            return true;
        }
        if (StringUtils.isNotBlank(executorRegion)
                && !Objects.equals(executor.getExecutorRegion(), executorRegion)) {
            log.warn("[INVALID_EXECUTOR_OP] Region mismatch: executor [{}] registered in [{}], claimed [{}], op [{}], subject [{}].",
                    executorId, executor.getExecutorRegion(), executorRegion, ops, subject);
            return true;
        }
        if (!ExecutorInfoUtils.verifyExecutorSign(executor.getIdentityId(), subject, ops, passSign)) {
            log.warn("[INVALID_EXECUTOR_OP] Signature verification failed: executor [{}], op [{}], subject [{}].",
                    executorId, ops, subject);
            return true;
        }
        return false;
    }

    private Set<String> cancelIfDriftedTask(List<HerculesTaskInfo> inRegionTaskInfo){
        Set<String> cancelSet = new HashSet<>();
        Map<String, Map<String, Set<String>>> groupedTaskInfo = inRegionTaskInfo.stream()
                .collect(Collectors.groupingBy(
                        HerculesTaskInfo::getPluginGroup,
                        Collectors.groupingBy(
                                HerculesTaskInfo::getPluginHandle,
                                Collectors.mapping(
                                        HerculesTaskInfo::getId,
                                        Collectors.toSet()))));
        Set<String> pluginDriftedTasks = new HashSet<>();
        groupedTaskInfo.forEach( (pluginGroup,innerGroup) -> {
            List<PluginRegisterImplVO> implVOList = Optional.ofNullable(pluginManagerService.searchPluginImplInfo(pluginGroup))
                    .orElse(new ArrayList<>());
            Set<String> pluginHandlersInSingleGroup = implVOList.stream().map(PluginRegisterImplVO::getPluginHandle).collect(Collectors.toSet());
            innerGroup.forEach( (pluginHandle,groupedTaskIds) -> {
                if(!pluginHandlersInSingleGroup.contains(pluginHandle)){
                    groupedTaskIds.forEach(taskId -> {
                        pluginDriftedTasks.add(taskId);
                        String info = StrFormat.format("PLUGIN_DRIFTED: handle [{}] not registered under group [{}] at lock attempt; task cancelled.",pluginHandle,pluginGroup);
                        log.warn(info);
                    });
                }
            });
        });
        if(!pluginDriftedTasks.isEmpty()){
            /*
             * Considering that users may re-register plugins and modify the mapping between
             * pluginGroup and PluginHandle, we should reject tasks that use an incorrect mapping.
             * As for tasks already sent to the executor, because the executor retains old class loaders,
             * and given that plugin registration is a low-frequency operation, the old historical tasks
             * currently executing in the executor can theoretically run to completion. Of course,
             * the executor currently retains at most 3 historical versions of old class loaders.
             * If plugins are registered frequently, the executor will rotate out old class loaders,
             * causing the executing task to fail. After the task is handled by fault tolerance,
             * it should theoretically request the tryLockOneTask method again;
             * at that point we can handle it correctly.
             * */
            String info = "PLUGIN_DRIFTED: plugin group/handle mapping no longer registered at lock attempt; task cancelled.";
            LambdaUpdateWrapper<HerculesTaskInfo> failedStatUpdateWrapper = new LambdaUpdateWrapper<HerculesTaskInfo>()
                    .in(HerculesTaskInfo::getId,pluginDriftedTasks)
                    .eq(HerculesTaskInfo::getStatus, TaskStatus.INIT.name())
                    .eq(HerculesTaskInfo::isEnable,true)
                    .set(HerculesTaskInfo::getStatus, TaskStatus.CANCELLED.name())
                    .set(HerculesTaskInfo::getOwnerId,null)
                    .set(HerculesTaskInfo::getCheckPointInfo,info);
            executorTasksService.update(failedStatUpdateWrapper);
            cancelSet.addAll(pluginDriftedTasks);
        }
        return cancelSet;
    }

    private Map<String,TaskInfoLockResult> findMissMatchTasks(Set<String> taskIds,List<HerculesTaskInfo> taskInDB,String executorRegion){
        Map<String, TaskInfoLockResult> lockResults = new HashMap<>();
        taskInDB.stream().filter(Objects::nonNull)
                .filter(x->!Objects.equals(x.getExecutorRegion(),executorRegion)).forEach(task -> {
                    lockResults.putIfAbsent(task.getId(),TaskInfoLockResult.REGION_MISMATCH);
                });
        List<HerculesTaskInfo> inRegionTaskInfo = taskInDB
                .stream()
                .filter(x->Objects.equals(x.getExecutorRegion(),executorRegion))
                .collect(Collectors.toList());
        Set<String> existsTasks =  inRegionTaskInfo.stream()
                .map(HerculesTaskInfo::getId)
                .collect(Collectors.toSet());
        Set<String> notExistsIds = new HashSet<>(taskIds);
        notExistsIds.removeAll(existsTasks);
        notExistsIds.forEach(taskId -> {
            lockResults.putIfAbsent(taskId,TaskInfoLockResult.NOT_EXIST);
        });
        return lockResults;
    }

    /**
     * Claim tasks with one conditional UPDATE (status=INIT, enabled, no owner),
     * then re-read the affected rows to classify them: rows now owned by this
     * executor are SUCCESS, anything else is reported as NOT_CLAIMED.
     *
     * <p>The UPDATE is atomic per row, so two competing executors can never
     * co-own a task. Races between the UPDATE and the re-read (e.g. a task
     * being reset by dead-task recovery in between) can only yield a
     * pessimistic NOT_CLAIMED — a safe outcome for callers to retry.
     */
    private Map<String, TaskInfoLockResult> lockBatchTask(Set<String> taskIds,String executorRegion,String executorId) {
        Map<String, TaskInfoLockResult> lockResults = new HashMap<>();
        Set<String> correctTaskIds = new HashSet<>(taskIds);
        LambdaUpdateWrapper<HerculesTaskInfo> updateWrapper = new LambdaUpdateWrapper<HerculesTaskInfo>()
                .in(HerculesTaskInfo::getId,correctTaskIds)
                .eq(HerculesTaskInfo::getStatus, TaskStatus.INIT.name())
                .eq(HerculesTaskInfo::getExecutorRegion,executorRegion)
                .eq(HerculesTaskInfo::isEnable,true)
                .isNull(HerculesTaskInfo::getOwnerId)
                .set(HerculesTaskInfo::getOwnerId,executorId)
                .set(HerculesTaskInfo::getStatus,TaskStatus.RUNNING.name());
        boolean updateSuccess = executorTasksService.update(updateWrapper);
        if(!updateSuccess){
            correctTaskIds.forEach(taskId -> {
                lockResults.putIfAbsent(taskId,TaskInfoLockResult.NOT_CLAIMED);
            });
        }else{
            List<HerculesTaskInfo> taskInfosAfterUpdate = Optional.ofNullable(executorTasksService.listByIds(correctTaskIds))
                    .orElse(new ArrayList<>());
            List<String> lockNotSuccessTaskIds = taskInfosAfterUpdate.stream().filter(x->!Objects.equals(x.getOwnerId(),executorId))
                    .map(HerculesTaskInfo::getId).collect(Collectors.toList());
            lockNotSuccessTaskIds.forEach(taskId -> {
                lockResults.putIfAbsent(taskId,TaskInfoLockResult.NOT_CLAIMED);
            });
            lockNotSuccessTaskIds.forEach(correctTaskIds::remove);
            correctTaskIds.forEach(taskId -> {
                lockResults.putIfAbsent(taskId,TaskInfoLockResult.SUCCESS);
            });
        }
        return lockResults;
    }

}
