package team.magic.flute.hercules.manager.controller;

import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.manager.service.HerculesPluginManagerService;
import team.magic.flute.hercules.manager.vo.PluginRegisterImplVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Plugin Management Controller
 *
 * <p>This controller provides REST APIs for managing Hercules plugins, including:
 * <ul>
 *   <li>Searching for plugin information by handle or group</li>
 *   <li>Listing all available plugins</li>
 *   <li>Viewing plugin implementation details</li>
 *   <li>Registering new plugins from uploaded JAR files</li>
 *   <li>Removing existing plugins</li>
 * </ul>
 *
 * <p>Plugins are the core execution units in Hercules, providing the actual business logic
 * for task execution. This controller manages the plugin lifecycle and metadata.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/pluginManager")
public class PluginRegisterController {
    @Autowired
    private HerculesPluginManagerService pluginManagerService;

    /**
     * Search for plugin information by handle or group.
     *
     * <p>Retrieves plugin information based on the provided search criteria.
     * Both parameters are optional, allowing for flexible search operations.
     *
     * @param pluginHandle the specific plugin handler to search for (optional)
     * @param pluginGroup the plugin group to search within (optional)
     * @return BaseResponse containing the matching plugin resource information
     */
    @GetMapping("/searchPlugin")
    public BaseResponse<PluginResourceInfo> searchPluginInfo(@RequestParam(name = "pluginHandle",required = false) String pluginHandle,
                                                             @RequestParam(name = "pluginGroup",required = false) String pluginGroup){
        return BaseResponse.success(pluginManagerService.searchPlugin(pluginHandle,pluginGroup));
    }

    /**
     * Retrieve information about all registered plugins.
     *
     * <p>Returns a comprehensive list of all plugins currently registered in the system,
     * including their resource information and metadata.
     *
     * @return BaseResponse containing a list of all plugin resource information
     */
    @GetMapping("/searchAllPlugin")
    public BaseResponse<List<PluginResourceInfo>> searchAllPluginInfo(){
        return BaseResponse.success(pluginManagerService.searchAllPluginInfo());
    }

    /**
     * Get implementation details for plugins in a specific group.
     *
     * <p>Retrieves detailed implementation information for all plugins within
     * the specified plugin group, including class names and descriptions.
     *
     * @param pluginGroup the plugin group to query for implementation details
     * @return BaseResponse containing a list of plugin implementation information
     */
    @GetMapping("/searchPluginImplInfo")
    public BaseResponse<List<PluginRegisterImplVO>> searchPluginImplInfo(@RequestParam(name = "pluginGroup") String pluginGroup){
        return BaseResponse.success(pluginManagerService.searchPluginImplInfo(pluginGroup));
    }

    /**
     * Register new plugins from uploaded JAR files.
     *
     * <p>Accepts one or more JAR files containing plugin implementations and registers
     * them under the specified plugin group. The system will scan the JAR files for
     * valid plugin implementations and update the plugin registry accordingly.
     *
     * @param pluginFiles array of JAR files containing plugin implementations
     * @param pluginGroup the group name under which to register the plugins
     * @return BaseResponse containing the registered plugin resource information
     */
    @PostMapping("/register")
    public BaseResponse<PluginResourceInfo> registerPlugin(@RequestParam(value = "pluginFiles") MultipartFile[] pluginFiles,
                                                           @RequestParam(value="pluginGroup")String pluginGroup){
        return pluginManagerService.registerPlugin(pluginFiles,pluginGroup);
    }

    /**
     * Remove a plugin group and all its associated plugins.
     *
     * <p>Permanently removes the specified plugin group and all plugins within it
     * from the system. This operation cannot be undone and will affect any tasks
     * that depend on the removed plugins.
     *
     * @param pluginGroup the plugin group to remove
     * @return BaseResponse containing a confirmation message
     */
    @GetMapping("/removePlugin")
    public BaseResponse<String> removePlugin(@RequestParam(value="pluginGroup")String pluginGroup){
        return pluginManagerService.removePlugin(pluginGroup);
    }
}
