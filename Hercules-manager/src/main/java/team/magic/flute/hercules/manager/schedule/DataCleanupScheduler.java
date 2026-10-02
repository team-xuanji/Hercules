package team.magic.flute.hercules.manager.schedule;

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
public class DataCleanupScheduler {
    private final Lock taskScheduleLock = new ReentrantLock();
    @Autowired
    private ManagerInstanceCoordinator managerInstanceCoordinator;
    @Autowired
    private ScheduleBusinessProcessor processor;
    private volatile long cleanUpWaterMark = System.currentTimeMillis();


    @Async
    @Scheduled(fixedRate = 60000,initialDelay = 60000)
    public void run(){
        log.debug("Starting metadata compression task");
        try{
            if(managerInstanceCoordinator.isMaster() && taskScheduleLock.tryLock()){
                processor.removeTooOldExecutorInfo();
                if(System.currentTimeMillis()>cleanUpWaterMark){
                    processor.removeTooOldTask();
                    processor.changeDeadTaskToCancelled();
                    cleanUpWaterMark = System.currentTimeMillis()+1200000;
                }
            }
        } catch (Exception e) {
            log.error("Execution of metadata compression task failed!");
            log.error(e.getMessage(),e);
        }finally {
            taskScheduleLock.unlock();
        }
        log.debug("Metadata compression task execution completed");
    }

}
