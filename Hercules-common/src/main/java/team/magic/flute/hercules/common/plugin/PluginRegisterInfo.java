package team.magic.flute.hercules.common.plugin;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Plugin Registration Information
 *
 * <p>This class contains the metadata required to register a plugin with the Hercules system. It
 * provides the essential information needed for plugin discovery, loading, and execution.
 *
 * <p>The registration information is used by the Hercules Manager to:
 *
 * <ul>
 *   <li>Catalog available plugins and their capabilities
 *   <li>Route tasks to the appropriate plugin implementations
 *   <li>Load plugin classes dynamically at runtime
 *   <li>Provide operational visibility into plugin functionality
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * PluginRegisterInfo info = new PluginRegisterInfo()
 *     .setPluginName("data_export_v1")
 *     .setClassName("com.example.plugins.DataExportPlugin")
 *     .setDesc("Export data from MySQL to OSS in various formats");
 * }</pre>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class PluginRegisterInfo {

    /**
     * The unique name of the plugin.
     *
     * <p>This name serves as the unique identifier for the plugin within its plugin group. It must
     * match the name returned by the plugin's {@code getName()} method and is used for task routing
     * and plugin selection.
     *
     * <p>Naming conventions:
     *
     * <ul>
     *   <li>Use descriptive names that indicate the plugin's purpose
     *   <li>Include version information if multiple versions exist
     *   <li>Use lowercase with underscores for consistency
     *   <li>Avoid special characters that might cause routing issues
     * </ul>
     *
     * <p>Examples: "data_export_v1", "email_notification", "report_generator_v2"
     */
    private String pluginName;

    /**
     * The fully qualified class name of the plugin implementation.
     *
     * <p>This must be the complete class name including the package path of the class that
     * implements the {@link TaskPlugin} interface. The class must be accessible from the plugin
     * JAR's classpath.
     *
     * <p>Example: "com.example.plugins.DataExportPlugin"
     */
    private String className;

    /**
     * Human-readable description of the plugin's functionality.
     *
     * <p>This description should clearly explain what the plugin does, its main use cases, and any
     * important operational considerations. It is used for:
     *
     * <ul>
     *   <li>Documentation and operational visibility
     *   <li>Plugin catalog and discovery
     *   <li>Debugging and troubleshooting
     *   <li>User interfaces and management tools
     * </ul>
     *
     * <p>Example: "Export data from MySQL databases to OSS storage in CSV, JSON, or Parquet formats
     * with configurable batch sizes and compression options"
     */
    private String desc;
}
