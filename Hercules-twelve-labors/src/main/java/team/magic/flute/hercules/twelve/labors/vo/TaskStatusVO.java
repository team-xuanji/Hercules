package team.magic.flute.hercules.twelve.labors.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.status.TaskStatus;

import java.time.LocalDateTime;

/**
 * Task status query result VO
 * Used to return task status query result information
 *
 * @author Hercules
 */
@Data
@Accessors(chain = true)
public class TaskStatusVO {

    /**
     * Task ID
     */
    private Long taskId;

    /**
     * Business scenario key
     */
    private String businessKey;

    /**
     * Task status
     */
    private TaskStatus taskStatus;

    /**
     * File path
     */
    private String filePath;

    /**
     * Task creation time
     */
    private LocalDateTime insertTime;

    /**
     * Task update time
     */
    private LocalDateTime updateTime;

    /**
     * Whether completed
     */
    private Boolean isCompleted;

    /**
     * Operation source
     */
    private String operatorSource;

    /**
     * Application key
     */
    private String appKey;

    /**
     * User ID
     */
    private String userId;
    /**
     * User name
     */
    private String userName;

    /**
     * Task execution progress description
     */
    private String progressDescription;

    /**
     * Error message (if task failed)
     */
    private String errorMessage;

}
