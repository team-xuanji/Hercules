package team.magic.flute.hercules.plugin.data.export.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DuckDB COPY EXPORT Statement Parameter Formatting Utility Class for formatting export parameters
 * of different file formats.
 *
 * @author Hercules
 */
public class ExportParamFormatUtil {

    /** Parameter collector, used for collecting and deduplicating parameters. */
    private static class ParameterCollector {
        private final LinkedHashMap<String, String> parameters = new LinkedHashMap<>();

        /**
         * Add parameters, overwriting if they already exist (the last setting takes precedence).
         */
        public void addParam(String paramName, String value) {
            if (value != null && !value.trim().isEmpty()) {
                parameters.put(paramName.toUpperCase(), formatParamValue(value));
            }
        }

        /** Build the final result */
        public String buildResult() {
            if (parameters.isEmpty()) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : parameters.entrySet()) {
                if (!first) {
                    sb.append(",\n");
                }
                sb.append("    ").append(entry.getKey()).append(" ").append(entry.getValue());
                first = false;
            }
            return sb.toString();
        }

        /**
         * Format parameter values with special handling for booleans, numbers, etc., and enclose
         * string values in quotes.
         */
        private static String formatParamValue(String value) {
            if (value == null || value.trim().isEmpty()) {
                return "''";
            }

            String trimmedValue = value.trim();

            // bool
            if ("true".equalsIgnoreCase(trimmedValue)
                    || "false".equalsIgnoreCase(trimmedValue)
                    || "on".equalsIgnoreCase(trimmedValue)
                    || "off".equalsIgnoreCase(trimmedValue)
                    || "1".equals(trimmedValue)
                    || "0".equals(trimmedValue)) {
                return trimmedValue.toLowerCase();
            }

            // number
            if (isNumeric(trimmedValue)) {
                return trimmedValue;
            }

            // array format (如 FORCE_QUOTE)
            if (trimmedValue.startsWith("[") && trimmedValue.endsWith("]")) {
                return trimmedValue;
            }

            // struct format (如 FIELD_IDS)
            if (trimmedValue.startsWith("{") && trimmedValue.endsWith("}")) {
                return trimmedValue;
            }

            // special keywords
            if ("auto".equalsIgnoreCase(trimmedValue) || "none".equalsIgnoreCase(trimmedValue)) {
                return trimmedValue.toLowerCase();
            }

            // By default, string values are enclosed in single quotes.
            return "'" + trimmedValue.replace("'", "''") + "'";
        }

