package team.magic.flute.hercules.manager.config;



import team.magic.flute.hercules.manager.util.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

/**
 * @author Aida
 * @since 2022-12-05
 */
@Component
@Slf4j
public class JobInitlizeApplicationRunner implements ApplicationRunner {

    @Autowired
    private ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Initialization started.");
        SpringContextUtil.setApplicationContext(applicationContext);
        log.info("Initialization complete.");
    }
}
