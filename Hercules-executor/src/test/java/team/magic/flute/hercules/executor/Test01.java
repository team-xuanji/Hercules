package team.magic.flute.hercules.executor;


import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = {HerculesExecutorApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)// Specify startup class
@ActiveProfiles({"qa", "test"})
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
