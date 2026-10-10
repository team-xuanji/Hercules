package team.magic.flute.hercules.executor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import team.magic.flute.hercules.executor.feign.SigningRequestInterceptor;

/**
 * Exposes {@link SigningRequestInterceptor} as a Spring bean so the Feign
 * builder in {@code ApiConfig} can attach it with a Spring-injected
 * {@link RunnerEnv} (the identity id is only known after context startup, so
 * the interceptor cannot be a static field on {@code FeignConfig}).
 */
@Configuration
public class SigningInterceptorConfig {

    @Bean
    public SigningRequestInterceptor signingRequestInterceptor(RunnerEnv runnerEnv) {
        return new SigningRequestInterceptor(runnerEnv);
    }
}
