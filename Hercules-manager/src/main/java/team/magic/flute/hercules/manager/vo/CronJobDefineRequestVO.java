package team.magic.flute.hercules.manager.vo;

import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.manager.dao.po.HerculesCronJobs;
import team.magic.flute.hercules.manager.entity.cron.CronTaskContext;
import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class CronJobDefineRequestVO {
    @NotBlank(message = "Must specify scheduled task ID, this is the unique identifier of the scheduled task.")
    private String jobId;
    @NotBlank(message = "Must specify task business key, as required by the processor")
    private String executorRegion;
    @NotBlank(message = "Must specify plugin group ID, as required by the processor")
    private String pluginGroup;
    @NotBlank(message = "Must specify plugin name, as required by the processor")
    private String pluginHandle;

    private String desc;
    @NotBlank(message = "Must specify Cron expression")
    private String cronExpression;
    private Boolean enable;
    private LocalDateTime begin;
    private LocalDateTime end;
    private String checkpoint;
    @NotBlank(message = "Must specify scheduling context parameters")
    private String context;
    private String asyncRecoverContext;
    private Boolean supportMisFire;
    private Integer misFireParallelism;
    private Integer maxRetryTimes;
    private Integer bucketId;

    public HerculesCronJobs parse2CronJob(boolean initNecessaryFieldWithDefaultVal){
        LocalDateTime snapshot = null;
        Integer maxRetryTimes = this.maxRetryTimes;
        Integer misFireParallelism = this.misFireParallelism;
        Integer bucketId = null;
        Boolean supportMisFire = this.supportMisFire;
        Boolean enable = this.enable;
        if(initNecessaryFieldWithDefaultVal){
            LocalDateTime now = LocalDateTime.now();
            maxRetryTimes = maxRetryTimes!=null?maxRetryTimes:5;
            misFireParallelism = misFireParallelism!=null?misFireParallelism:5;
            bucketId = RandomUtils.nextInt(1,100);
            supportMisFire = supportMisFire!=null?supportMisFire:false;
            LocalDateTime begin = getBegin();
            enable = enable!=null?enable:true;
            if(begin!=null){
                snapshot = begin;
            }else{
                this.begin = now;
                snapshot = now;
            }
            if(this.end==null){
                this.end = LocalDateTime.now().minusYears(-100);
            }
        }
        CronTaskContext cronTaskContext = null;
        if(StringUtils.isNotBlank(context) && !"{}".equals(context.trim())){
            cronTaskContext = JacksonUtils.readValue(context,CronTaskContext.class);
        }
        if(cronTaskContext==null){
            throw new IllegalArgumentException("Scheduling context parameters cannot be empty");
        }
        RecoverStrategy recoverStrategy = null;
        if(StringUtils.isNotBlank(asyncRecoverContext) && !"{}".equals(asyncRecoverContext.trim())){
            recoverStrategy = JacksonUtils.readValue(asyncRecoverContext,RecoverStrategy.class);
        }

        return new HerculesCronJobs()
                .setJobId(jobId)
                .setContext(cronTaskContext)
                .setAsyncRecoverContext(recoverStrategy)
                .setSnapshot(snapshot)
                .setExecutorRegion(executorRegion)
                .setCronExpression(cronExpression)
                .setDescription(desc)
                .setEnable(enable)
                .setMaxRetryTimes(maxRetryTimes)
                .setMisFireParallelism(misFireParallelism)
                .setPluginGroup(pluginGroup)
                .setPluginHandle(pluginHandle)
                .setSupportMisFire(supportMisFire)
                .setBucketId(bucketId)
                .setBegin(begin)
                .setEnd(end);
    }
}
