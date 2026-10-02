package team.magic.flute.hercules.twelve.labors.config;


import team.magic.flute.hercules.twelve.labors.api.HerculesManagerApi;
import team.magic.flute.hercules.twelve.labors.feign.FeignInterceptor;
import team.magic.flute.hercules.twelve.labors.feign.FeignUtils;
import team.magic.flute.hercules.twelve.labors.feign.bean.ApiConfigBean;
import team.magic.flute.hercules.twelve.labors.feign.bean.FeignConfigBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API Configuration for Business Access Gateway
 *
 * <p>This configuration class sets up the external API clients and communication infrastructure
 * required by the Hercules business access gateway. It configures Feign clients for seamless
 * integration with the underlying Hercules distributed execution framework and other external
 * services that support business operations.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this configuration enables reliable communication with:
 * <ul>
 *   <li>Hercules Manager API for task orchestration and monitoring</li>
 *   <li>External business services and data sources</li>
 *   <li>Authentication and authorization services</li>
 *   <li>Monitoring and logging infrastructure</li>
 * </ul>
 *
 * <p><strong>Configuration Features:</strong>
 * <ul>
 *   <li>Automatic retry configuration for resilient API calls</li>
 *   <li>Request/response interceptors for authentication and logging</li>
 *   <li>Configurable timeouts and connection pooling</li>
 *   <li>Support for multiple API endpoints and environments</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> This configuration supports the business access gateway's
 * role as the primary entry point for business operations by ensuring reliable connectivity
 * to all necessary backend services and infrastructure components.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class ApiConfig {

    /**
     * Configure Feign retry settings for resilient API communication
     *
     * <p>Creates a configuration bean that defines retry behavior for Feign clients
     * used by the business access gateway. This ensures reliable communication with
     * external services even in the presence of transient network issues.
     *
     * @return FeignConfigBean with retry configuration from application properties
     */
    @ConfigurationProperties(prefix = "feign.retry")
    @Bean("feignConfigBean")
    public FeignConfigBean feignConfigBean() {
        return new FeignConfigBean();
    }

    /**
     * Configure API connection settings for Hercules Manager integration
     *
     * <p>Creates a configuration bean that contains connection details for the
     * Hercules Manager API, including endpoint URLs, authentication credentials,
     * and other connection parameters required for business operation orchestration.
     *
     * @return ApiConfigBean with Hercules Manager API configuration
     */
    @ConfigurationProperties(prefix = "api.app-open-api")
    @Bean("appOpenApiConfig")
    public ApiConfigBean appOpenApiConfig() {
        return new ApiConfigBean();
    }

    /**
     * Create Hercules Manager API client for business operation orchestration
     *
     * <p>Configures and creates a Feign client for communicating with the Hercules Manager
     * service. This client is essential for the business access gateway to submit tasks,
     * monitor execution status, and coordinate business operations across the distributed
     * Hercules execution framework.
     *
     * <p><strong>Business Operations Supported:</strong>
     * <ul>
     *   <li>Task submission for various business functions</li>
     *   <li>Real-time status monitoring and progress tracking</li>
     *   <li>Result retrieval and error handling</li>
     *   <li>Resource management and optimization</li>
     * </ul>
     *
     * @param feignConfigBean retry and timeout configuration for the client
     * @param apiConfigBean connection and authentication configuration
     * @return configured HerculesManagerApi client for business operations
     */
    @Bean("HerculesManagerApi")
    public HerculesManagerApi appOpenApi(
            @Qualifier("feignConfigBean") FeignConfigBean feignConfigBean,
            @Qualifier("appOpenApiConfig") ApiConfigBean apiConfigBean) {
        return FeignUtils.getDefaultBuilder(feignConfigBean)
                .requestInterceptor(new FeignInterceptor(apiConfigBean))
                .target(HerculesManagerApi.class, apiConfigBean.getUrl());
    }

//    @ConfigurationProperties(prefix = "api.executor-master")
//    @Bean("executorMasterConfig")
//    public ApiConfigBean executorMasterConfig() {
//        return new ApiConfigBean();
//    }

//    @Bean("executorMasterApi")
//    public ExecutorMasterApi executorMasterApi(
//            @Qualifier("feignConfigBean") FeignConfigBean feignConfigBean,
//            @Qualifier("executorMasterConfig") ApiConfigBean apiConfigBean) {
//        return FeignUtils.getDefaultBuilder(feignConfigBean)
//                .requestInterceptor(new FeignInterceptor(apiConfigBean))
//                .target(ExecutorMasterApi.class, apiConfigBean.getUrl());
//    }


}
