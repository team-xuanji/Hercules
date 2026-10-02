package team.magic.flute.hercules.twelve.labors.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * Submit Task Request Value Object
 *
 * <p>Value object for task submission requests within the Hercules business
 * access gateway. Provides validation and data transfer for task creation operations.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class SubmitTaskRequestVO {
    
    /**
     * Business scenario key
     */
    @NotBlank(message = "Business scenario key cannot be empty")
    private String businessKey;

    /**
     * JSON parameter list, used to replace parameters in SQL template
     * Format: {"param1": "value1", "param2": "value2"}
     */
    @NotNull(message = "Parameter list cannot be empty")
    private Map<String, String> params;

    /**
     * Operation source
     * Passed in from external, used to identify the creation source of the task
     */
    private String operatorSource;

    /**
     * Application key
     * Used for permission control in backend service calls, optional
     */
    private String appKey;

    /**
     * User ID
     * Used for permission control in backend service calls, optional
     */
    private String userId;

    /**
     * User name
     * Used for display purposes
     */
    private String userName;
}
