package team.magic.flute.hercules.common.http;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

@Data
@Accessors(chain=true)
public class BatchTaskLockProcessResult {
    private boolean allSuccess;
    private Map<String, TaskInfoLockResult> lockResults;
}
