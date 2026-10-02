package team.magic.flute.hercules.common.plugin;

import java.util.Map;

/**
 * Plugin Registration Interface
 *
 * <p>This interface is responsible for providing plugin registration information
 * to the Hercules system. Every plugin JAR must contain exactly one implementation
 * of this interface to register all the plugins contained within the JAR.
 *
 * <p>The registration process works as follows:
 * <ol>
 *   <li>When a plugin JAR is uploaded to Hercules Manager, the system scans for
 *       implementations of this interface</li>
 *   <li>The {@link #getRegisterInfo()} method is called to retrieve plugin metadata</li>
 *   <li>Each plugin is registered in the system with its associated metadata</li>
 *   <li>The plugins become available for task execution</li>
 * </ol>
 *
 * <p>Example implementation:
 * <pre>{@code
 * @AutoService(PluginRegister.class)
 * public class MyPluginRegister implements PluginRegister {
 *
 *     @Override
 *     public Map<String, PluginRegisterInfo> getRegisterInfo() {
 *         Map<String, PluginRegisterInfo> plugins = new HashMap<>();
 *
 *         // Register first plugin
 *         PluginRegisterInfo plugin1 = new PluginRegisterInfo()
 *             .setPluginName("data_export_v1")
 *             .setClassName("com.example.DataExportPlugin")
 *             .setDesc("Export data from RDS to OSS");
 *         plugins.put(plugin1.getPluginName(), plugin1);
 *
 *         // Register second plugin
 *         PluginRegisterInfo plugin2 = new PluginRegisterInfo()
 *             .setPluginName("data_transform_v1")
 *             .setClassName("com.example.DataTransformPlugin")
 *             .setDesc("Transform and clean data using DuckDB");
 *         plugins.put(plugin2.getPluginName(), plugin2);
 *
 *         return plugins;
 *     }
 * }
 * }</pre>
 *
 * <p>Important notes:
 * <ul>
 *   <li>Use {@code @AutoService(PluginRegister.class)} for automatic discovery</li>
 *   <li>Plugin names must be unique within the same plugin group</li>
 *   <li>Class names must be fully qualified and accessible from the plugin JAR</li>
 *   <li>Descriptions should be meaningful for operational and debugging purposes</li>
 * </ul>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public interface PluginRegister {

    /**
     * Get registration information for all plugins in this JAR.
     *
     * <p>This method returns a map containing the registration information for all
     * plugins that should be registered from this JAR file. The map key is the
     * plugin name (which must match the name returned by the plugin's {@code getName()}
     * method), and the value is the registration metadata.
     *
     * <p>The registration information includes:
     * <ul>
     *   <li>Plugin name - unique identifier for the plugin</li>
     *   <li>Class name - fully qualified class name of the plugin implementation</li>
     *   <li>Description - human-readable description of the plugin's functionality</li>
     * </ul>
     *
     * @return a map of plugin names to their registration information
     *         (must not be null, but can be empty if no plugins to register)
     */
    Map<String,PluginRegisterInfo> getRegisterInfo();
}
