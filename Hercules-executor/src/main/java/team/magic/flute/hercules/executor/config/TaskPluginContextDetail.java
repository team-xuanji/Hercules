package team.magic.flute.hercules.executor.config;

import java.net.URLClassLoader;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.plugin.TaskPlugin;

@Data
@Accessors(chain = true)
public class TaskPluginContextDetail {
    private String pluginGroup;
    private String pluginVersion;
    private URLClassLoader pluginClassLoader;
    private Map<String, TaskPlugin> plugins;
    private LocalDateTime publishTime;
}
