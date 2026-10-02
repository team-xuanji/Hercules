package team.magic.flute.hercules.manager.entity.recover.impl;


import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * The interval between retries gradually increases.
 */
@Slf4j
@Data
@Accessors(chain=true)
public class GradualDecelerationStrategy implements RecoverStrategy {
    private boolean persistence = false;
    private long basicStep = DEFAULT_BASIC_STEP;
    private long maxStep = DEFAULT_MAX_STEP;
    private long stepIncrement = DEFAULT_INCREMENT_STEP;
    private volatile long step = basicStep;
    private volatile long retryTimes = 0L;
    private long maxRetryTimes = DEFAULT_MAX_RETRY_TIMES;

    public static final long DEFAULT_BASIC_STEP = 60L * 10L * 1000L;
    public static final long DEFAULT_MAX_STEP = 3600L * 24L * 1000L*7L;
    public static final long DEFAULT_INCREMENT_STEP = 60L * 10L * 1000L;
    public static final long DEFAULT_MAX_RETRY_TIMES = 150L;

    @Override
    public boolean processFinished() {
        return retryTimes>maxRetryTimes;
    }

    @Override
    public synchronized LocalDateTime processAndIncrementNextTimeStamp() {
        LocalDateTime nextTriggerTime = LocalDateTime.now();
        if(retryTimes<maxRetryTimes){
            nextTriggerTime = nextTriggerTime.minusSeconds(-1*step/1000);
        }
        if(step<=0L){
            step = basicStep;
        }else{
            step = Math.min(step + stepIncrement,maxStep);
        }
        retryTimes++;
        return nextTriggerTime;
    }

    @Override
    public boolean isPersistence() {
        return persistence;
    }
}
