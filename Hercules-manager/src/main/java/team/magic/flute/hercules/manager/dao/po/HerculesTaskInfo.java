package team.magic.flute.hercules.manager.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.status.TaskType;
import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.RandomUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static team.magic.flute.hercules.common.global.Constant.CRON_JOB_MAX_BUCKET_SIZE;

@Data
@Accessors(chain = true)
@TableName(value = "HERCULES_TASK_INFO", autoResultMap = true)
public class HerculesTaskInfo {
    @TableId(value = "TASK_ID",type = IdType.ASSIGN_ID)
    private String id;
    @TableField(value = "EXECUTOR_REGION")
    private String executorRegion;
    @TableField(value = "PLUGIN_GROUP")
    private String pluginGroup;
    @TableField(value = "PLUGIN_HANDLE")
    private String pluginHandle;
    @TableField(value = "DESCRIPTION")
    private String desc;
    @TableField(value = "FROM_TYPE")
    private String fromType;
    @TableField(value = "SOURCE_ID")
    private String sourceId;
    @TableField(value = "CONTEXT")
    private String context;
    @TableField(value = "CHECK_POINT_INFO")
    private String checkPointInfo;
    @TableField(value = "ENABLE")
    private boolean enable;
    @TableField(value = "STATUS")
    private String status;
    @TableField(value = "MAX_RETRY_TIMES")
    private Integer maxRetryTimes;
    @TableField(value = "OWNER_ID")
    private String ownerId;
    @TableField(value = "BUCKET_ID")
    private Integer bucketId;

    @TableField(value = "ASYNC_RECOVER_CONTEXT",typeHandler = JacksonTypeHandler.class)
    private RecoverStrategy asyncRecoverContext;

    @TableField(value = "INSERT_TIME",updateStrategy = FieldStrategy.NEVER)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime insertTime;

    @TableField(value = "UPDATE_TIME")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updateTime;

    public HerculesFailedTaskPo convert2FailedTaskPo(String errorInfo){
        if(getAsyncRecoverContext()==null){
            throw new IllegalStateException("Only tasks configured with an asynchronous recovery policy can be retried asynchronously.");
        }
        if(!TaskStatus.RUNNING.name().equals(getStatus())){
            throw new IllegalStateException("Only asynchronously retry tasks that are in a running state and about to fail.");
        }
        HerculesTaskInfo herculesTaskInfo = new HerculesTaskInfo();
        BeanUtils.copyProperties(this,herculesTaskInfo);
        if(!TaskType.ASYNC_RECOVER.name().equalsIgnoreCase(getFromType())){
            herculesTaskInfo.setSourceId(this.id);
        }
        herculesTaskInfo.setId(null);
        herculesTaskInfo.setFromType(TaskType.ASYNC_RECOVER.name());
        herculesTaskInfo.setStatus(TaskStatus.INIT.name());
        herculesTaskInfo.setOwnerId(null);
        herculesTaskInfo.setCheckPointInfo(null);
        LocalDateTime nextProcessTime = null;
        if(herculesTaskInfo.getAsyncRecoverContext().processFinished()){
            if (!herculesTaskInfo.getAsyncRecoverContext().isPersistence()) {
                return null;
            }
        }else{
            nextProcessTime = herculesTaskInfo.getAsyncRecoverContext().processAndIncrementNextTimeStamp();
        }
        return new HerculesFailedTaskPo()
                .setNextProcessTime(nextProcessTime)
                .setDescription(desc)
                .setPluginGroup(pluginGroup)
                .setPluginHandle(pluginHandle)
                .setExecutorRegion(executorRegion)
                .setDescription(desc)
                .setBucketId(RandomUtils.nextInt(1,CRON_JOB_MAX_BUCKET_SIZE))
                .setTaskInfo(herculesTaskInfo)
                .setErrorInfo(errorInfo)
                .setInsertTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
    }
}
