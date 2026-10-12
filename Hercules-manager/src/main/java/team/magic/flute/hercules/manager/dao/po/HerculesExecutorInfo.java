package team.magic.flute.hercules.manager.dao.po;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.Jackson3TypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;
import team.magic.flute.hercules.common.executor.ExecutorCurrentLoadPluginInfo;
import team.magic.flute.hercules.common.executor.ExecutorProcessHandleWhiteList;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;

@Data
@Accessors(chain = true)
@TableName(value = "HERCULES_EXECUTOR_INFO", autoResultMap = true)
public class HerculesExecutorInfo {
    @TableId(value = "EXECUTOR_ID")
    private String executorId;

    @TableField(value = "IDENTITY_ID")
    private String identityId;

    @TableField(value = "EXECUTOR_REGION")
    private String executorRegion;

    @TableField(value = "EXECUTOR_REGION_DESC")
    private String executorRegionDesc;

    @TableField(value = "EXECUTOR_MAX_SLOT")
    private Integer executorMaxSlot;

    @TableField(value = "EXECUTOR_AVAILABLE_SLOT")
    private Integer executorAvailableSlot;

    @TableField(value = "ENABLE_DUCKDB")
    private boolean enableDuckdb;

    @TableField(value = "EXECUTOR_LOAD_PLUGIN_INFO", typeHandler = Jackson3TypeHandler.class)
    private ExecutorCurrentLoadPluginInfo executorLoadPluginInfo;

    @TableField(
            value = "EXECUTOR_PLUGIN_HANDLE_WHITE_LIST",
            typeHandler = Jackson3TypeHandler.class)
    private ExecutorProcessHandleWhiteList executorPluginHandleWhiteList;

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
