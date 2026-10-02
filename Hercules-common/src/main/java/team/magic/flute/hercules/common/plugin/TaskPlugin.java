package team.magic.flute.hercules.common.plugin;

import team.magic.flute.hercules.common.status.TaskExecutionContext;

import java.io.Closeable;
import java.io.IOException;

/**
 * Task Plugin Interface
 *
 * <p>This interface defines the contract that all Hercules task plugins must implement.
 * Plugins are the core execution units that contain the actual business logic for
 * processing tasks within the Hercules system.
 *
 * <p>Plugin Lifecycle:
 * <ol>
 *   <li>{@link #init()} - Called once when the plugin is first loaded</li>
 *   <li>{@link #execute(TaskExecutionContext)} - Called for each task execution</li>
 *   <li>{@link #cleanUp()} - Called when the plugin is being unloaded</li>
 * </ol>
 *
 * <p>Key responsibilities of a plugin implementation:
 * <ul>
 *   <li>Provide a unique name for identification and routing</li>
 *   <li>Implement the core business logic in the execute method</li>
 *   <li>Handle task parameters and configuration from the execution context</li>
 *   <li>Set results in the context for business consumption</li>
 *   <li>Manage any plugin-specific resources properly</li>
 * </ul>
 *
 * <p>Example implementation:
 * <pre>{@code
 * @AutoService(TaskPlugin.class)
 * public class MyBusinessPlugin implements TaskPlugin {
 *
 *     @Override
 *     public String getName() {
 *         return "my_business_plugin_v1";
 *     }
 *
 *     @Override
 *     public void execute(TaskExecutionContext context) throws Exception {
 *         // Parse configuration
 *         String config = context.getExecutionContext();
 *         MyConfig myConfig = JacksonUtils.readValue(config, MyConfig.class);
 *
 *         // Use DuckDB for data processing
 *         Connection duckdb = context.getDuckdbConnection();
 *         // ... business logic ...
 *
 *         // Set results
 *         context.setCheckPointResult("Processing completed successfully");
 *     }
 * }
 * }</pre>
 *
 * <p>Plugins are automatically discovered using the Java ServiceLoader mechanism.
 * Make sure to annotate your implementation with {@code @AutoService(TaskPlugin.class)}
 * or manually create the appropriate META-INF/services file.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public interface TaskPlugin {

    /**
     * Initialize the plugin.
     *
     * <p>This method is called once when the plugin is first loaded by the executor.
     * Use this method to perform any one-time initialization tasks such as:
     * <ul>
     *   <li>Setting up connection pools</li>
     *   <li>Loading configuration files</li>
     *   <li>Initializing caches or other resources</li>
     * </ul>
     *
     * <p>The default implementation does nothing. Override this method if your
     * plugin requires initialization.
     */
    default void init(){}

    /**
     * Get the unique name of this plugin.
     *
     * <p>This name is used by the Hercules system to identify and route tasks
     * to the correct plugin implementation. The name should be:
     * <ul>
     *   <li>Unique across all plugins in the same plugin group</li>
     *   <li>Descriptive and meaningful for operational purposes</li>
     *   <li>Stable across plugin versions (or versioned explicitly)</li>
     * </ul>
     *
     * @return the unique plugin name (must not be null or empty)
     */
    String getName();

    /**
     * Execute the plugin's business logic.
     *
     * <p>This is the main method where the plugin performs its work. The method
     * receives a {@link TaskExecutionContext} containing all necessary resources
     * and configuration for task execution.
     *
     * <p>Key responsibilities:
     * <ul>
     *   <li>Parse task configuration from {@code context.getExecutionContext()}</li>
     *   <li>Perform the required business logic</li>
     *   <li>Use provided resources (DuckDB, additional context) as needed</li>
     *   <li>Set results using {@code context.setCheckPointResult()} if applicable</li>
     *   <li>Handle errors appropriately and provide meaningful error messages</li>
     * </ul>
     *
     * @param context the execution context containing resources and configuration
     * @throws Exception if task execution fails for any reason
     */
    void execute(TaskExecutionContext context) throws Exception;

    /**
     * Clean up plugin resources.
     *
     * <p>This method is called when the plugin is being unloaded or when the
     * executor is shutting down. Use this method to clean up any resources
     * that the plugin has allocated.
     *
     * <p>The default implementation automatically closes the plugin if it
     * implements the {@link Closeable} interface. Override this method if
     * your plugin requires custom cleanup logic.
     *
     * @throws IOException if an error occurs during cleanup
     */
    default void cleanUp() throws IOException {
        if(this instanceof Closeable){
            ((Closeable) this).close();
        }
    }
}
