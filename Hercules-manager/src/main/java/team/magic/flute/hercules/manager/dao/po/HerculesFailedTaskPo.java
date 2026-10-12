package team.magic.flute.hercules.manager.dao.po;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;

@Data
@Accessors(chain = true)
@TableName(value = "HERCULES_RECOVER_TASKS", autoResultMap = true)
public class HerculesFailedTaskPo {
    @TableId(value = "ID", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("PLUGIN_GROUP")
    private String pluginGroup;

    @TableField("PLUGIN_HANDLE")
    private String pluginHandle;

    @TableField("EXECUTOR_REGION")
    private String executorRegion;

    @TableField("DESCRIPTION")
    private String description;

    @TableField(value = "TASK_INFO", typeHandler = Jackson3TypeHandler.class)
    private HerculesTaskInfo taskInfo;

    @TableField("ERROR_INFO")
    private String errorInfo;

    @TableField("BUCKET_ID")
    private Integer bucketId;

    @TableField(value = "NEXT_PROCESS_TIME")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime nextProcessTime;

    @TableField(value = "INSERT_TIME", updateStrategy = FieldStrategy.NEVER)
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
}
