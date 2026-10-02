package team.magic.flute.hercules.twelve.labors.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Object Storage Service (OSS) Configuration Information
 *
 * <p>Configuration parameters for object storage integration within the Hercules
 * business access gateway. Supports various cloud storage providers and services.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain=true)
public class OssInfo {
    /**
     * OSS authentication key
     */
    private String ossKey;
    /**
     * OSS authentication secret
     */
    private String ossSecret;
    /**
     * OSS region, e.g.: cn-zhangjiakou
     */
    private String ossRegion;
    /**
     * OSS endpoint
     */
    private String ossEndpoint;
    /**
     * OSS storage root path
     */
    private String ossRootPath;
}
