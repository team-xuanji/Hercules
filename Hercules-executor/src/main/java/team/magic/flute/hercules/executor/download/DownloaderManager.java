package team.magic.flute.hercules.executor.download;

import team.magic.flute.hercules.common.util.StrFormat;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Downloader Manager
 *
 * <p>This component manages and coordinates multiple downloader implementations,
 * providing a centralized registry for protocol-specific download handlers.
 *
 * <p>The manager automatically discovers and registers all available downloader
 * implementations at startup, creating a mapping between URI schemas and their
 * corresponding downloader instances. This allows for dynamic routing of download
 * requests based on the source URI protocol.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Automatic discovery and registration of downloader implementations</li>
 *   <li>Schema-based routing to appropriate downloader instances</li>
 *   <li>Validation to prevent duplicate schema registrations</li>
 *   <li>Thread-safe access to downloader instances</li>
 * </ul>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Component
public class DownloaderManager {

    @Autowired
    private List<Downloader> downloaders;
    private final ConcurrentHashMap<String, Downloader> services = new ConcurrentHashMap<>();

    /**
     * Initialize the downloader manager by registering all available downloaders.
     *
     * <p>This method is called automatically after dependency injection is complete.
     * It discovers all downloader implementations, validates their schemas, and
     * registers them in the internal registry for later use.
     *
     * @throws RuntimeException if a downloader has no schema or if duplicate schemas are found
     */
    @PostConstruct
    public void init() {
        if (CollectionUtils.isEmpty(downloaders)) {
            log.warn("no download plugin");
            return;
        }
        for (Downloader downloader : downloaders) {
            String schema = downloader.getSchema();
            if (StringUtils.isBlank(schema)) {
                throw new RuntimeException(StrFormat.format("downloader must be specified schema，Class={}", downloader.getClass()));
            }

            Downloader oldDownloader = this.services.get(schema);
            if (Objects.nonNull(oldDownloader)) {
                throw new RuntimeException(StrFormat.format("downloader schema definite repeat，Class={}，OtherClass={}", downloader.getClass(), oldDownloader.getClass()));
            }
            log.info("register downloader，schema={}，Class={}", schema, downloader.getClass());
            this.services.put(schema, downloader);
        }
    }

    /**
     * Get a downloader instance for the specified URI schema.
     *
     * <p>This method looks up and returns the appropriate downloader implementation
     * based on the provided URI schema (e.g., "http", "https", "ftp").
     *
     * @param schema the URI schema to get a downloader for
     * @return the downloader instance that handles the specified schema
     * @throws RuntimeException if no downloader is registered for the given schema
     */
    public Downloader getDownloader(String schema) {
        Downloader downloader = this.services.get(schema);
        if (Objects.isNull(downloader)) {
            String errorMsg = StrFormat.format("no downloader，schema={}", schema);
            log.error(errorMsg);
            throw new RuntimeException(errorMsg);
        }
        return downloader;
    }
}
