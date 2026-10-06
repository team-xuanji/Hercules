package team.magic.flute.hercules.common.http;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Result of a batch lock attempt. Exactly one {@link TaskInfoLockResult} is
 * reported per requested task id; {@link #allSuccess} is true only when every
 * requested task reached {@link TaskInfoLockResult#SUCCESS}.
 *
 * <p>Callers must tolerate a null {@link #lockResults} (e.g. the empty-request
 * early return) and treat a missing task id as an unsuccessful lock.
 */
@Data
@Accessors(chain=true)
public class BatchTaskLockProcessResult {
    private boolean allSuccess;
    private Map<String, TaskInfoLockResult> lockResults;
}
