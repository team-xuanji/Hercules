package team.magic.flute.hercules.twelve.labors.business.inner;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Submit Once Type Task Request Value Object (Deprecated)
 *
 * <p>This class represents a request for submitting one-time business tasks within the
 * Hercules business access gateway. It encapsulates the necessary information required
 * to create and execute business operations through the distributed task execution framework.
 *
 * <p><strong>Deprecation Notice:</strong> This class is deprecated and should be replaced
 * with {@code team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo} for
 * new implementations. The common module provides a standardized and more comprehensive
 * task submission interface that supports advanced features and better integration.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this class historically enabled the submission of various business operations including:
 * <ul>
 *   <li>Data export and transformation tasks</li>
 *   <li>Business process automation operations</li>
 *   <li>Integration and synchronization tasks</li>
 *   <li>Custom business logic execution</li>
 * </ul>
 *
 * <p><strong>Migration Path:</strong> Existing code using this class should be migrated
 * to use the standardized common module implementation to ensure compatibility with
 * future versions and access to enhanced features.
 *
 * <p><strong>Legacy Support:</strong> This class is maintained for backward compatibility
 * but will be removed in future versions. New development should use the recommended
 * replacement class from the common module.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 * @deprecated Use {@code team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo} instead
 */
@Data
@Accessors(chain = true)
@Deprecated
public class SubmitOnceTypeTaskRequestVO {
    /**
     * Unique task identifier
     *
     * <p>Optional unique identifier for the task. If not provided, the system
     * will automatically generate a unique ID for task tracking and management.
     *
     * @deprecated Use the common module's HerculesSubmitOnceTaskRequest instead
     */
    private String id;

    /**
     * Business operation key
     *
     * <p>Identifies the type of business operation to be performed. This key
     * is used to route the task to the appropriate business logic and execution
     * context within the business access gateway.
     *
     * @deprecated Use the common module's HerculesSubmitOnceTaskRequest instead
     */
    private String executorRegion;

    /**
     * Task description
     *
     * <p>Human-readable description of the business task being submitted.
     * Used for logging, monitoring, and administrative purposes to provide
     * context about the operation being performed.
     *
     * @deprecated Use the common module's HerculesSubmitOnceTaskRequest instead
     */
    private String desc;

    /**
     * Plugin handle identifier
     *
     * <p>Specifies the exact plugin implementation that will execute this task.
     * The plugin handle identifies the specific business logic implementation
     * within the configured plugin group.
     *
     * @deprecated Use the common module's HerculesSubmitOnceTaskRequest instead
     */
    private String pluginHandle;

    /**
     * Plugin group identifier
     *
     * <p>Identifies the plugin group that contains the execution logic for this task.
     * Plugin groups organize related business functionality within the Hercules
     * distributed execution framework.
     *
     * @deprecated Use the common module's HerculesSubmitOnceTaskRequest instead
     */
    private String pluginGroup;

    /**
     * Task execution context
     *
     * <p>Contains the serialized execution context and parameters required for
     * task execution. This typically includes business-specific configuration,
     * input data, and execution parameters in JSON format.
     *
     * @deprecated Use the common module's HerculesSubmitOnceTaskRequest instead
     */
    private String context;

    /**
     * Maximum retry attempts
     *
     * <p>The maximum number of retry attempts allowed for this task in case of
     * failures. Supports resilient execution of business operations by allowing
     * automatic recovery from transient failures.
     *
     * @deprecated Use the common module's HerculesSubmitOnceTaskRequest instead
     */
    private Integer maxRetryTimes;
}
