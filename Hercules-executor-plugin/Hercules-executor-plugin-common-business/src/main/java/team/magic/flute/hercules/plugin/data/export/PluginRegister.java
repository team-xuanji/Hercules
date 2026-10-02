package team.magic.flute.hercules.plugin.data.export;

import com.google.auto.service.AutoService;
import java.util.HashMap;
import java.util.Map;
import team.magic.flute.hercules.common.plugin.PluginRegisterInfo;
import team.magic.flute.hercules.plugin.data.export.rds.RdsExportPlugin;

@AutoService(team.magic.flute.hercules.common.plugin.PluginRegister.class)
public class PluginRegister implements team.magic.flute.hercules.common.plugin.PluginRegister {
    @Override
    public Map<String, PluginRegisterInfo> getRegisterInfo() {
        Map<String, PluginRegisterInfo> map = new HashMap<>();
        PluginRegisterInfo pluginRegisterInfo =
                new PluginRegisterInfo()
                        .setClassName(RdsExportPlugin.class.getName())
                        .setDesc("Mysql Data Export Plugin")
                        .setPluginName("common_rds_exporter_v1");
        map.put(pluginRegisterInfo.getPluginName(), pluginRegisterInfo);
        return map;
    }
}
