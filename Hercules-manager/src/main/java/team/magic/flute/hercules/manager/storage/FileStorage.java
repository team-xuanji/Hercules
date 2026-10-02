package team.magic.flute.hercules.manager.storage;

import team.magic.flute.hercules.common.plugin.PluginResourceInfo;
import team.magic.flute.hercules.manager.dao.po.HerculesPluginInfoPo;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public interface FileStorage {
    PluginResourceInfo getPlugin(HerculesPluginInfoPo plugin);

    default List<PluginResourceInfo> getPlugins(List<HerculesPluginInfoPo> plugins){
        if (CollectionUtils.isEmpty(plugins)) {
            return Collections.emptyList();
        }
        return plugins.stream()
                .map(this::getPlugin)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    Set<String> getUrls(String pluginGroup, MultipartFile[] files);

    @Deprecated
    void download(String pluginGroup, String fileName, HttpServletResponse response) throws IOException;

    void download2LocalFile(String ossKey, File destFile);

    InputStream getDownloadStream(String ossKey);

    void upload(String pluginGroup, MultipartFile[] files)throws IOException;

    void delete(String pluginGroup)throws IOException;

    URL parse2HttpUrls(String originalUrl);

}
