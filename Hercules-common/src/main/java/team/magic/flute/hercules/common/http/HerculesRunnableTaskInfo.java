package team.magic.flute.hercules.common.http;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import team.magic.flute.hercules.common.status.TaskType;

import javax.validation.constraints.NotBlank;

@Data
@Accessors(chain = true)
public class HerculesRunnableTaskInfo {
    /**
     * It can be left blank and will be automatically generated.
     * If filled in, ensure that tasks with the same ID do not conflict.
     * In addition, for a derived forward task, if the ID is empty,
     * the executor will use the buildDefaultUniId method to fill in the missing ID field.
     */
    private String id;
    /**
     * Business Grouping of Executors
     */
    @NotBlank
    private String executorRegion;
    /**
     * Description
     */
    private String desc;
    /**
     * plugin-key
     */
    @NotBlank
    private String pluginHandle;
    /**
     * plugin-business-group
     */
    @NotBlank
    private String pluginGroup;
    /**
     * The running context structure refers to the request in hercules-manager.
     */
    @NotBlank
    private String context;
    /**
     * Maximum number of retries for synchronization
     */
    private Integer maxRetryTimes;
    /**
     * The async-recover context structure refers to the request in hercules-manager.
     */
    private String asyncRecoverContext;

    /**
     * taskType,Once,Cron,Recover,etc.
     */
    private TaskType fromType;

    /**
     * sourceId.
     * If this task is triggered by a scheduled task, this value is the ID of the scheduled task.
     * For a FORWARD task, this value is the ID of the parent task that forwarded it.
     */
    private String fromSourceId;

    /**
     * Forward-chain depth. Derived by the manager at submission time: 0 for
     * business/cron/recover tasks, parent depth + 1 for FORWARD tasks.
     * Client-supplied values are ignored — the manager always derives it and
     * rejects submissions that would exceed the configured maximum depth.
     */
    private Integer chainDepth;

    /**
     * AES encryption IV, public key information
     */
    private String encryptIV;

    /**
     * TaskStatus
     */
    private String status;

    /**
     * Bucket affinity key. When set, the task is routed to a deterministic
     * bucket derived from this key instead of a random one, so tasks sharing
     * the same key are polled and executed in submission order.
     */
    private String hashKey;

    public String buildDefaultUniId(){
        String uniId = StringUtils.joinWith(",",pluginGroup,pluginHandle,context,executorRegion,fromType,fromSourceId);
        return DigestUtils.md5Hex(uniId);
    }
}
