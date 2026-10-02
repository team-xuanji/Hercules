package team.magic.flute.hercules.twelve.labors;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hercules Twelve Labors Application - Business Access Gateway
 *
 * <p>This is the main entry point for the Hercules Twelve Labors module, which serves as the
 * <strong>primary business access gateway</strong> for the Hercules distributed task execution
 * ecosystem. Named after the legendary twelve labors of Hercules, this module is designed to
 * handle diverse and complex business operations through a unified, enterprise-grade platform.
 *
 * <p><strong>Core Purpose:</strong> While data export functionality represents one of its key
 * capabilities, the module is architected as a comprehensive business service gateway that can
 * accommodate various business scenarios and operational requirements.
 *
 * <p><strong>Current Business Functions:</strong>
 * <ul>
 *   <li><strong>Data Export Operations:</strong> Multi-format export support (CSV, JSON, Parquet, XLSX)</li>
 *   <li><strong>Task Management:</strong> Business task definition, execution, and monitoring</li>
 *   <li><strong>Integration Services:</strong> Various data sources (RDS, MySQL) and cloud storage (OSS, S3)</li>
 *   <li><strong>User Access Control:</strong> Authentication, authorization, and audit logging</li>
 *   <li><strong>Process Orchestration:</strong> Workflow management and task scheduling</li>
 * </ul>
 *
 * <p><strong>Extensible Architecture:</strong> The module is designed for future expansion to support
 * additional business functions such as:
 * <ul>
 *   <li>Document processing and transformation</li>
 *   <li>Notification and communication services</li>
 *   <li>Advanced analytics and reporting</li>
 *   <li>Third-party system integration</li>
 *   <li>Workflow automation and orchestration</li>
 * </ul>
 *
 * <p><strong>Technical Architecture:</strong>
 * <ul>
 *   <li>Spring Boot-based microservice architecture</li>
 *   <li>RESTful API design with comprehensive validation</li>
 *   <li>MyBatis-Plus for database operations</li>
 *   <li>Scheduled task processing with automatic cleanup</li>
 *   <li>Integration with Hercules Manager for distributed task execution</li>
 *   <li>Plugin-based architecture for business function extensibility</li>
 * </ul>
 *
 * <p>The application leverages the Hercules ecosystem for distributed task execution,
 * providing a web-based interface for business users to configure and monitor various
 * business operations. It supports both synchronous and asynchronous processing modes
 * with comprehensive error handling and retry mechanisms.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
@EnableScheduling
public class HerculesTwelveLaborsApplication {

    /**
     * Main method to start the Hercules Twelve Labors application.
     *
     * <p>This method initializes the Spring Boot application context and starts
     * the embedded web server. The application will be available for handling
     * HTTP requests and processing scheduled tasks.
     *
     * @param args command line arguments passed to the application
     */
    public static void main( String[] args ) {
        SpringApplication.run(HerculesTwelveLaborsApplication.class, args);
    }
}
