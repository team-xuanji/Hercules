package team.magic.flute.hercules.plugin.etl;

import com.google.auto.service.AutoService;
import team.magic.flute.hercules.common.plugin.PluginRegisterInfo;


import java.util.HashMap;
import java.util.Map;

@AutoService(team.magic.flute.hercules.common.plugin.PluginRegister.class)
public class PluginRegister implements team.magic.flute.hercules.common.plugin.PluginRegister {
    @Override
    public Map<String, PluginRegisterInfo> getRegisterInfo() {
        Map<String, PluginRegisterInfo> map = new HashMap<>();
        PluginRegisterInfo pluginRegisterInfo =
                new PluginRegisterInfo()
                        .setClassName(DataYEtlPlugin.class.getName())
                        .setDesc("DataY ETL Export Plugin")
                        .setPluginName("datay_etl_plugin_v1");
        map.put(pluginRegisterInfo.getPluginName(), pluginRegisterInfo);
        return map;
    }
}
