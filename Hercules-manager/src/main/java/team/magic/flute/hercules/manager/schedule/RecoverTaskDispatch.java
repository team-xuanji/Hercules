package team.magic.flute.hercules.manager.schedule;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.magic.flute.hercules.manager.global.RecoverEventLevel;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Component
@Slf4j
@EnableAsync
public class RecoverTaskDispatch {
    private final Lock recoverLock = new ReentrantLock();

    @Autowired
    private ScheduleBusinessProcessor processor;

    private volatile long warmRecoverWatermark = System.currentTimeMillis();
    private volatile long coldRecoverWatermark = System.currentTimeMillis();

    @Async
    @Scheduled(fixedRate = 10000,initialDelay = 60000)
    public void recoverHot(){
        if(recoverLock.tryLock()){
            try{
                long waterMark = System.currentTimeMillis();
                log.info("Starting recovery of hot data.");
                processor.recoverEvents(RecoverEventLevel.HOT);
                if(waterMark>=warmRecoverWatermark){
                    log.info("Starting recovery of warm data.");
                    processor.recoverEvents(RecoverEventLevel.WARM);
                    long warmRecoverInterval = 1800000;
                    warmRecoverWatermark = waterMark+ warmRecoverInterval;
                }

                if(waterMark>=coldRecoverWatermark){
                    log.info("Starting recovery of cold data.");
                    processor.recoverEvents(RecoverEventLevel.COLD);
                    long coldRecoverInterval = 3600000;
                    coldRecoverWatermark = waterMark+ coldRecoverInterval;
                }
            }finally {
                recoverLock.unlock();
            }
        }
    }
}
