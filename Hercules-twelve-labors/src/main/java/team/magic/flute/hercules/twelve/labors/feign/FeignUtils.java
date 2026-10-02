package team.magic.flute.hercules.twelve.labors.feign;

import team.magic.flute.hercules.twelve.labors.feign.bean.FeignConfigBean;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Retryer;

/**
 * Feign Utilities for Business Access Gateway
 *
 * <p>Utility class for creating pre-configured Feign builders with standard
 * settings for HTTP clients within the Hercules business access gateway.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class FeignUtils {
    public static Feign.Builder getDefaultBuilder(FeignConfigBean feignConfigBean) {
        return Feign.builder()
                .client(FeignConfig.okHttpClient())
                .errorDecoder(FeignConfig.feignErrorDecoder())
                .encoder(FeignConfig.jacksonEncoder())
                .decoder(FeignConfig.jacksonDecoder())
                .logger(FeignConfig.slf4jLogger())
                .logLevel(Logger.Level.FULL)
                .contract(new Contract.Default())
                .retryer(
                        new Retryer.Default(
                                feignConfigBean.getPeriod(),
                                feignConfigBean.getMaxPeriod(),
                                feignConfigBean.getMaxAttempts()));
    }
}
