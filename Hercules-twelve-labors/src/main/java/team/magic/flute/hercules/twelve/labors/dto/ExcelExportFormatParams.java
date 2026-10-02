package team.magic.flute.hercules.twelve.labors.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Excel/XLSX Export Format Parameters
 *
 * <p>Configuration parameters for Excel/XLSX data export operations within the Hercules
 * business access gateway. Based on DuckDB XLSX export specifications.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class ExcelExportFormatParams implements ExportFormatSupport{

    private String exportFormatType = "xlsx";
    // ========== Common parameters ==========

    /**
     * Whether to write to temporary file
     * Optional values: auto, true, false
     * Default value: auto
     */
    private String useTmpFile;

    /**
     * Whether to allow file overwrite
     * Optional values: true, false
     * Default value: false
     */
    private Boolean overwriteOrIgnore;

    /**
     * Remove existing files in target directory
     * Optional values: true, false
     * Default value: false
     */
    private Boolean overwrite;

    /**
     * Regenerate filename pattern to ensure no overwrite
     * Optional values: true, false
     * Default value: false
     */
    private Boolean append;

    /**
     * Filename pattern, can include {uuid} or {i}
     * Default value: auto
     */
    private String filenamePattern;

    /**
     * File extension for generated files
     * Default value: auto
     */
    private String fileExtension;

    /**
     * Generate one file per thread
     * Optional values: true, false
     * Default value: false
     */
    private Boolean perThreadOutput;

    /**
     * File size limit (bytes or human-readable format, e.g., 1GB)
     */
    private String fileSizeBytes;

    /**
     * Partition columns using Hive partitioning scheme
     */
    private String partitionBy;

    /**
     * Whether to include created file paths in query results
     * Optional values: true, false
     * Default value: false
     */
    private Boolean returnFiles;

    /**
     * Whether to write partition columns to files
     * Optional values: true, false
     * Default value: false
     */
    private Boolean writePartitionColumns;

    // ========== Excel/XLSX-specific parameters ==========

    /**
     * Whether to write column names as first row
     * Optional values: true, false
     * Default value: false
     */
    private Boolean header=true;

    /**
     * Worksheet name
     * Default value: Sheet1
     */
    private String sheet;

    /**
     * Maximum rows per worksheet
     * Default value: 1048576
     */
    private Integer sheetRowLimit;
}
