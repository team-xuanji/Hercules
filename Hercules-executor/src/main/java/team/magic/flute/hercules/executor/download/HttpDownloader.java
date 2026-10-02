package team.magic.flute.hercules.executor.download;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;

@Slf4j
@Component
public class HttpDownloader implements Downloader {

    @Override
    public String getSchema() {
        return "http";
    }

    @Override
    public File downloadToLocal(URI srcURI, File dstDir, String filename) throws IOException {
        URL url = srcURI.toURL();
        File localFile = new File(dstDir, filename);
        try (
                InputStream inputStream = url.openStream();
                BufferedOutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(localFile.toPath()));
        ) {
            byte[] buf = new byte[4096];
            int readBytes = -1;
            while ((readBytes = inputStream.read(buf)) >= 0) {
                outputStream.write(buf, 0, readBytes);
            }
        } catch (IOException e) {
            log.error("download error, srcURI: {}, dstFile: {}", srcURI, localFile, e);
            throw e;
        }
        return localFile;
    }
}
