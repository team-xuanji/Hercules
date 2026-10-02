package team.magic.flute.hercules.executor.config;

import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.plugin.TaskPlugin;

import java.net.URLClassLoader;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Accessors(chain=true)
public class TaskPluginContextDetail {
    private String pluginGroup;
    private String pluginVersion;
    private URLClassLoader pluginClassLoader;
    private Map<String, TaskPlugin> plugins;
    private LocalDateTime publishTime;
}
