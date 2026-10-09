package team.magic.flute.hercules;

import team.magic.flute.hercules.manager.HerculesManagerApplication;
import team.magic.flute.hercules.manager.controller.TaskManagerController;
import team.magic.flute.hercules.manager.global.RecoverEventLevel;
import team.magic.flute.hercules.manager.schedule.CronTaskDispatch;
import team.magic.flute.hercules.manager.schedule.RecoverTaskDispatch;
import team.magic.flute.hercules.manager.service.HerculesCronJobManagerService;
import team.magic.flute.hercules.manager.vo.AsyncRetryOneTaskRequestVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.TimeUnit;

/**
 * Integration tests: boot the full Spring context against the qa/test profile
 * (MySQL, OSS) and exercise live controllers. Tagged so the default
 * {@code mvn test} run skips them; run explicitly via the {@code integration} group.
 */
@SpringBootTest(classes = {HerculesManagerApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)// 指定启动类
@ActiveProfiles({"qa", "test"})
@Tag("integration")
public class Test01 {

    @Autowired
    private TaskManagerController taskManagerController;

    @Autowired
    private HerculesCronJobManagerService cronJobManagerService;

    @Autowired
    private CronTaskDispatch cronTaskDispatch;

    @Autowired
    private RecoverTaskDispatch recoverTaskDispatch;

    @Test
    public void test01(){
        System.out.println("test01");
    }

    @Test
    public void herculesManagerTest02() throws InterruptedException {
        System.out.println("test01");
        TimeUnit.HOURS.sleep(1);
    }



    @Test
    public void herculesManagerTest04() throws Exception {
        taskManagerController.asyncRetryOneTask(new AsyncRetryOneTaskRequestVO()
                .setErrorMessage("Just testing").setTaskId("00230ffe4b0b56be12664020ec971bb1"));
    }

    @Test
    public void herculesManagerTest05() throws Exception {
        taskManagerController.asyncRetryOneTask(new AsyncRetryOneTaskRequestVO()
                .setErrorMessage("Just testing").setTaskId("00230ffe4b0b56be12664020ec971bb1"));
    }


}
