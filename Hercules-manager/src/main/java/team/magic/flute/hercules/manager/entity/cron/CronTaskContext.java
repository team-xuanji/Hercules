package team.magic.flute.hercules.manager.entity.cron;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import team.magic.flute.hercules.manager.dao.po.HerculesCronJobs;
import team.magic.flute.hercules.manager.entity.cron.impl.BasicTimeRangContext;

/**
 * In the scheduling context, since tasks initiated by scheduling often need
 * to carry parameters, and in reality, each type of scheduling business
 * requires different parameters for generating executable tasks,
 * this is designed as a strategy class.
 * <p>If needed, scheduling types can be extended.
 * <p>For example:
 * <p>1.Cursor queries require the querying party to pass a cursor parameter.
 * <p>2.The relevant scenario for time slice queries requires the querying party to pass a parameter for the time slice.
 * <p>3.When querying the HTTP interface, it is necessary to pass third-party information such as a token.
 * <p>Although context information in many scenarios could indeed be managed and obtained
 * by the executor itself, making it seem unnecessary for the manager to generate and pass down context.
 * However, first, we do not restrict users' freedom to manage context information within the executor.
 * Second, much context information is actually generated based on fixed rules (such as time-slice-related
 * parameters), which can be calculated in batches upstream, allowing the executor to focus solely on execution.
 * This approach reduces the complexity of the executor and improves the throughput of task distribution
 * and execution. By combining these two methods, we can relatively easily address a series of issues
 * regarding how context information for executing tasks is generated and maintained.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "cronTaskContextType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = BasicTimeRangContext.class, name = "TIME_RANGE")
})
public interface CronTaskContext {
    TaskUpdateInfo parseTaskContext(HerculesCronJobs cronJobs) throws Exception;
}
