package team.magic.flute.hercules.twelve.labors.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * CMS data download task query VO
 * Used for task queries with permission control
 *
 * @author Hercules
 */
@Data
@Accessors(chain = true)
public class CmsDataDownloadTaskQueryVO {
    
    /**
     * Current page number, default 1
     */
    private Long current = 1L;

    /**
     * Page size, default 10
     */
    private Long size = 10L;

    /**
     * Business scenario key (exact query)
     */
    private String businessKey;

    /**
     * Task status (exact query)
     */
    private String taskStatus;

    /**
     * Task ID (exact query)
     */
    private Long taskId;

    /**
     * Application key (obtained from UserAccessInfo, used for permission control)
     */
    private String appKey;

    /**
     * User ID (obtained from UserAccessInfo, used for permission control)
     */
    private String userId;
}
