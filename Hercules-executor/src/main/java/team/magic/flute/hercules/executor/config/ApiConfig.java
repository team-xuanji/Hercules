package team.magic.flute.hercules.executor.config;


import team.magic.flute.hercules.executor.api.HerculesManagerApi;
import team.magic.flute.hercules.executor.feign.FeignInterceptor;
import team.magic.flute.hercules.executor.feign.SigningRequestInterceptor;
import team.magic.flute.hercules.executor.feign.FeignUtils;
import team.magic.flute.hercules.executor.feign.bean.ApiConfigBean;
import team.magic.flute.hercules.executor.feign.bean.FeignConfigBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class ApiConfig {

    @ConfigurationProperties(prefix = "feign.retry")
    @Bean("feignConfigBean")
    public FeignConfigBean feignConfigBean() {
        return new FeignConfigBean();
    }

    @ConfigurationProperties(prefix = "api.app-open-api")
    @Bean("appOpenApiConfig")
    public ApiConfigBean appOpenApiConfig() {
        return new ApiConfigBean();
    }

    @Autowired
    private SigningRequestInterceptor signingRequestInterceptor;

    @Bean("HerculesManagerApi")
    public HerculesManagerApi appOpenApi(
            @Qualifier("feignConfigBean") FeignConfigBean feignConfigBean,
            @Qualifier("appOpenApiConfig") ApiConfigBean apiConfigBean) {
        return FeignUtils.getDefaultBuilder(feignConfigBean)
                .requestInterceptor(new FeignInterceptor(apiConfigBean))
                .requestInterceptor(signingRequestInterceptor)
                .target(HerculesManagerApi.class, apiConfigBean.getUrl());
    }
}
