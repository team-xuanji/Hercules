package team.magic.flute.hercules.manager.vo;

import com.github.f4b6a3.uuid.alt.GUID;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;

import javax.validation.constraints.NotNull;

import static team.magic.flute.hercules.common.global.Constant.TASK_MAX_BUCKET_SIZE;
import static team.magic.flute.hercules.common.status.TaskType.ONCE;

@Data
@Accessors(chain = true)
public class SubmitOnceTypeTaskRequestVO {
    private String id;
    @NotNull
    private String executorRegion;
    private String desc;
    @NotNull
    private String pluginHandle;
    @NotNull
    private String pluginGroup;
    @NotNull
    private String context;
    private Integer maxRetryTimes;
    private String asyncRecoverContext;
    private String aesIV;
    private String hashKey;

    public HerculesTaskInfo parse2TaskInfo(String aesKey){
        RecoverStrategy recoverStrategy = null;
        if(StringUtils.isNotBlank(asyncRecoverContext) && !"{}".equals(asyncRecoverContext.trim())){
            recoverStrategy = JacksonUtils.readValue(asyncRecoverContext,RecoverStrategy.class);
        }
        String finalContext = getContext();
        if(StringUtils.isNotBlank(finalContext) && StringUtils.isNotBlank(aesIV)){
            finalContext = AESUtils.decrypt(finalContext,aesKey,aesIV);
        }
        int bucketId = RandomUtils.nextInt(1,TASK_MAX_BUCKET_SIZE);
        if(StringUtils.isNotBlank(hashKey)){
            bucketId = (Math.abs(hashKey.hashCode())%TASK_MAX_BUCKET_SIZE)+1;
        }
        return new HerculesTaskInfo()
                .setId(id!=null?id:GUID.v7().toString().replace("-",""))
                .setExecutorRegion(executorRegion)
                .setDesc(desc)
                .setPluginGroup(pluginGroup)
                .setPluginHandle(pluginHandle)
                .setFromType(ONCE.name())
                .setContext(finalContext)
                .setEnable(true)
                .setBucketId(bucketId)
                .setStatus(TaskStatus.INIT.name())
                .setAsyncRecoverContext(recoverStrategy)
                .setMaxRetryTimes(maxRetryTimes!=null?maxRetryTimes:5);
    }
}
