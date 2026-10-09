package team.magic.flute.hercules.manager.util;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for {@link CronUtil}. No Spring context, no DB. Quartz is on
 * the classpath via the manager's quartz dependency. These pin the cron-parsing
 * contract the cron-task scheduler relies on: valid crons yield future fire
 * times, invalid crons yield null (never throw).
 */
class CronUtilTest {

    private static final String EVERY_HOUR = "0 0 * * * ?";
    private static final String EVERY_MINUTE = "0 * * * * ?";

    @Test
    void validCronYieldsFutureNextFireTimeWithoutMisfire() throws ParseException {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 0, 0);
        LocalDateTime next = CronUtil.getNextTriggerTime(EVERY_HOUR, start, true);

        assertNotNull(next);
        assertTrue(next.isAfter(start), "next fire time must be after the start time");
        // "0 0 * * * ?" fires on the top of each hour; next after 00:00:00 is 01:00:00.
        assertTrue(next.getMinute() == 0 && next.getSecond() == 0);
    }

    @Test
    void validCronYieldsFutureNextFireTimeWithStartAt() throws ParseException {
        LocalDateTime start = LocalDateTime.of(2025, 1, 1, 0, 30, 0);
        LocalDateTime next = CronUtil.getNextTriggerTime(EVERY_HOUR, start, false);

        assertNotNull(next);
        assertTrue(next.isAfter(start));
        assertTrue(next.getMinute() == 0 && next.getSecond() == 0);
    }

    @Test
    void everyMinuteCronFiresOnMinuteBoundary() throws ParseException {
        LocalDateTime start = LocalDateTime.of(2025, 6, 15, 12, 0, 0);
        LocalDateTime next = CronUtil.getNextTriggerTime(EVERY_MINUTE, start, true);

        assertNotNull(next);
        assertTrue(next.isAfter(start));
        // "0 * * * * ?" fires at second 0 of every minute; the next fire must land
        // on a minute boundary. (Quartz may advance past the immediate minute
        // depending on misfire/startAt semantics, so we only pin the boundary.)
        assertEquals(0, next.getSecond());
    }

    @Test
    void invalidCronReturnsNullNextTriggerTime() throws ParseException {
        // Garbage cron must return null, not throw — the scheduler treats null as
        // "unschedulable" and skips the job rather than crashing the dispatch loop.
        LocalDateTime start = LocalDateTime.now();
        assertNull(CronUtil.getNextTriggerTime("not-a-cron-expression", start, true));
    }

    @Test
    void invalidCronReturnsNullCronTrigger() throws ParseException {
        Date date = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
        CronTrigger trigger = CronUtil.getCronTrigger("xyz", date, "id", true);
        assertNull(trigger);
    }
}
