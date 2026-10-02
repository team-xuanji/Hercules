package team.magic.flute.hercules.twelve.labors.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * CSV Export Format Parameters
 *
 * <p>Configuration parameters for CSV data export operations within the Hercules business
 * access gateway. Supports comprehensive CSV formatting options based on DuckDB specifications.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class CsvExportFormatParams implements ExportFormatSupport{

    private String exportFormatType = "csv";

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
    private Boolean overwriteOrIgnore=true;

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

    // ========== CSV-specific parameters ==========

    /**
     * Compression type
     * Optional values: auto, none, gzip, zstd, uncompressed
     * Default value: auto
     */
    private String compression;

    /**
     * Date format
     * Example: yyyy-MM-dd
     */
    private String dateFormat;

    /**
     * Delimiter
     * Optional values: ,, ;, \t, |
     * Default value: ,
     */
    private String delim;

    /**
     * Escape character
     * Default value: "
     */
    private String escape;

    /**
     * List of columns to force quote
     * Format: ['col1', 'col2']
     * Default value: []
     */
    private List<String> forceQuote;

    /**
     * Whether to write header
     * Optional values: true, false
     * Default value: true
     */
    private Boolean header;

    /**
     * String representation of NULL values
     */
    private String nullStr;

    /**
     * File prefix (must be used with suffix, header must be false)
     */
    private String prefix;

    /**
     * File suffix (must be used with prefix, header must be false)
     */
    private String suffix;

    /**
     * Quote character
     * Default value: "
     */
    private String quote;

    /**
     * Timestamp format
     * Example: yyyy-MM-dd HH:mm:ss
     */
    private String timestampFormat;
}
