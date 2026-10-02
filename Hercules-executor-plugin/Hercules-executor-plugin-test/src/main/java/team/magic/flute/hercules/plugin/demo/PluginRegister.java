package team.magic.flute.hercules.plugin.demo;

import com.google.auto.service.AutoService;
import java.util.HashMap;
import java.util.Map;
import team.magic.flute.hercules.common.plugin.PluginRegisterInfo;

@AutoService(team.magic.flute.hercules.common.plugin.PluginRegister.class)
public class PluginRegister implements team.magic.flute.hercules.common.plugin.PluginRegister {
    @Override
    public Map<String, PluginRegisterInfo> getRegisterInfo() {
        Map<String, PluginRegisterInfo> map = new HashMap<>();
        PluginRegisterInfo pluginRegisterInfo =
                new PluginRegisterInfo()
                        .setClassName("team.magic.flute.hercules.plugin.demo.TestPlugin")
                        .setDesc("DemoTaskPlugin")
                        .setPluginName("TEST");
        map.put(pluginRegisterInfo.getPluginName(), pluginRegisterInfo);
        return map;
    }
}
