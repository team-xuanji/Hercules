package team.magic.flute.hercules.manager.config;


import team.magic.flute.hercules.manager.global.RecoverEventLevel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;


public class RecoverConfig {

    private static final long HOT_DAILY =3600L*1000L;

    private static final long WARM_DAILY =3600L*1000L*24L;


    public static RecoverEventLevel getRecoverStrategy(LocalDateTime timeStamp){
        if(timeStamp==null){
            return getRecoverStrategy(-1);
        }
        ZoneId zoneId = ZoneId.systemDefault();
        ZonedDateTime zonedDateTime = timeStamp.atZone(zoneId);
        return getRecoverStrategy(zonedDateTime.toInstant().toEpochMilli());
    }
    /**
     * Calculate the data recovery strategy based on the
     * difference between the target timestamp and the current time.
     * @param nextProcessTimeStamp
     * @return
     */
    public static RecoverEventLevel getRecoverStrategy(long nextProcessTimeStamp){
        if(nextProcessTimeStamp==-1){
            return RecoverEventLevel.DEAD;
        } else if (nextProcessTimeStamp==0L) {
            return RecoverEventLevel.UNKNOWN;
        }
        long diff = nextProcessTimeStamp - System.currentTimeMillis();
        if(diff<HOT_DAILY){
            return RecoverEventLevel.HOT;
        }else if(diff<WARM_DAILY){
            return RecoverEventLevel.WARM;
        }else {
            return RecoverEventLevel.COLD;
        }
    }
}
