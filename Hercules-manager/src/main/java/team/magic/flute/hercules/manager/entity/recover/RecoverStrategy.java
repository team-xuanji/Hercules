package team.magic.flute.hercules.manager.entity.recover;


import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import team.magic.flute.hercules.manager.entity.recover.impl.FixedIntervalStrategy;
import team.magic.flute.hercules.manager.entity.recover.impl.GradualDecelerationStrategy;

import java.time.LocalDateTime;


/**
 * Context information required for asynchronous task recovery.
 * Typically, this is used to determine the frequency and number of attempts for task recovery.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "recoverStrategyType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = FixedIntervalStrategy.class, name = "FIXED_INTERVAL"),
        @JsonSubTypes.Type(value = GradualDecelerationStrategy.class, name = "GRADUAL_DECELERATION")
})
public interface RecoverStrategy {
    /**
     * Whether the asynchronous retry has reached the termination condition.
     * @return
     */
    boolean processFinished();

    /**
     * Process the asynchronous retry and return the next trigger time.
     * @return
     */
    LocalDateTime processAndIncrementNextTimeStamp();

    /**
     * Whether to persist the task after the asynchronous retry is finished.
     * @return
     */
    boolean isPersistence();
}