        /** Check if the string is a number. */
        private static boolean isNumeric(String str) {
            if (str == null || str.isEmpty()) {
                return false;
            }
            try {
                Double.parseDouble(str);
                return true;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    /**
     * Format export parameters
     *
     * @param exportFormat export-format (csv, json, parquet, excel, xlsx)
     * @param exportParam Export parameter configuration based on the parameters supported by the
     *     official DuckDB documentation.
     * @return Formatted parameter string for the DuckDB COPY statement
     */
    public static String formatExportParam(String exportFormat, Map<String, String> exportParam) {
        if (exportParam == null || exportParam.isEmpty()) {
            return "";
        }
        exportParam.putIfAbsent("OVERWRITE_OR_IGNORE", "true");

        if (exportFormat == null) {
            throw new IllegalArgumentException("Export format cannot be null");
        }

        // Using a parameter collector to maintain parameter order and support deduplication.
        ParameterCollector collector = new ParameterCollector();

        // 1. First, add the general parameters (common parameters for all formats)
        formatGenericParams(exportParam, collector);

        // 2. Then, add specific parameters according to the specific format (this will overwrite
        // duplicate parameters).
        switch (exportFormat.toLowerCase()) {
            case "csv":
                formatCsvParams(exportParam, collector);
                break;
            case "json":
                formatJsonParams(exportParam, collector);
                break;
            case "parquet":
                formatParquetParams(exportParam, collector);
                break;
            case "xlsx":
                formatExcelParams(exportParam, collector);
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported export format: "
                                + exportFormat
                                + ". Supported formats are: csv, json, parquet, excel, xlsx");
        }

        return collector.buildResult();
    }

    /**
     * Formatting General Parameters (Applicable to All Formats) Based on DuckDB Official
     * Documentation's General COPY Parameters
     */
    private static void formatGenericParams(
            Map<String, String> exportParam, ParameterCollector collector) {
        // General Parameters (Based on DuckDB Official Documentation)
        addParam(exportParam, collector, "FORMAT"); // format type，default-val: auto
        addParam(
                exportParam,
                collector,
                "USE_TMP_FILE"); // Write to temporary file?，default-val: auto
        addParam(
                exportParam,
                collector,
                "OVERWRITE_OR_IGNORE"); // Allow to overwrite the file? default-val: false
        addParam(
                exportParam,
                collector,
                "OVERWRITE"); // Remove existing files from the target directory.default-val: false
        addParam(
                exportParam,
                collector,
                "APPEND"); // Regenerate file name patterns to prevent overwriting. default-val:
        // false
        addParam(
                exportParam,
                collector,
                "FILENAME_PATTERN"); // File name pattern, which may include {uuid} or
        // {i},default-val: auto
        addParam(
                exportParam,
                collector,
                "FILE_EXTENSION"); // File extension for generated files.default-val: auto
        addParam(
                exportParam,
                collector,
                "PER_THREAD_OUTPUT"); // Each thread generates one file.default-val: false
        addParam(
                exportParam,
                collector,
                "FILE_SIZE_BYTES"); // File size limit (in bytes or human-readable
        // format).default-val: empty
        addParam(
                exportParam,
                collector,
                "PARTITION_BY"); // Using partition columns with the Hive partitioning
        // scheme,default-val: empty
        addParam(
                exportParam,
                collector,
                "RETURN_FILES"); // Whether to include the created file path in the query
        // results,default-val: false
        addParam(
                exportParam,
                collector,
                "WRITE_PARTITION_COLUMNS"); // Should the partition column be written to the
        // file,default-val: false
    }

    /** Format CSV parameters based on DuckDB official documentation's CSV options. */
    private static void formatCsvParams(
            Map<String, String> exportParam, ParameterCollector collector) {
        // Supported Parameters for CSV (Based on DuckDB Official Documentation)
        addParam(exportParam, collector, "COMPRESSION");
        addParam(exportParam, collector, "DATEFORMAT");
        addParam(exportParam, collector, "DELIM", "SEP");
        addParam(exportParam, collector, "ESCAPE");
        addParam(exportParam, collector, "FORCE_QUOTE");
        addParam(exportParam, collector, "HEADER");
        addParam(exportParam, collector, "NULLSTR");
        addParam(exportParam, collector, "PREFIX");
        addParam(exportParam, collector, "SUFFIX");
        addParam(exportParam, collector, "QUOTE");
        addParam(exportParam, collector, "TIMESTAMPFORMAT");
    }

    /** Format JSON parameters based on DuckDB official documentation's JSON options. */
    private static void formatJsonParams(
            Map<String, String> exportParam, ParameterCollector collector) {
        // Parameters Supported by JSON (Based on DuckDB Official Documentation)
        addParam(exportParam, collector, "ARRAY");
        addParam(exportParam, collector, "COMPRESSION");
        addParam(exportParam, collector, "DATEFORMAT");
        addParam(exportParam, collector, "TIMESTAMPFORMAT");
    }

    /**
     * Formatting Excel XLSX Format Parameters Based on DuckDB Official Documentation's XLSX Options
     */
    private static void formatExcelParams(
            Map<String, String> exportParam, ParameterCollector collector) {
        // Parameters Supported by XLSX (Based on DuckDB Official Documentation)
        addParam(exportParam, collector, "header"); // first line is header,default-val: false
        addParam(exportParam, collector, "sheet"); // sheet name,default-val: "Sheet1"
        addParam(exportParam, collector, "sheet_row_limit"); // max row number,default-val: 1048576
    }

    /**
     * Formatting Parquet Format Parameters Based on DuckDB Official Documentation's Parquet Options
     */
    private static void formatParquetParams(
            Map<String, String> exportParam, ParameterCollector collector) {
        // Parameters Supported by Parquet (Based on DuckDB Official Documentation)
        addParam(
                exportParam,
                collector,
                "COMPRESSION"); // Compression format: uncompressed, snappy, gzip, zstd, brotli,
        // lz4, lz4_raw
        addParam(
                exportParam,
                collector,
                "COMPRESSION_LEVEL"); // Compression level 1-22, only supports zstd.
        addParam(exportParam, collector, "FIELD_IDS"); // Field ID Mapping
        addParam(exportParam, collector, "ROW_GROUP_SIZE_BYTES"); // Target byte size per row
        addParam(
                exportParam,
                collector,
                "ROW_GROUP_SIZE"); // Target number of rows in a group,default-val: 122880
        addParam(exportParam, collector, "ROW_GROUPS_PER_FILE"); // Number of line groups per file
        addParam(exportParam, collector, "PARQUET_VERSION"); // Parquet version: V1, V2
    }

    /**
     * Add parameters to the collector to support multiple possible parameter names (aliases).
     * Parameter names will be converted to uppercase to enable case-insensitive lookup. If multiple
     * aliases match, use the first value found.
     */
    private static void addParam(
            Map<String, String> exportParam, ParameterCollector collector, String... paramNames) {
        for (String paramName : paramNames) {
            // First attempt an exact match.
            String value = exportParam.get(paramName);
            if (value != null && !value.trim().isEmpty()) {
                collector.addParam(paramName, value);
                return;
            }

            // If an exact match fails, attempt a case-insensitive match.
            for (Map.Entry<String, String> entry : exportParam.entrySet()) {
                if (paramName.equalsIgnoreCase(entry.getKey())
                        && entry.getValue() != null
                        && !entry.getValue().trim().isEmpty()) {
                    collector.addParam(paramName, entry.getValue());
                    return;
                }
            }
        }
    }
}
