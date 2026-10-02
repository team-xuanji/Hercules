package team.magic.flute.hercules.twelve.labors.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * Dynamic Entity for Configuration Fields
 *
 * <p>This class represents dynamic configuration fields used in export format configurations.
 * It provides a flexible way to define configuration parameters with validation and documentation.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class DynamicEntity {
    /**
     * Configuration key
     */
    private String key;
    /**
     * Configuration description
     */
    private String desc;
    /**
     * Whether this field is required
     */
    private boolean require;
    /**
     * Example value
     */
    private String demoValue;
    /**
     * Available enumeration values
     */
    private List<String> valueEnum;
}
