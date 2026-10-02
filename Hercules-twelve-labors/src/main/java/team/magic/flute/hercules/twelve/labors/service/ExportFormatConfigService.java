package team.magic.flute.hercules.twelve.labors.service;

import team.magic.flute.hercules.twelve.labors.constant.CmsDownloadFormat;
import team.magic.flute.hercules.twelve.labors.constant.CmsDownloadFormatCompressionCodec;
import team.magic.flute.hercules.twelve.labors.vo.DynamicEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Export Format Configuration Service
 *
 * <p>This service provides dynamic form configuration capabilities for different export formats
 * supported by the Hercules data export system. It generates configuration fields and validation
 * rules for each export format, enabling users to customize export parameters through a
 * user-friendly interface.
 *
 * <p>Supported export formats and their configurations:
 * <ul>
 *   <li><strong>CSV</strong>: Delimiter, header options, quote character, escape character</li>
 *   <li><strong>JSON</strong>: Array format, pretty printing, date format</li>
 *   <li><strong>Parquet</strong>: Compression codec, row group size, page size</li>
 *   <li><strong>XLSX</strong>: Sheet name, header styling, cell formatting</li>
 * </ul>
 *
 * <p>All formats support common configuration options:
 * <ul>
 *   <li>Compression settings (GZIP, ZSTD, None)</li>
 *   <li>File encoding options</li>
 *   <li>Output formatting preferences</li>
 * </ul>
 *
 * <p>The service generates {@link DynamicEntity} objects that describe form fields,
 * including field types, validation rules, default values, and available options.
 * This enables the frontend to dynamically render appropriate configuration forms
 * for each export format.
 *
 * <p>Usage example:
 * <pre>{@code
 * ExportFormatConfigService configService = new ExportFormatConfigService();
 * List<DynamicEntity> csvFields = configService.getFormatConfigFields(CmsDownloadFormat.CSV);
 * Map<CmsDownloadFormat, List<DynamicEntity>> allConfigs = configService.getAllFormatConfigs();
 * }</pre>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class ExportFormatConfigService {

    /**
     * Get configuration fields for a specific export format.
     *
     * <p>This method returns a list of dynamic form fields that are applicable to the
     * specified export format. The returned fields include both generic configuration
     * options (available for all formats) and format-specific options.
     *
     * <p>The configuration fields are represented as {@link DynamicEntity} objects
     * that contain metadata about each field including:
     * <ul>
     *   <li>Field name and display label</li>
     *   <li>Field type (text, select, checkbox, etc.)</li>
     *   <li>Default values and validation rules</li>
     *   <li>Available options for select fields</li>
     *   <li>Help text and descriptions</li>
     * </ul>
     *
     * @param format the export format for which to retrieve configuration fields
     * @return a list of dynamic form field configurations
     * @throws IllegalArgumentException if the format is null or unsupported
     */
    public List<DynamicEntity> getFormatConfigFields(CmsDownloadFormat format) {
        List<DynamicEntity> fields = new ArrayList<>();

        // Add generic parameters
        fields.addAll(getGenericConfigFields());

        // Add format-specific parameters based on format
        switch (format) {
            case CSV:
                fields.addAll(getCsvConfigFields());
                break;
            case JSON:
                fields.addAll(getJsonConfigFields());
                break;
            case PARQUET:
                fields.addAll(getParquetConfigFields());
                break;
            case XLSX:
                fields.addAll(getXlsxConfigFields());
                break;
        }

        return fields;
    }

    /**
     * Get configuration fields for all formats.
     *
     * @return mapping from format names to configuration field lists
     */
    public Map<String, List<DynamicEntity>> getAllFormatConfigFields() {
        Map<String, List<DynamicEntity>> allConfigs = new LinkedHashMap<>();

        for (CmsDownloadFormat format : CmsDownloadFormat.values()) {
            allConfigs.put(format.name().toLowerCase(), getFormatConfigFields(format));
        }

        return allConfigs;
    }

    /**
     * Get generic configuration fields (applicable to all formats).
     */
    private List<DynamicEntity> getGenericConfigFields() {
        List<DynamicEntity> fields = new ArrayList<>();

        fields.add(new DynamicEntity()
                .setKey("USE_TMP_FILE")
                .setDesc("Whether to write to temporary file")
                .setRequire(false)
                .setDemoValue("auto")
                .setValueEnum(Arrays.asList("auto", "true", "false")));

        fields.add(new DynamicEntity()
                .setKey("OVERWRITE_OR_IGNORE")
                .setDesc("Whether to allow file overwrite")
                .setRequire(false)
                .setDemoValue("false")
                .setValueEnum(Arrays.asList("true", "false")));

        fields.add(new DynamicEntity()
                .setKey("OVERWRITE")
                .setDesc("Remove existing files in target directory")
                .setRequire(false)
                .setDemoValue("false")
                .setValueEnum(Arrays.asList("true", "false")));

        fields.add(new DynamicEntity()
                .setKey("APPEND")
                .setDesc("Regenerate filename pattern to ensure no overwrite")
                .setRequire(false)
                .setDemoValue("false")
                .setValueEnum(Arrays.asList("true", "false")));

        fields.add(new DynamicEntity()
                .setKey("FILENAME_PATTERN")
                .setDesc("Filename pattern, can include {uuid} or {i}")
                .setRequire(false)
                .setDemoValue("auto")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("FILE_EXTENSION")
                .setDesc("File extension for generated files")
                .setRequire(false)
                .setDemoValue("auto")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("PER_THREAD_OUTPUT")
                .setDesc("Generate one file per thread")
                .setRequire(false)
                .setDemoValue("false")
                .setValueEnum(Arrays.asList("true", "false")));

        fields.add(new DynamicEntity()
                .setKey("FILE_SIZE_BYTES")
                .setDesc("File size limit (bytes or human-readable format, e.g., 1GB)")
                .setRequire(false)
                .setDemoValue("")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("PARTITION_BY")
                .setDesc("Partition columns using Hive partitioning scheme")
                .setRequire(false)
                .setDemoValue("")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("RETURN_FILES")
                .setDesc("Whether to include created file paths in query results")
                .setRequire(false)
                .setDemoValue("false")
                .setValueEnum(Arrays.asList("true", "false")));

        fields.add(new DynamicEntity()
                .setKey("WRITE_PARTITION_COLUMNS")
                .setDesc("Whether to write partition columns to files")
                .setRequire(false)
                .setDemoValue("false")
                .setValueEnum(Arrays.asList("true", "false")));

        return fields;
    }

    /**
     * Get CSV format-specific configuration fields.
     */
    private List<DynamicEntity> getCsvConfigFields() {
        List<DynamicEntity> fields = new ArrayList<>();

        // Get CSV supported compression formats
        Collection<CmsDownloadFormatCompressionCodec> csvCompressions =
                CmsDownloadFormatCompressionCodec.getSupportCompressCodec(CmsDownloadFormat.CSV);
        List<String> csvCompressionNames = csvCompressions.stream()
                .map(codec -> codec.name().toLowerCase())
                .collect(Collectors.toList());

        fields.add(new DynamicEntity()
                .setKey("COMPRESSION")
                .setDesc("Compression type")
                .setRequire(false)
                .setDemoValue("auto")
                .setValueEnum(csvCompressionNames));

        fields.add(new DynamicEntity()
                .setKey("DATEFORMAT")
                .setDesc("Date format")
                .setRequire(false)
                .setDemoValue("yyyy-MM-dd")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("DELIM")
                .setDesc("Delimiter")
                .setRequire(false)
                .setDemoValue(",")
                .setValueEnum(Arrays.asList(",", ";", "\t", "|")));

        fields.add(new DynamicEntity()
                .setKey("ESCAPE")
                .setDesc("Escape character")
                .setRequire(false)
                .setDemoValue("\"")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("FORCE_QUOTE")
                .setDesc("List of columns to force quote, format: ['col1', 'col2']")
                .setRequire(false)
                .setDemoValue("[]")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("HEADER")
                .setDesc("Whether to write header")
                .setRequire(false)
                .setDemoValue("true")
                .setValueEnum(Arrays.asList("true", "false")));

        fields.add(new DynamicEntity()
                .setKey("NULLSTR")
                .setDesc("String representation of NULL values")
                .setRequire(false)
                .setDemoValue("")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("PREFIX")
                .setDesc("File prefix (must be used with SUFFIX, HEADER must be false)")
                .setRequire(false)
                .setDemoValue("")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("SUFFIX")
                .setDesc("File suffix (must be used with PREFIX, HEADER must be false)")
                .setRequire(false)
                .setDemoValue("")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("QUOTE")
                .setDesc("Quote character")
                .setRequire(false)
                .setDemoValue("\"")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("TIMESTAMPFORMAT")
                .setDesc("Timestamp format")
                .setRequire(false)
                .setDemoValue("yyyy-MM-dd HH:mm:ss")
                .setValueEnum(null));

        return fields;
    }

    /**
     * Get Parquet format-specific configuration fields.
     */
    private List<DynamicEntity> getParquetConfigFields() {
        List<DynamicEntity> fields = new ArrayList<>();

        // Get Parquet supported compression formats
        Collection<CmsDownloadFormatCompressionCodec> parquetCompressions =
                CmsDownloadFormatCompressionCodec.getSupportCompressCodec(CmsDownloadFormat.PARQUET);
        List<String> parquetCompressionNames = parquetCompressions.stream()
                .map(codec -> codec.name().toLowerCase())
                .collect(Collectors.toList());

        fields.add(new DynamicEntity()
                .setKey("COMPRESSION")
                .setDesc("Compression format")
                .setRequire(false)
                .setDemoValue("snappy")
                .setValueEnum(parquetCompressionNames));

        fields.add(new DynamicEntity()
                .setKey("COMPRESSION_LEVEL")
                .setDesc("Compression level 1-22, only supports zstd")
                .setRequire(false)
                .setDemoValue("3")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("FIELD_IDS")
                .setDesc("Field ID mapping, format: {field_name: id}")
                .setRequire(false)
                .setDemoValue("{}")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("ROW_GROUP_SIZE_BYTES")
                .setDesc("Row group target byte size")
                .setRequire(false)
                .setDemoValue("134217728")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("ROW_GROUP_SIZE")
                .setDesc("Row group target row count")
                .setRequire(false)
                .setDemoValue("122880")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("ROW_GROUPS_PER_FILE")
                .setDesc("Number of row groups per file")
                .setRequire(false)
                .setDemoValue("1")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("PARQUET_VERSION")
                .setDesc("Parquet version")
                .setRequire(false)
                .setDemoValue("V2")
                .setValueEnum(Arrays.asList("V1", "V2")));

        return fields;
    }

    /**
     * Get XLSX format-specific configuration fields.
     */
    private List<DynamicEntity> getXlsxConfigFields() {
        List<DynamicEntity> fields = new ArrayList<>();

        fields.add(new DynamicEntity()
                .setKey("header")
                .setDesc("Whether to write column names as first row")
                .setRequire(false)
                .setDemoValue("false")
                .setValueEnum(Arrays.asList("true", "false")));

        fields.add(new DynamicEntity()
                .setKey("sheet")
                .setDesc("Worksheet name")
                .setRequire(false)
                .setDemoValue("Sheet1")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("sheet_row_limit")
                .setDesc("Maximum rows per worksheet")
                .setRequire(false)
                .setDemoValue("1048576")
                .setValueEnum(null));

        return fields;
    }

    /**
     * Get JSON format-specific configuration fields.
     */
    private List<DynamicEntity> getJsonConfigFields() {
        List<DynamicEntity> fields = new ArrayList<>();

        // Get JSON supported compression formats
        Collection<CmsDownloadFormatCompressionCodec> jsonCompressions =
                CmsDownloadFormatCompressionCodec.getSupportCompressCodec(CmsDownloadFormat.JSON);
        List<String> jsonCompressionNames = jsonCompressions.stream()
                .map(codec -> codec.name().toLowerCase())
                .collect(Collectors.toList());

        fields.add(new DynamicEntity()
                .setKey("COMPRESSION")
                .setDesc("Compression type")
                .setRequire(false)
                .setDemoValue("auto")
                .setValueEnum(jsonCompressionNames));

        fields.add(new DynamicEntity()
                .setKey("ARRAY")
                .setDesc("Whether to write JSON array")
                .setRequire(false)
                .setDemoValue("false")
                .setValueEnum(Arrays.asList("true", "false")));

        fields.add(new DynamicEntity()
                .setKey("DATEFORMAT")
                .setDesc("Date format")
                .setRequire(false)
                .setDemoValue("yyyy-MM-dd")
                .setValueEnum(null));

        fields.add(new DynamicEntity()
                .setKey("TIMESTAMPFORMAT")
                .setDesc("Timestamp format")
                .setRequire(false)
                .setDemoValue("yyyy-MM-dd HH:mm:ss")
                .setValueEnum(null));

        return fields;
    }
}
