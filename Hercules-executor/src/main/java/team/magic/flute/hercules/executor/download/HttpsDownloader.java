package team.magic.flute.hercules.executor.download;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HttpsDownloader extends HttpDownloader {

    @Override
    public String getSchema() {
        return "https";
    }
}
