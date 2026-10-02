package team.magic.flute.hercules.executor.feign;

import team.magic.flute.hercules.executor.feign.bean.FeignConfigBean;
import feign.Contract;
import feign.Feign;
import feign.Logger;
import feign.Retryer;


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
