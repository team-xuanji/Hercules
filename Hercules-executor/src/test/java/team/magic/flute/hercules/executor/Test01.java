package team.magic.flute.hercules.executor;


import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

/**
 * Integration tests: boot the full executor Spring context against the qa/test
 * profile. Tagged so the default {@code mvn test} run skips them.
 */
@SpringBootTest(classes = {HerculesExecutorApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)// Specify startup class
@ActiveProfiles({"qa", "test"})
@Tag("integration")
public class Test01 {
    @Test
    public void test01(){
        System.out.println("test01");
    }

    @Test
    public void executorQaTest() throws InterruptedException {
        System.out.println("test01");
        TimeUnit.HOURS.sleep(1);
    }
}
