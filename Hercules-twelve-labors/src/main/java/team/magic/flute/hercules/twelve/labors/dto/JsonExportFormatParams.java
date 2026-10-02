package team.magic.flute.hercules.twelve.labors.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * JSON Export Format Parameters
 *
 * <p>Configuration parameters for JSON data export operations within the Hercules
 * business access gateway. Based on DuckDB JSON export specifications.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class JsonExportFormatParams implements ExportFormatSupport{

    private String exportFormatType = "json";
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

    // ========== JSON-specific parameters ==========

    /**
     * Compression type
     * Optional values: auto, none, gzip, zstd, uncompressed
     * Default value: auto
     */
    private String compression;

    /**
     * Whether to write JSON array
     * Optional values: true, false
     * Default value: false
     */
    private Boolean array;

    /**
     * Date format
     * Example: yyyy-MM-dd
     */
    private String dateFormat;

    /**
     * Timestamp format
     * Example: yyyy-MM-dd HH:mm:ss
     */
    private String timestampFormat;
}
