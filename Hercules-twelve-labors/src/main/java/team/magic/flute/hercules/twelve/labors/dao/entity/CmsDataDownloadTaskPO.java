package team.magic.flute.hercules.twelve.labors.dao.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import team.magic.flute.hercules.twelve.labors.business.export.entity.RdsExportContext;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * CMS Data Download Task Persistent Object
 *
 * <p>This persistent object represents individual data download task instances within the
 * Hercules business access gateway. Each instance corresponds to a specific execution of
 * a data export operation, tracking the complete lifecycle from submission to completion
 * and providing access to the generated export files.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this entity enables comprehensive task management for business data export operations:
 * <ul>
 *   <li>Individual task instance tracking with unique identification</li>
 *   <li>User and application context preservation for audit and access control</li>
 *   <li>Real-time status monitoring and progress tracking</li>
 *   <li>Export file location and access management</li>
 * </ul>
 *
 * <p><strong>Task Lifecycle Management:</strong>
 * <ul>
 *   <li>Task creation with user and business context</li>
 *   <li>Status progression from submission through completion</li>
 *   <li>Export context preservation for reproducibility</li>
 *   <li>File path tracking for download access</li>
 * </ul>
 *
 * <p><strong>Integration Features:</strong>
 * <ul>
 *   <li>Complex object storage using Jackson type handlers for RdsExportContext</li>
 *   <li>Multi-tenant support through app key and user ID tracking</li>
 *   <li>Audit trail with creation and modification timestamps</li>
 *   <li>Flexible task status management for various execution states</li>
 * </ul>
 *
 * <p><strong>Database Mapping:</strong> This entity maps to the HERCULES_CMS_DATA_DOWNLOAD_TASK
 * table with automatic ID generation and complex object serialization support through
 * MyBatis Plus annotations and Jackson type handlers.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
@TableName(value = "HERCULES_CMS_DATA_DOWNLOAD_TASK", autoResultMap = true)
public class CmsDataDownloadTaskPO {
    /**
     * Unique task identifier
     *
     * <p>Auto-generated unique identifier for this data download task instance.
     * Used for task tracking, status monitoring, and file access across the
     * business access gateway and underlying execution framework.
     */
    @TableId(value = "TASK_ID",type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long taskId;

    /**
     * Business scenario key
     *
     * <p>References the business scenario key from the task definition that was
     * used to create this task instance. Links this execution to the template
     * configuration and business context.
     */
    @TableField("BUSINESS_KEY")
    private String businessKey;

    /**
     * Export execution context
     *
     * <p>Complete configuration context for this specific task execution,
     * including database connections, SQL queries, export format settings,
     * and storage destinations. Stored as JSON using Jackson type handler.
     */
    @TableField(value = "CONTEXT",typeHandler = JacksonTypeHandler.class)
    private RdsExportContext context;

    /**
     * Generated file path
     *
     * <p>The complete path to the generated export file in the configured
     * object storage system. Used for file download and access control
     * within the business access gateway.
     */
    @TableField("FILE_PATH")
    private String filePath;

    /**
     * Current task execution status
     *
     * <p>Tracks the current state of task execution (e.g., PENDING, RUNNING,
     * SUCCESS, FAILED). Updated throughout the task lifecycle to provide
     * real-time status information to users and monitoring systems.
     */
    @TableField("TASK_STATUS")
    private String taskStatus;

    /**
     * Application key
     *
     * <p>Identifies the application or service that submitted this task.
     * Used for multi-tenant access control and resource allocation within
     * the business access gateway architecture.
     */
    @TableField("APP_KEY")
    private String appKey;

    /**
     * User identifier
     *
     * <p>Identifies the user who submitted this task. Used for access control,
     * audit logging, and ensuring users can only access their own task results
     * within the business access gateway.
     */
    @TableField("USER_ID")
    private String userId;

    /**
     * User display name
     *
     * <p>Human-readable name of the user who submitted this task. Used for
     * display purposes in user interfaces and audit logs within the business
     * access gateway.
     */
    @TableField("USER_NAME")
    private String userName;

    /**
     * Operation source
     *
     * <p>Identifies the source or channel through which this task was submitted
     * (e.g., web interface, API, scheduled job). Helps in understanding usage
     * patterns and optimizing the business access gateway for different access methods.
     */
    @TableField("OPERATOR_SOURCE")
    private String operatorSource;

    /**
     * Task creation time
     *
     * <p>The timestamp when this task was first submitted to the business access
     * gateway. Used for audit trails, performance analysis, and task lifecycle
     * tracking within the distributed execution framework.
     */
    @TableField("INSERT_TIME")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime insertTime;

    /**
     * Task last update time
     *
     * <p>The timestamp when this task record was last modified, typically when
     * status changes occur or additional information becomes available. Provides
     * a complete audit trail of task progression through the business access gateway.
     */
    @TableField("UPDATE_TIME")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updateTime;
}
