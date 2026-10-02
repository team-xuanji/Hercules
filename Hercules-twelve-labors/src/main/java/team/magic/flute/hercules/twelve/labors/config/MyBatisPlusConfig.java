package team.magic.flute.hercules.twelve.labors.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis Plus Configuration for Business Access Gateway
 *
 * <p>This configuration class sets up MyBatis Plus framework components required for
 * data access operations within the Hercules business access gateway. It configures
 * database interaction features that support various business operations including
 * task management, user authentication, and business data processing.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this configuration enables efficient and scalable data access for:
 * <ul>
 *   <li>Business task definition and execution tracking</li>
 *   <li>User authentication and authorization data</li>
 *   <li>Business operation audit logs and history</li>
 *   <li>Configuration and metadata management</li>
 * </ul>
 *
 * <p><strong>Database Features:</strong>
 * <ul>
 *   <li>Automatic pagination support for large result sets</li>
 *   <li>MySQL-optimized query execution</li>
 *   <li>Performance monitoring and optimization</li>
 *   <li>Transaction management for business operations</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> This configuration supports the business access gateway's
 * data persistence requirements, ensuring reliable and efficient storage and retrieval
 * of business operation data, user information, and system metadata.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Configuration
public class MyBatisPlusConfig {

    /**
     * Configure MyBatis Plus interceptors for enhanced database operations
     *
     * <p>Sets up interceptors that provide additional functionality for database
     * operations within the business access gateway, including automatic pagination
     * support for handling large datasets efficiently.
     *
     * <p><strong>Configured Interceptors:</strong>
     * <ul>
     *   <li>PaginationInnerInterceptor - Automatic pagination for MySQL queries</li>
     * </ul>
     *
     * <p>The pagination interceptor is particularly important for business operations
     * that need to handle large amounts of data, such as task history queries,
     * audit log retrieval, and bulk data export operations.
     *
     * @return configured MybatisPlusInterceptor with pagination support
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}