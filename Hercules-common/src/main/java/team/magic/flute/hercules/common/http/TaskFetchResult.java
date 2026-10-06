package team.magic.flute.hercules.common.http;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Payload of the binary fetch channel ({@code tryFetchTasksWithByteArray}).
 *
 * <p>{@link #crossPartition} is true when the manager could not compute this
 * executor's bucket range (e.g. within seconds after a restart, before the
 * executor list is re-partitioned) and fell back to scanning all buckets.
 * Executors should then lock tasks one by one instead of in one batch, to
 * reduce contention with executors that do have a dedicated bucket range.
 */
@Data
@Accessors(chain=true)
public class TaskFetchResult {
    private List<HerculesRunnableTaskInfo> taskInfoList;
    private boolean crossPartition;
    public boolean isEmpty() {
        return taskInfoList == null || taskInfoList.isEmpty();
    }
}
