package team.magic.flute.hercules.twelve.labors.dto;

/**
 * Export Format Support Interface
 *
 * <p>Marker interface for export format parameter classes within the Hercules business
 * access gateway. Provides type identification for different export format configurations.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public interface ExportFormatSupport {
    /**
     * Get the export format type identifier
     *
     * @return format type string (e.g., "csv", "json", "xlsx", "parquet")
     */
    public String getExportFormatType();
}
