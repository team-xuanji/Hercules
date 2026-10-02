package team.magic.flute.hercules.manager.entity.recover.impl;


import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * Fixed interval retry.
 */
@Slf4j
@Data
@Accessors(chain = true)
public class FixedIntervalStrategy implements RecoverStrategy {
    private boolean persistence = false;
    private long dailyMs = DEFAULT_DAILY_MS;
    private long maxRetryTimes = DEFAULT_MAX_RETRY_TIMES;
    private volatile long retryTimes = 0L;

    public static final long DEFAULT_DAILY_MS = 60L * 10L * 1000L;
    public static final long DEFAULT_MAX_RETRY_TIMES = 150L;

    @Override
    public boolean processFinished() {
        return retryTimes>maxRetryTimes;
    }

    @Override
    public synchronized LocalDateTime processAndIncrementNextTimeStamp() {
        LocalDateTime nextTriggerTime = LocalDateTime.now();
        if(retryTimes<=maxRetryTimes){
            nextTriggerTime = nextTriggerTime.minusSeconds(-1*dailyMs/1000);
            retryTimes++;
        }
        return nextTriggerTime;
    }

    @Override
    public boolean isPersistence() {
        return persistence;
    }
}
