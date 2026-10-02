package team.magic.flute.hercules.manager.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Objects;


@UtilityClass
@Slf4j
public class DateUtil {
    private static final String[] PATTERN = {"yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss","yyyyMMdd"};
    private static final DateTimeFormatter[] FORMAT_PATTERNS = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyyMMdd")};
    public Date parseDate(String str) throws ParseException {
        return DateUtils.parseDate(str, PATTERN);
    }

    public String formatLocalDateTime2Long(LocalDateTime localDateTime){
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Objects.toString(instant.toEpochMilli());
    }

    public LocalDateTime parseLocalDateTime(String customDateTimeStr) throws ParseException{
        for (DateTimeFormatter formatPattern : FORMAT_PATTERNS) {
            try{
                return LocalDateTime.parse(customDateTimeStr, formatPattern);
            } catch (DateTimeParseException e){
                log.debug(e.getParsedString());
            }
        }
        throw new IllegalArgumentException("Date string must be in format: yyyy-MM-dd, yyyy-MM-dd HH:mm:ss, or yyyyMMdd");
    }

    public String formatLong(long timestamp){
        return DateFormatUtils.format(timestamp,"yyyy-MM-dd HH:mm:ss");
    }

    public static long dateDiffFromNowWithOutError(String startTime) {
        // Get the millisecond time difference between two times
        try {
            // Milliseconds in one day
            long nd = 1000L * 24L * 60L * 60L;
            long diff;
            diff = System.currentTimeMillis() - parseDate(startTime).getTime();
            // Calculate how many days difference
            return diff / nd;
        } catch (Exception e) {
            return -1L;
        }
    }
}
