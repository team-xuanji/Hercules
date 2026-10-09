package team.magic.flute.hercules;

import org.junit.jupiter.api.Test;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;
import team.magic.flute.hercules.manager.entity.recover.impl.FixedIntervalStrategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class Test02 {
    /**
     * Polymorphic round-trip: RecoverStrategy is annotated with @JsonTypeInfo on
     * the {@code recoverStrategyType} discriminator, so a payload carrying that
     * discriminator must deserialize back into the matching strategy subclass.
     * This pins the wiring the async-recover path depends on.
     */
    @Test
    public void test01(){
        String data = "{\"recoverStrategyType\":\"FIXED_INTERVAL\"}";
        RecoverStrategy recoverStrategy = JacksonUtils.readValue(data, RecoverStrategy.class);
        assertInstanceOf(FixedIntervalStrategy.class, recoverStrategy);
        // Defaults must survive a JSON round-trip (NON_NULL serialization drops nulls,
        // but the FIXED_INTERVAL defaults are non-null primitives/longs, so they persist).
        assertEquals(FixedIntervalStrategy.DEFAULT_DAILY_MS, ((FixedIntervalStrategy) recoverStrategy).getDailyMs());
        System.out.println(JacksonUtils.writeValueAsString(recoverStrategy));
    }
}
