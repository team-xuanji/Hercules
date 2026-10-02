package team.magic.flute.hercules.manager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Hercules Manager Application
 *
 * <p>This is the main entry point for the Hercules Manager module, which is responsible for:
 * <ul>
 *   <li>Task definition and management</li>
 *   <li>Scheduled task management</li>
 *   <li>Plugin registration and management</li>
 *   <li>Task orchestration and coordination</li>
 * </ul>
 *
 * <p>The Hercules Manager acts as the central control plane for the Hercules task execution system,
 * providing REST APIs for task submission, monitoring, and plugin management.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@SpringBootApplication
@EnableScheduling
public class HerculesManagerApplication {

    /**
     * Main method to start the Hercules Manager application.
     *
     * @param args command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(HerculesManagerApplication.class, args);
    }
}
