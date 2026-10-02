package team.magic.flute.hercules.twelve.labors.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import team.magic.flute.hercules.twelve.labors.auth.UserInfoResolver;

import java.util.List;

/**
 * Web MVC Configuration for Business Access Gateway
 *
 * <p>This configuration class sets up the web layer components for the Hercules business
 * access gateway, including custom argument resolvers, static resource handling, and
 * API documentation access. It provides the foundation for web-based business operations
 * and user interactions with the gateway services.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this configuration enables:
 * <ul>
 *   <li>Seamless user authentication context injection into business endpoints</li>
 *   <li>API documentation access for business operation integration</li>
 *   <li>Static resource serving for web-based business interfaces</li>
 *   <li>Cross-origin request support for distributed business applications</li>
 * </ul>
 *
 * <p><strong>Web Features:</strong>
 * <ul>
 *   <li>Custom argument resolvers for user context injection</li>
 *   <li>Swagger UI integration for API documentation</li>
 *   <li>Static resource handling for web assets</li>
 *   <li>View controller configuration for navigation</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> This configuration supports the business access gateway's
 * web interface requirements, ensuring that business users and external systems can
 * effectively interact with the gateway through web-based interfaces and APIs.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configure custom argument resolvers for business operation endpoints
     *
     * <p>Adds custom argument resolvers that enable automatic injection of user
     * authentication and authorization context into business operation endpoints.
     * This provides seamless access to user information for business logic
     * execution and audit logging.
     *
     * <p>The UserInfoResolver automatically populates UserAccessInfo parameters
     * in controller methods when annotated with @UserLogInInfo, enabling
     * business operations to access user context without manual extraction.
     *
     * @param argumentResolvers list of argument resolvers to configure
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new UserInfoResolver());
    }

//    /**
//     * Configure CORS mappings for cross-origin business application access
//     *
//     * <p>Can be enabled for local testing to avoid HTML page cross-origin access issues.
//     * In production environments, CORS should be configured through proper infrastructure
//     * components like API gateways or load balancers for security reasons.
//     *
//     * @param registry CORS registry for configuration
//     */
//    @Override
//    public void addCorsMappings(CorsRegistry registry) {
//        registry.addMapping("/**")
//                .allowedOrigins("*")
//                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
//                .allowedHeaders("*");
//    }

    /**
     * Configure static resource handlers for web assets and API documentation
     *
     * <p>Sets up resource handlers for serving static web assets and API documentation
     * through the business access gateway. This includes Swagger UI resources for
     * API documentation and general static resources for web-based business interfaces.
     *
     * <p><strong>Configured Resources:</strong>
     * <ul>
     *   <li>Swagger UI assets for API documentation access</li>
     *   <li>WebJars resources for frontend dependencies</li>
     *   <li>Static web assets for business interface components</li>
     * </ul>
     *
     * @param registry resource handler registry for configuration
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Configure Swagger UI static resources
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");

        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");

        // Maintain default static resource handling
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/", "classpath:/public/");
    }

    /**
     * Configure view controllers for navigation and redirects
     *
     * <p>Sets up view controllers that handle navigation and redirects within
     * the business access gateway web interface. This includes redirects to
     * API documentation and other business interface components.
     *
     * @param registry view controller registry for configuration
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Add Swagger UI redirect
        registry.addRedirectViewController("/swagger-ui", "/swagger-ui.html");
    }
}
