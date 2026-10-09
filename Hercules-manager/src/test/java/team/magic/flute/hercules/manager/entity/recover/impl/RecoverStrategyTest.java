package team.magic.flute.hercules.manager.entity.recover.impl;

import org.junit.jupiter.api.Test;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure unit tests for the recover-strategy implementations and their Jackson
 * polymorphic wiring. No Spring context, no DB. These pin the retry cadence and
 * finish-threshold invariants the recover dispatch loop depends on.
 */
class RecoverStrategyTest {

    @Test
    void fixedIntervalDefaultsAndIncrement() {
        FixedIntervalStrategy s = new FixedIntervalStrategy();
        assertEquals(FixedIntervalStrategy.DEFAULT_DAILY_MS, s.getDailyMs());
        assertEquals(FixedIntervalStrategy.DEFAULT_MAX_RETRY_TIMES, s.getMaxRetryTimes());
        assertEquals(0L, s.getRetryTimes());
        assertFalse(s.processFinished(), "a fresh strategy must not be finished");

        // Each increment advances retryTimes by exactly one and schedules ~dailyMs ahead.
        long expectedRetry = 0L;
        for (int i = 0; i < 10; i++) {
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime next = s.processAndIncrementNextTimeStamp();
            assertEquals(++expectedRetry, s.getRetryTimes());
            // next must be roughly now + dailyMs/1000 seconds (allow a generous clock skew window).
            assertTrue(next.isAfter(now.plusSeconds(FixedIntervalStrategy.DEFAULT_DAILY_MS / 1000 - 5)));
            assertFalse(s.processFinished());
        }
    }

    @Test
    void fixedIntervalFinishesAfterMaxRetryThreshold() {
        FixedIntervalStrategy s = new FixedIntervalStrategy();
        // Burn through the whole retry budget. finished() flips true once retryTimes
        // exceeds maxRetryTimes (strictly greater — the boundary itself still runs).
        for (int i = 0; i < s.getMaxRetryTimes(); i++) {
            s.processAndIncrementNextTimeStamp();
        }
        assertEquals(s.getMaxRetryTimes(), s.getRetryTimes());
        assertFalse(s.processFinished(), "at exactly maxRetryTimes the strategy is NOT yet finished");

        s.processAndIncrementNextTimeStamp(); // retryTimes = maxRetryTimes + 1
        assertTrue(s.processFinished(), "one past maxRetryTimes must mark the strategy finished");
    }

    @Test
    void gradualDecelerationStepCapsAtMaxStep() {
        GradualDecelerationStrategy s = new GradualDecelerationStrategy();
        assertEquals(GradualDecelerationStrategy.DEFAULT_BASIC_STEP, s.getBasicStep());
        assertEquals(GradualDecelerationStrategy.DEFAULT_MAX_STEP, s.getMaxStep());
        assertEquals(GradualDecelerationStrategy.DEFAULT_INCREMENT_STEP, s.getStepIncrement());

        // Each increment grows the next interval by stepIncrement until it hits maxStep.
        // step = basicStep + n*stepIncrement, capped at maxStep; reaching the cap needs
        // ceil((maxStep-basicStep)/stepIncrement) = ~1007 increments.
        for (int i = 0; i < 1200; i++) {
            s.processAndIncrementNextTimeStamp();
        }
        assertEquals(GradualDecelerationStrategy.DEFAULT_MAX_STEP, s.getStep(),
                "step must cap at maxStep and never overflow");
        assertEquals(1200L, s.getRetryTimes());
        assertTrue(s.processFinished(), "1200 retries exceeds maxRetryTimes(150), must be finished");
    }

    @Test
    void gradualDecelerationIntervalMonotonicallyNonDecreasingUntilCap() {
        GradualDecelerationStrategy s = new GradualDecelerationStrategy();
        long prevStep = s.getBasicStep();
        for (int i = 0; i < 20; i++) {
            s.processAndIncrementNextTimeStamp();
            assertTrue(s.getStep() >= prevStep, "step must never decrease before reaching the cap");
            assertTrue(s.getStep() <= GradualDecelerationStrategy.DEFAULT_MAX_STEP);
            prevStep = s.getStep();
        }
    }

    @Test
    void fixedIntervalRoundTripsThroughJacksonAsRecoverStrategy() {
        // The recover context is stored as JSON keyed by the discriminator; the async
        // path must deserialize a FIXED_INTERVAL payload back into the right subclass.
        FixedIntervalStrategy original = new FixedIntervalStrategy().setDailyMs(12_000L).setMaxRetryTimes(7L);
        String json = JacksonUtils.writeValueAsString(original);

        RecoverStrategy recovered = JacksonUtils.readValue(json, RecoverStrategy.class);
        assertNotNull(recovered);
        assertInstanceOf(FixedIntervalStrategy.class, recovered);
        FixedIntervalStrategy typed = (FixedIntervalStrategy) recovered;
        assertEquals(12_000L, typed.getDailyMs());
        assertEquals(7L, typed.getMaxRetryTimes());
    }

    @Test
    void gradualDecelerationRoundTripsThroughJacksonAsRecoverStrategy() {
        GradualDecelerationStrategy original = new GradualDecelerationStrategy()
                .setBasicStep(1_000L)
                .setMaxStep(60_000L)
                .setStepIncrement(500L)
                .setMaxRetryTimes(3L);
        String json = JacksonUtils.writeValueAsString(original);

        RecoverStrategy recovered = JacksonUtils.readValue(json, RecoverStrategy.class);
        assertInstanceOf(GradualDecelerationStrategy.class, recovered);
        GradualDecelerationStrategy typed = (GradualDecelerationStrategy) recovered;
        assertEquals(1_000L, typed.getBasicStep());
        assertEquals(60_000L, typed.getMaxStep());
        assertEquals(500L, typed.getStepIncrement());
        assertEquals(3L, typed.getMaxRetryTimes());
    }
}
