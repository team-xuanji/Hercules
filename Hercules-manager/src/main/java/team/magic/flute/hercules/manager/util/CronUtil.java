package team.magic.flute.hercules.manager.util;

import lombok.experimental.UtilityClass;
import org.quartz.*;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;


@UtilityClass
public class CronUtil {

    private final static String TRIGGER_NAME = "NextProcessTime";

    public static LocalDateTime getNextTriggerTime(String cron,LocalDateTime startDate,boolean withOutMisfire) throws ParseException {
        Date date = Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant());
        CronTrigger trigger = getCronTrigger(cron, date,TRIGGER_NAME,withOutMisfire);
        if(trigger==null){
            return null;
        }
        Date nextFireTime = trigger.getFireTimeAfter(date);
        return nextFireTime!=null?nextFireTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime():null;
    }

    public static CronTrigger getCronTrigger(String cron,Date startDate,String identity,boolean withOutMisfire) throws ParseException {
        if(!CronExpression.isValidExpression(cron)){
            return null;
        }
        TriggerBuilder<Trigger> builder = TriggerBuilder.newTrigger();
        if(!withOutMisfire && startDate!=null){
            builder.startAt(startDate);
        }
        return builder
                .withIdentity(identity)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron))
                .build();
    }

    public static LocalDateTime getPreviousTriggerTime(String cron,LocalDateTime startDate,boolean withOutMisfire) throws ParseException {
        Date date = Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant());
        CronTrigger trigger = getCronTrigger(cron, date,TRIGGER_NAME,withOutMisfire);
        if(trigger==null){
            return null;
        }
        Date previousFireTime = trigger.getPreviousFireTime();
        return previousFireTime!=null?previousFireTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime():null;
    }

    public static void main(String[] args) throws ParseException {
        System.out.println(getNextTriggerTime("0 0 * * * ?",DateUtil.parseLocalDateTime("2025-01-01 00:00:00"),true));
        System.out.println(getNextTriggerTime("0 0 * * * ?",DateUtil.parseLocalDateTime("2025-01-01 00:00:00"),false));
    }
}
