package team.magic.flute.hercules.executor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hercules Executor Application
 *
 * <p>This is the main entry point for the Hercules Executor module, which is responsible for:
 * <ul>
 *   <li>Task execution and processing</li>
 *   <li>Plugin loading and management</li>
 *   <li>Resource downloading and caching</li>
 *   <li>Task status reporting and coordination</li>
 * </ul>
 *
 * <p>The Hercules Executor acts as the execution engine for the Hercules task system,
 * consuming tasks from the Hercules Manager and executing them using dynamically loaded plugins.
 * It supports various execution environments and can be configured for different business scenarios.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Multi-threaded task execution with configurable parallelism</li>
 *   <li>Dynamic plugin loading from remote resources</li>
 *   <li>DuckDB integration for data processing capabilities</li>
 *   <li>Automatic task recovery and retry mechanisms</li>
 *   <li>Resource management and cleanup</li>
 * </ul>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
@EnableScheduling
public class HerculesExecutorApplication {

    /**
     * Main method to start the Hercules Executor application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(HerculesExecutorApplication.class, args);
    }
}
