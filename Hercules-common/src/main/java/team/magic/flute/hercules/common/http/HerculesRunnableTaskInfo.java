package team.magic.flute.hercules.common.http;

import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.status.TaskType;

@Data
@Accessors(chain = true)
public class HerculesRunnableTaskInfo {
    /**
     * It can be left blank and will be automatically generated.
     * If filled in, ensure that tasks with the same ID do not conflict.
     */
    private String id;
    /**
     * Business Grouping of Executors
     */
    private String executorRegion;
    /**
     * Description
     */
    private String desc;
    /**
     * plugin-key
     */
    private String pluginHandle;
    /**
     * plugin-business-group
     */
    private String pluginGroup;
    /**
     * The running context structure refers to the request in hercules-manager.
     */
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
     */
    private String fromSourceId;

    /**
     * AES encryption IV, public key information
     */
    private String encryptIV;

    /**
     * TaskStatus
     */
    private String status;
}
