package team.magic.flute.hercules.twelve.labors.feign.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Feign Configuration Bean for Business Access Gateway
 *
 * <p>Configuration bean containing retry and timeout parameters for Feign
 * HTTP clients within the Hercules business access gateway.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class FeignConfigBean {
    private Integer maxAttempts;
    private Long period;
    private Long maxPeriod;
}
