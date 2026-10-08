package team.magic.flute.hercules.manager.util;

import com.github.f4b6a3.uuid.alt.GUID;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.status.TaskType;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;

import static team.magic.flute.hercules.common.global.Constant.TASK_MAX_BUCKET_SIZE;
import static team.magic.flute.hercules.common.status.TaskType.ONCE;

public class DTOConvertUtils {

    public static HerculesRunnableTaskInfo parse2RunnableTaskInfo(HerculesTaskInfo taskInfo, boolean encrypt, String encryptKey, String encryptIV){
        String contextInfo = taskInfo.getContext();
        if(encrypt){
            contextInfo = AESUtils.encrypt(contextInfo,encryptKey,encryptIV);
        }
        return new HerculesRunnableTaskInfo()
                .setStatus(taskInfo.getStatus())
                .setId(taskInfo.getId())
                .setDesc(taskInfo.getDesc())
                .setPluginGroup(taskInfo.getPluginGroup())
                .setPluginHandle(taskInfo.getPluginHandle())
                .setExecutorRegion(taskInfo.getExecutorRegion())
                .setContext(contextInfo)
                .setEncryptIV(encryptIV)
                .setMaxRetryTimes(taskInfo.getMaxRetryTimes())
                .setAsyncRecoverContext(taskInfo.getAsyncRecoverContext()!=null? JacksonUtils.writeValueAsString(taskInfo.getAsyncRecoverContext()) :"")
                .setFromSourceId(taskInfo.getSourceId())
                .setChainDepth(taskInfo.getChainDepth())
                .setFromType(TaskType.valueOf(taskInfo.getFromType()));
    }

    /**
     * Convert a task submission request (once-task or executor-forwarded chain task)
     * into the persisted task entity.
     *
     * <p>Both directions share {@link HerculesRunnableTaskInfo} as the wire type,
     * so executor-forwarded tasks keep their lineage ({@code fromType=FORWARD},
     * {@code fromSourceId=parent task id}) instead of being flattened to ONCE.
     *
     * @param request the submission request; {@code id} may be blank to auto-generate,
     *                and {@code fromType} may be null to default to {@link TaskType#ONCE}
     * @param aesKey  channel key used to decrypt {@code context} when {@code encryptIV} is present
     * @param chainDepth forward-chain depth derived by the caller from the parent task
     *                   (0 for non-chain tasks); never trusted from the request body
     */
    public static HerculesTaskInfo parse2TaskInfo(HerculesRunnableTaskInfo request, String aesKey, int chainDepth){
        RecoverStrategy recoverStrategy = null;
        if(StringUtils.isNotBlank(request.getAsyncRecoverContext()) && !"{}".equals(request.getAsyncRecoverContext().trim())){
            recoverStrategy = JacksonUtils.readValue(request.getAsyncRecoverContext(),RecoverStrategy.class);
        }
        String finalContext = request.getContext();
        if(StringUtils.isNotBlank(finalContext) && StringUtils.isNotBlank(request.getEncryptIV())){
            finalContext = AESUtils.decrypt(finalContext,aesKey,request.getEncryptIV());
        }
        int bucketId = RandomUtils.nextInt(1,TASK_MAX_BUCKET_SIZE);
        if(StringUtils.isNotBlank(request.getHashKey())){
            /*
             * floorMod, not Math.abs(hashCode()): Integer.MIN_VALUE's absolute
             * value is itself, which would push bucketId out of range.
             * floorMod always yields 0..TASK_MAX_BUCKET_SIZE-1 for a positive modulus.
             * */
            bucketId = Math.floorMod(request.getHashKey().hashCode(),TASK_MAX_BUCKET_SIZE)+1;
        }
        TaskType fromType = request.getFromType()!=null?request.getFromType():ONCE;
        return new HerculesTaskInfo()
                /*
                 * Blank id (empty string) must fall through to auto-generation:
                 * persisting "" as a task id would collide across submissions and,
                 * worse, the idempotent-replay path would return the wrong task.
                 * */
                .setId(StringUtils.isNotBlank(request.getId())?request.getId():GUID.v7().toString().replace("-",""))
                .setExecutorRegion(request.getExecutorRegion())
                .setDesc(request.getDesc())
                .setPluginGroup(request.getPluginGroup())
                .setPluginHandle(request.getPluginHandle())
                .setFromType(fromType.name())
                .setContext(finalContext)
                .setEnable(true)
                .setSourceId(request.getFromSourceId())
                .setChainDepth(chainDepth)
                .setBucketId(bucketId)
                .setStatus(TaskStatus.INIT.name())
                .setAsyncRecoverContext(recoverStrategy)
                .setMaxRetryTimes(request.getMaxRetryTimes()!=null?request.getMaxRetryTimes():5);
    }

}
