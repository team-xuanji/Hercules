package team.magic.flute.hercules.twelve.labors.service;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.comm.Protocol;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * OSS Client Cache Service for Business Access Gateway
 *
 * <p>Service providing OSS client caching and download link caching functionality
 * within the Hercules business access gateway for efficient object storage operations.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
public class OssClientCacheService {
    
    /**
     * OSS client cache
     * Key: endPoint + bucket + ossKey + ossSecret
     * Value: OSS client instance
     */
    private final ConcurrentHashMap<String, OSS> ossClientCache = new ConcurrentHashMap<>();

    /**
     * Download link cache
     * Cache expiration time: 50 minutes
     */
    private final Cache<String, String> downloadUrlCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(50, TimeUnit.MINUTES)
            .build();
    
    /**
     * Get or create OSS client
     *
     * @param endpoint OSS endpoint
     * @param bucket bucket name
     * @param ossKey OSS access key ID
     * @param ossSecret OSS access secret
     * @return OSS client instance
     */
    public OSS getOrCreateOssClient(String endpoint, String bucket, String ossKey, String ossSecret) {
        String cacheKey = buildOssClientCacheKey(endpoint, bucket, ossKey, ossSecret);

        return ossClientCache.computeIfAbsent(cacheKey, key -> {
            log.info("Creating new OSS client: endpoint={}, bucket={}", endpoint, bucket);
            ClientBuilderConfiguration config = new ClientBuilderConfiguration();
            config.setProtocol(Protocol.HTTPS);
            return new OSSClientBuilder().build(endpoint, ossKey, ossSecret,config);
        });
    }
    
    /**
     * Get cached download URL
     *
     * @param endpoint OSS endpoint
     * @param bucket bucket name
     * @param objectKey object key
     * @param ossKey OSS access key ID
     * @param ossSecret OSS access secret
     * @return cached download URL, returns null if not exists
     */
    public String getCachedDownloadUrl(String endpoint, String bucket, String objectKey, String ossKey, String ossSecret) {
        String cacheKey = buildDownloadUrlCacheKey(endpoint, bucket, objectKey, ossKey, ossSecret);
        return downloadUrlCache.getIfPresent(cacheKey);
    }

    /**
     * Cache download URL
     *
     * @param endpoint OSS endpoint
     * @param bucket bucket name
     * @param objectKey object key
     * @param ossKey OSS access key ID
     * @param ossSecret OSS access secret
     * @param downloadUrl download URL
     */
    public void cacheDownloadUrl(String endpoint, String bucket, String objectKey, String ossKey, String ossSecret, String downloadUrl) {
        String cacheKey = buildDownloadUrlCacheKey(endpoint, bucket, objectKey, ossKey, ossSecret);
        downloadUrlCache.put(cacheKey, downloadUrl);
        log.debug("Cached download URL: key={}, url={}", cacheKey, downloadUrl);
    }
    
    /**
     * Build OSS client cache key
     *
     * @param endpoint OSS endpoint
     * @param bucket bucket name
     * @param ossKey OSS access key ID
     * @param ossSecret OSS access secret
     * @return cache key
     */
    private String buildOssClientCacheKey(String endpoint, String bucket, String ossKey, String ossSecret) {
        return String.format("oss_client:%s:%s:%s:%s", endpoint, bucket, ossKey, hashSecret(ossSecret));
    }

    /**
     * Build download URL cache key
     *
     * @param endpoint OSS endpoint
     * @param bucket bucket name
     * @param objectKey object key
     * @param ossKey OSS access key ID
     * @param ossSecret OSS access secret
     * @return cache key
     */
    private String buildDownloadUrlCacheKey(String endpoint, String bucket, String objectKey, String ossKey, String ossSecret) {
        return String.format("download_url:%s:%s:%s:%s:%s", endpoint, bucket, objectKey, ossKey, hashSecret(ossSecret));
    }
    
    /**
     * Hash secret for security considerations
     *
     * @param secret original secret
     * @return hashed secret
     */
    private String hashSecret(String secret) {
        if (secret == null || secret.length() < 8) {
            return "unknown";
        }
        // Simple hash processing, keep only first 4 and last 4 characters, replace middle with ***
        return secret.substring(0, 4) + "***" + secret.substring(secret.length() - 4);
    }

    /**
     * Clear OSS client cache
     * Close all OSS client connections
     */
    public void clearOssClientCache() {
        log.info("Starting to clear OSS client cache, current cache size: {}", ossClientCache.size());

        ossClientCache.values().forEach(ossClient -> {
            try {
                ossClient.shutdown();
            } catch (Exception e) {
                log.warn("Exception occurred when closing OSS client", e);
            }
        });

        ossClientCache.clear();
        log.info("OSS client cache cleanup completed");
    }
    
    /**
     * Clear download URL cache
     */
    public void clearDownloadUrlCache() {
        downloadUrlCache.invalidateAll();
        log.info("Download URL cache cleanup completed");
    }

    /**
     * Get cache statistics
     *
     * @return cache statistics information
     */
    public java.util.Map<String, Object> getCacheStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("ossClientCacheSize", ossClientCache.size());
        stats.put("downloadUrlCacheSize", downloadUrlCache.asMap().size());
        stats.put("downloadUrlCacheStats", downloadUrlCache.stats());
        return stats;
    }
}
