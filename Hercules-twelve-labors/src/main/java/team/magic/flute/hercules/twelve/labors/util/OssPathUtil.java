package team.magic.flute.hercules.twelve.labors.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskDefPO;
import team.magic.flute.hercules.twelve.labors.dto.CsvExportFormatParams;
import team.magic.flute.hercules.twelve.labors.dto.JsonExportFormatParams;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

/**
 * OSS Path Generation Utility for Business Access Gateway
 *
 * <p>Utility class for generating standardized OSS object paths within the Hercules
 * business access gateway. Provides consistent path generation for exported files.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class OssPathUtil {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_FILE_PREFIX = "/cms-data-download/";
    
    /**
     * Generate OSS file path
     *
     * @param taskDef task definition
     * @return OSS file path
     */
    public static String generateOssPath(CmsDataDownloadTaskDefPO taskDef) {
        if (taskDef == null) {
            throw new IllegalArgumentException("Task definition cannot be null");
        }

        // Generate file name
        String fileName = generateFileName(taskDef);

        // Combine complete path
        String ossRootPath = taskDef.getOssRootPath();
        if (!StringUtils.hasText(ossRootPath)) {
            ossRootPath = DEFAULT_FILE_PREFIX;
        }
        
        // Ensure path ends with /
        if (!ossRootPath.endsWith("/")) {
            ossRootPath += "/";
        }

        // Ensure path does not start with / (OSS path specification)
        if (ossRootPath.startsWith("/")) {
            ossRootPath = ossRootPath.substring(1);
        }
        
        return ossRootPath + fileName;
    }
    
    /**
     * Generate file name
     * Rule: filePrefix + uuid + exportFormat + compression extension
     *
     * @param taskDef task definition
     * @return file name
     */
    private static String generateFileName(CmsDataDownloadTaskDefPO taskDef) {
        StringBuilder fileName = new StringBuilder();
        
        // File prefix
        String filePrefix = taskDef.getFilePrefix();
        if (StringUtils.hasText(filePrefix)) {
            fileName.append(filePrefix);
        }

        // UUID
        String uuid = UUID.randomUUID().toString().replace("-", "");
        fileName.append(uuid);

        // Export format extension
        String exportFormat = taskDef.getExportFormatType();
        if (StringUtils.hasText(exportFormat)) {
            fileName.append(".").append(exportFormat.toLowerCase());
        }
        
        // Compression extension
        String compressionExtension = getCompressionExtension(taskDef);
        if (StringUtils.hasText(compressionExtension)) {
            fileName.append(compressionExtension);
        }
        
        return fileName.toString();
    }
    
    /**
     * Get compression format extension
     *
     * @param taskDef task definition
     * @return compression extension
     */
    private static String getCompressionExtension(CmsDataDownloadTaskDefPO taskDef) {
        String exportFormat = taskDef.getExportFormatType();
        String exportFormatConfig = taskDef.getExportFormatConfig();
        
        if (!StringUtils.hasText(exportFormat) || !StringUtils.hasText(exportFormatConfig)) {
            return null;
        }
        
        try {
            // Parse compression configuration based on export format
            if ("csv".equalsIgnoreCase(exportFormat)) {
                return getCsvCompressionExtension(exportFormatConfig);
            } else if ("json".equalsIgnoreCase(exportFormat)) {
                return getJsonCompressionExtension(exportFormatConfig);
            }
            // parquet and xlsx formats do not need additional compression extensions

        } catch (Exception e) {
            // Ignore compression extension when parsing fails
            return null;
        }
        
        return null;
    }
    
    /**
     * Get CSV format compression extension
     *
     * @param exportFormatConfig export format configuration
     * @return compression extension
     */
    private static String getCsvCompressionExtension(String exportFormatConfig) {
        try {
            // Try to parse as CsvExportFormatParams
            CsvExportFormatParams csvParams = objectMapper.readValue(exportFormatConfig, CsvExportFormatParams.class);
            String compression = csvParams.getCompression();
            
            if ("gzip".equalsIgnoreCase(compression)) {
                return ".gz";
            } else if ("zstd".equalsIgnoreCase(compression)) {
                return ".zst";
            }
            
        } catch (Exception e) {
            // If parsing as object fails, try parsing as Map
            try {
                Map<String, Object> configMap = objectMapper.readValue(exportFormatConfig, new TypeReference<Map<String, Object>>() {});
                Object compression = configMap.get("compression");
                
                if (compression != null) {
                    String compressionStr = compression.toString();
                    if ("gzip".equalsIgnoreCase(compressionStr)) {
                        return ".gz";
                    } else if ("zstd".equalsIgnoreCase(compressionStr)) {
                        return ".zst";
                    }
                }
            } catch (Exception ex) {
                // Ignore parsing errors
            }
        }
        
        return null;
    }
    
    /**
     * Get JSON format compression extension
     *
     * @param exportFormatConfig export format configuration
     * @return compression extension
     */
    private static String getJsonCompressionExtension(String exportFormatConfig) {
        try {
            // Try to parse as JsonExportFormatParams
            JsonExportFormatParams jsonParams = objectMapper.readValue(exportFormatConfig, JsonExportFormatParams.class);
            String compression = jsonParams.getCompression();
            
            if ("gzip".equalsIgnoreCase(compression)) {
                return ".gz";
            } else if ("zstd".equalsIgnoreCase(compression)) {
                return ".zst";
            }
            
        } catch (Exception e) {
            // If parsing as object fails, try parsing as Map
            try {
                Map<String, Object> configMap = objectMapper.readValue(exportFormatConfig, new TypeReference<Map<String, Object>>() {});
                Object compression = configMap.get("compression");
                
                if (compression != null) {
                    String compressionStr = compression.toString();
                    if ("gzip".equalsIgnoreCase(compressionStr)) {
                        return ".gz";
                    } else if ("zstd".equalsIgnoreCase(compressionStr)) {
                        return ".zst";
                    }
                }
            } catch (Exception ex) {
                // Ignore parsing errors
            }
        }
        
        return null;
    }
}
