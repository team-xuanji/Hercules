package team.magic.flute.hercules.common.http;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain=true)
public class PluginDesc {
    private String pluginHandle;
    private String pluginGroup;
    private String pluginDesc;
}
