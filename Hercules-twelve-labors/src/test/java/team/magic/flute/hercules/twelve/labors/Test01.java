package team.magic.flute.hercules.twelve.labors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskPO;
import team.magic.flute.hercules.twelve.labors.service.CmsDataDownloadTaskService;

import java.util.concurrent.TimeUnit;

@SpringBootTest(classes = {HerculesTwelveLaborsApplication.class}, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)// Specify startup class
@ActiveProfiles({"qa", "test"})
public class Test01 {

    @Autowired
    private CmsDataDownloadTaskService taskService;
    public void test01() throws InterruptedException {
        System.out.println("test01");
        TimeUnit.HOURS.sleep(1);
    }

    public void test011() throws InterruptedException {
        System.out.println("test011");
        TimeUnit.HOURS.sleep(1);
    }

    @Test
    public void test012() throws InterruptedException {
        for(int i=0;i<10;i++){
            taskService.saveOrUpdate(new CmsDataDownloadTaskPO()
                    .setBusinessKey("test012")
                    .setTaskStatus("INIT")
                    .setFilePath("test/path")
                    .setAppKey("app-key-001")
                    .setUserId("1001")
                    .setOperatorSource("web-console"));
        }
    }

}
