package team.magic.flute.hercules.twelve.labors.vo;

import team.magic.flute.hercules.common.status.TaskStatus;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Task Submission and Download Result VO
 * Used to return complete results for integrated task submission and download link retrieval
 *
 * @author Hercules
 */
@Data
@Accessors(chain = true)
public class TaskSubmissionAndDownloadResultVO {

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
     * File download URL
     */
    private String downloadUrl;

    /**
     * Download URL expiration time description
     */
    private String expirationTime;

    /**
     * Download URL generation time
     */
    private LocalDateTime generateTime;

    /**
     * Whether authentication is required
     */
    private Boolean authRequired;

    /**
     * File path
     */
    private String filePath;

    /**
     * Operation source
     */
    private String operatorSource;

    /**
     * Task execution time (seconds)
     */
    private Long executionTimeSeconds;

    /**
     * Polling count
     */
    private Integer pollCount;

    /**
     * Task creation time
     */
    private LocalDateTime taskCreateTime;

    /**
     * Task completion time
     */
    private LocalDateTime taskCompleteTime;

    /**
     * Additional information
     */
    private String remarks;
}
