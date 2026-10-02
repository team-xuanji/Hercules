package team.magic.flute.hercules.common.executor;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain=true)
public class ExecutorCurrentLoadPluginInfo {
    private Map<String,PluginDesc> pluginMap;
}
