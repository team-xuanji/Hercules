package team.magic.flute.hercules.twelve.labors.business.inner;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Hercules Task Information Container
 *
 * <p>This class represents the core task information structure used throughout the
 * Hercules business access gateway for managing and tracking business operation tasks.
 * It serves as the primary data transfer object for task-related information between
 * the business access gateway and the underlying Hercules distributed execution framework.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this class enables comprehensive task management for various business operations including:
 * <ul>
 *   <li>Data export and transformation operations</li>
 *   <li>Business process automation tasks</li>
 *   <li>Integration and synchronization operations</li>
 *   <li>Custom business logic execution</li>
 * </ul>
 *
 * <p><strong>Task Lifecycle Management:</strong> This class supports the complete task
 * lifecycle from submission through completion, including:
 * <ul>
 *   <li>Task identification and business context tracking</li>
 *   <li>Plugin-based execution configuration</li>
 *   <li>Status monitoring and progress tracking</li>
 *   <li>Error handling and retry mechanisms</li>
 *   <li>Checkpoint and recovery support</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> Used extensively by the business access gateway
 * to communicate with the Hercules Manager API and coordinate task execution across
 * the distributed system, ensuring reliable and scalable business operation processing.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class HerculesTaskInfo {
    /**
     * Unique task identifier
     *
     * <p>A globally unique identifier for this task within the Hercules system.
     * Used for task tracking, status monitoring, and result retrieval across
     * the distributed execution environment.
     */
    private String id;

    /**
     * Business operation key
     *
     * <p>A business-meaningful identifier that categorizes the type of business
     * operation being performed. This key helps in organizing and managing
     * different types of business functions within the access gateway.
     */
    private String businessKey;

    /**
     * Plugin group identifier
     *
     * <p>Identifies the plugin group that contains the execution logic for this task.
     * Plugin groups organize related functionality and enable modular business
     * operation implementations within the Hercules framework.
     */
    private String pluginGroup;

    /**
     * Plugin handle identifier
     *
     * <p>Specifies the exact plugin implementation within the plugin group that
     * will execute this task. This allows for fine-grained control over which
     * business logic implementation is used for task execution.
     */
    private String pluginHandle;

    /**
     * Task description
     *
     * <p>A human-readable description of the task and its business purpose.
     * Used for logging, monitoring, and administrative purposes to provide
     * context about the business operation being performed.
     */
    private String desc;

    /**
     * Task source type
     *
     * <p>Indicates the source or origin type of this task, such as API request,
     * scheduled job, or system-generated task. Helps in understanding the
     * context and priority of the business operation.
     */
    private String fromType;

    /**
     * Source identifier
     *
     * <p>The specific identifier of the source that initiated this task.
     * This could be a user ID, system component ID, or external system
     * identifier, providing traceability for the business operation.
     */
    private String sourceId;

    /**
     * Task execution context
     *
     * <p>Contains the serialized execution context and parameters required
     * for task execution. This typically includes business-specific configuration,
     * input data, and execution parameters in JSON format.
     */
    private String context;

    /**
     * Checkpoint information
     *
     * <p>Contains checkpoint data for task recovery and resumption purposes.
     * Enables the task to resume from a specific point in case of failures,
     * supporting reliable execution of long-running business operations.
     */
    private String checkPointInfo;

    /**
     * Task enable status
     *
     * <p>Indicates whether this task is enabled for execution. Disabled tasks
     * will not be processed by the execution framework, allowing for task
     * management and control within the business access gateway.
     */
    private boolean enable;

    /**
     * Current task status
     *
     * <p>The current execution status of the task (e.g., PENDING, RUNNING,
     * SUCCESS, FAILED). Used for monitoring task progress and determining
     * the current state of business operations.
     */
    private String status;

    /**
     * Maximum retry attempts
     *
     * <p>The maximum number of retry attempts allowed for this task in case
     * of failures. Supports resilient execution of business operations by
     * allowing automatic recovery from transient failures.
     */
    private Integer maxRetryTimes;

    /**
     * Task owner identifier
     *
     * <p>Identifies the owner of this task, typically the user or system
     * component that initiated the business operation. Used for access control,
     * audit logging, and result delivery within the business access gateway.
     */
    private String ownerId;
}
