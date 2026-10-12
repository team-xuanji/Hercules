package team.magic.flute.hercules.twelve.labors.vo;

import lombok.Data;
import team.magic.flute.hercules.twelve.labors.constant.CmsDownloadFormat;

/**
 * Export Support Value Object
 *
 * <p>Value object for export format support information within the Hercules business access
 * gateway. Provides format capabilities and configuration options.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
public class ExportSupportVO {
    private CmsDownloadFormat exportFormat;
}
