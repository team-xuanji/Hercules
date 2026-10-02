package team.magic.flute.hercules.executor.feign.bean;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FeignConfigBean {
    private Integer maxAttempts;
    private Long period;
    private Long maxPeriod;
}
