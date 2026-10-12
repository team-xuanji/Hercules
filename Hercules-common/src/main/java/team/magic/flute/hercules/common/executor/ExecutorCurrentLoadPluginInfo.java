package team.magic.flute.hercules.common.executor;

import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.http.PluginDesc;

@Data
@Accessors(chain = true)
public class ExecutorCurrentLoadPluginInfo {
    private Map<String, PluginDesc> pluginMap;
}
