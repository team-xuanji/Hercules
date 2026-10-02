package team.magic.flute.hercules.manager.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.manager.dao.po.HerculesCronJobs;
import team.magic.flute.hercules.manager.entity.cron.CronTaskContext;
import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class HerculesCronJobVO {
    @JsonIgnore
    private final HerculesCronJobs cronJobs;
    private final String encryptIV;
    @JsonIgnore
    private final String encryptKey;

    public String getEncryptIV(){
        return  encryptIV;
    }

    public String getJobId() {
        return cronJobs.getJobId();
    }
    public String getExecutorRegion() {
        return cronJobs.getExecutorRegion();
    }
    public String getPluginGroup() {
        return cronJobs.getPluginGroup();
    }
    public String getPluginHandle() {
        return cronJobs.getPluginHandle();
    }
    public String getDescription() {
        return cronJobs.getDescription();
    }
    public String getCronExpression() {
        return cronJobs.getCronExpression();
    }
    public Boolean getEnable() {
        return cronJobs.getEnable();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getBegin() {
        return cronJobs.getBegin();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getEnd() {
        return cronJobs.getEnd();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getSnapshot() {
        return cronJobs.getSnapshot();
    }
    public String getCheckpoint() {
        if(StringUtils.isNotBlank(encryptKey) && StringUtils.isNotBlank(encryptIV)){
            return AESUtils.decrypt(cronJobs.getCheckpoint(),encryptKey,encryptIV);
        }
        return cronJobs.getCheckpoint();
    }
    public CronTaskContext getContext() {
        return cronJobs.getContext();
    }
    public Boolean getSupportMisFire() {
        return cronJobs.getSupportMisFire();
    }
    public Integer getMisFireParallelism() {
        return cronJobs.getMisFireParallelism();
    }
    public Integer getMaxRetryTimes() {
        return cronJobs.getMaxRetryTimes();
    }
    public RecoverStrategy getAsyncRecoverContext() {
        return cronJobs.getAsyncRecoverContext();
    }
    public Integer getBucketId() {
        return cronJobs.getBucketId();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getInsertTime() {
        return cronJobs.getInsertTime();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getUpdateTime() {
        return cronJobs.getUpdateTime();
    }
}
