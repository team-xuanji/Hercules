package team.magic.flute.hercules.manager.service;

import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.manager.exception.PluginRegisterFailedException;
import team.magic.flute.hercules.manager.vo.PluginRegisterImplVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface HerculesPluginManagerService {
    BaseResponse<PluginResourceInfo> registerPlugin(MultipartFile[]pluginFiles, String name) throws PluginRegisterFailedException;
    BaseResponse<String> removePlugin(String pluginGroup) throws PluginRegisterFailedException;

    PluginResourceInfo searchPlugin(String pluginHandle, String pluginGroup);

    List<PluginRegisterImplVO> searchPluginImplInfo(String pluginGroup);

    List<PluginResourceInfo> searchAllPluginInfo();
}
