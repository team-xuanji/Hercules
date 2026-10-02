package team.magic.flute.hercules.executor.storage;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Set;

public interface FileStorage {

    Set<String> getUrls(String pluginGroup, MultipartFile[] files);

    @Deprecated
    void download(String pluginGroup, String fileName, HttpServletResponse response) throws IOException;

    void download2LocalFile(String ossKey, File destFile);

    InputStream getDownloadStream(String ossKey);

    void upload(String pluginGroup, MultipartFile[] files)throws IOException;

    void delete(String pluginGroup)throws IOException;

    URL parse2HttpUrls(String originalUrl);

}
