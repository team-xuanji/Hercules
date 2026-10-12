package team.magic.flute.hercules.manager.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.RequiredArgsConstructor;
import team.magic.flute.hercules.manager.dao.po.HerculesPluginImplInfoPo;

@RequiredArgsConstructor
public class PluginRegisterImplVO {
    @JsonIgnore private final HerculesPluginImplInfoPo pluginImpl;

    public String getDesc() {
        return pluginImpl.getDesc();
    }

    public String getPluginImplName() {
        return pluginImpl.getPluginGroup();
    }

    public String getPluginHandle() {
        return pluginImpl.getPluginHandle();
    }

    public String getImplClass() {
        return pluginImpl.getImplClassName();
    }
}
