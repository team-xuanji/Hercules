package team.magic.flute.hercules.twelve.labors.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Swagger Configuration
 *
 * <p>This configuration class sets up OpenAPI documentation for the Hercules Twelve Labors
 * business access gateway service. It provides comprehensive API documentation for all
 * business operations and endpoints exposed by the module.
 *
 * <p>The API documentation includes:
 * <ul>
 *   <li>Business operation endpoints</li>
 *   <li>Data export functionality</li>
 *   <li>Task management operations</li>
 *   <li>Authentication and authorization details</li>
 * </ul>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class SwaggerConfig {

    /**
     * Configure OpenAPI documentation for Hercules business access service.
     *
     * @return configured OpenAPI instance with service information
     */
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hercules Business Access Gateway Service")
                        .version("v1")
                        .description("Hercules Business Access Gateway Service API Documentation - Primary entry point for business operations in the Hercules ecosystem")
                        .contact(new Contact()
                                .name("Hercules Team")
                                .email("support@hercules.com")));
    }
}
