package team.magic.flute.hercules.manager.schedule;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@EnableAsync
public class CronTaskDispatch {
    @Autowired
    private ScheduleBusinessProcessor processor;
    private final Lock taskScheduleLock = new ReentrantLock();

    @Async
    @Scheduled(fixedRate = 1000,initialDelay = 30000)
    @SneakyThrows(Exception.class)
    public void run(){
        if(taskScheduleLock.tryLock()){
            try{
                log.debug("Start executing scheduled tasks.");
                processor.cronJobProcess();
            }catch (Exception e){
                log.error("An exception occurred while executing the scheduled task!");
                log.error(e.getMessage(),e);
                throw e;
            }finally {
                taskScheduleLock.unlock();
            }
        }else{
            log.info("Failed to acquire the lock, skipping execution.");
        }
    }

}
