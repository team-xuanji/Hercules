package team.magic.flute.hercules.twelve.labors.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Map;

/**
 * Parquet Export Format Parameters
 *
 * <p>Configuration parameters for Parquet data export operations within the Hercules
 * business access gateway. Based on DuckDB Parquet export specifications.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class ParquetExportFormatParams implements ExportFormatSupport{

    private String exportFormatType = "parquet";
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

    // ========== Parquet-specific parameters ==========

    /**
     * Compression format
     * Optional values: uncompressed, snappy, gzip, zstd, brotli, lz4, lz4_raw
     * Default value: snappy
     */
    private String compression;

    /**
     * Compression level 1-22, only supported for zstd
     * Default value: 3
     */
    private Integer compressionLevel;

    /**
     * Field ID mapping
     * Format: {field_name: id}
     * Default value: {}
     */
    private Map<String, Integer> fieldIds;

    /**
     * Row group target byte size
     * Default value: 134217728 (128MB)
     */
    private Long rowGroupSizeBytes;

    /**
     * Row group target row count
     * Default value: 122880
     */
    private Integer rowGroupSize;

    /**
     * Number of row groups per file
     * Default value: 1
     */
    private Integer rowGroupsPerFile;

    /**
     * Parquet version
     * Optional values: V1, V2
     * Default value: V2
     */
    private String parquetVersion;
}
