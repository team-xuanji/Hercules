package team.magic.flute.hercules.twelve.labors.util;

import team.magic.flute.hercules.twelve.labors.dto.CsvExportFormatParams;
import team.magic.flute.hercules.twelve.labors.dto.ExcelExportFormatParams;
import team.magic.flute.hercules.twelve.labors.dto.JsonExportFormatParams;
import team.magic.flute.hercules.twelve.labors.dto.ParquetExportFormatParams;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Export Format Parameters Converter Utility Class
 *
 * <p>Utility class for converting export format parameter objects to/from Map representations
 * within the Hercules business access gateway. Supports bidirectional conversion for all
 * supported export formats including CSV, JSON, Parquet, and Excel.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class ExportFormatParamsConverter {

    /**
     * Convert CSV parameter object to Map
     */
    public static Map<String, String> csvParamsToMap(CsvExportFormatParams params) {
        if (params == null) {
            return new HashMap<>();
        }
        
        Map<String, String> map = new HashMap<>();
        
        // Common parameters
        addCommonParams(map, params);

        // CSV-specific parameters
        addIfNotNull(map, "COMPRESSION", params.getCompression());
        addIfNotNull(map, "DATEFORMAT", params.getDateFormat());
        addIfNotNull(map, "DELIM", params.getDelim());
        addIfNotNull(map, "ESCAPE", params.getEscape());
        addIfNotNull(map, "HEADER", params.getHeader());
        addIfNotNull(map, "NULLSTR", params.getNullStr());
        addIfNotNull(map, "PREFIX", params.getPrefix());
        addIfNotNull(map, "SUFFIX", params.getSuffix());
        addIfNotNull(map, "QUOTE", params.getQuote());
        addIfNotNull(map, "TIMESTAMPFORMAT", params.getTimestampFormat());
        
        // Handle forceQuote list
        if (params.getForceQuote() != null && !params.getForceQuote().isEmpty()) {
            String forceQuoteStr = "[" + params.getForceQuote().stream()
                    .map(col -> "'" + col + "'")
                    .collect(Collectors.joining(", ")) + "]";
            map.put("FORCE_QUOTE", forceQuoteStr);
        }
        
        return map;
    }

    /**
     * Convert JSON parameter object to Map
     */
    public static Map<String, String> jsonParamsToMap(JsonExportFormatParams params) {
        if (params == null) {
            return new HashMap<>();
        }
        
        Map<String, String> map = new HashMap<>();
        
        // Common parameters
        addCommonParams(map, params);

        // JSON-specific parameters
        addIfNotNull(map, "COMPRESSION", params.getCompression());
        addIfNotNull(map, "ARRAY", params.getArray());
        addIfNotNull(map, "DATEFORMAT", params.getDateFormat());
        addIfNotNull(map, "TIMESTAMPFORMAT", params.getTimestampFormat());
        
        return map;
    }

    /**
     * Convert Parquet parameter object to Map
     */
    public static Map<String, String> parquetParamsToMap(ParquetExportFormatParams params) {
        if (params == null) {
            return new HashMap<>();
        }
        
        Map<String, String> map = new HashMap<>();
        
        // Common parameters
        addCommonParams(map, params);

        // Parquet-specific parameters
        addIfNotNull(map, "COMPRESSION", params.getCompression());
        addIfNotNull(map, "COMPRESSION_LEVEL", params.getCompressionLevel());
        addIfNotNull(map, "ROW_GROUP_SIZE_BYTES", params.getRowGroupSizeBytes());
        addIfNotNull(map, "ROW_GROUP_SIZE", params.getRowGroupSize());
        addIfNotNull(map, "ROW_GROUPS_PER_FILE", params.getRowGroupsPerFile());
        addIfNotNull(map, "PARQUET_VERSION", params.getParquetVersion());
        
        // Handle fieldIds mapping
        if (params.getFieldIds() != null && !params.getFieldIds().isEmpty()) {
            StringBuilder fieldIdsStr = new StringBuilder("{");
            params.getFieldIds().entrySet().forEach(entry -> {
                if (fieldIdsStr.length() > 1) {
                    fieldIdsStr.append(", ");
                }
                fieldIdsStr.append("'").append(entry.getKey()).append("': ").append(entry.getValue());
            });
            fieldIdsStr.append("}");
            map.put("FIELD_IDS", fieldIdsStr.toString());
        }
        
        return map;
    }

    /**
     * Convert Excel parameter object to Map
     */
    public static Map<String, String> excelParamsToMap(ExcelExportFormatParams params) {
        if (params == null) {
            return new HashMap<>();
        }
        
        Map<String, String> map = new HashMap<>();
        
        // Common parameters
        addCommonParams(map, params);

        // Excel-specific parameters
        addIfNotNull(map, "header", params.getHeader());
        addIfNotNull(map, "sheet", params.getSheet());
        addIfNotNull(map, "sheet_row_limit", params.getSheetRowLimit());
        
        return map;
    }

    /**
     * Create CSV parameter object from Map
     */
    public static CsvExportFormatParams mapToCsvParams(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return new CsvExportFormatParams();
        }
        
        CsvExportFormatParams params = new CsvExportFormatParams();
        
        // Set common parameters
        setCommonParams(params, map);

        // Set CSV-specific parameters
        params.setCompression(map.get("COMPRESSION"))
              .setDateFormat(map.get("DATEFORMAT"))
              .setDelim(map.get("DELIM"))
              .setEscape(map.get("ESCAPE"))
              .setHeader(getBooleanValue(map, "HEADER"))
              .setNullStr(map.get("NULLSTR"))
              .setPrefix(map.get("PREFIX"))
              .setSuffix(map.get("SUFFIX"))
              .setQuote(map.get("QUOTE"))
              .setTimestampFormat(map.get("TIMESTAMPFORMAT"));
        
        // Handle forceQuote
        String forceQuoteStr = map.get("FORCE_QUOTE");
        if (StringUtils.hasText(forceQuoteStr)) {
            // Simple parsing of ['col1', 'col2'] format
            String cleaned = forceQuoteStr.replaceAll("[\\[\\]']", "");
            if (StringUtils.hasText(cleaned)) {
                List<String> forceQuoteList = Arrays.stream(cleaned.split(","))
                        .map(String::trim)
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList());
                params.setForceQuote(forceQuoteList);
            }
        }
        
        return params;
    }

    /**
     * Create JSON parameter object from Map
     */
    public static JsonExportFormatParams mapToJsonParams(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return new JsonExportFormatParams();
        }
        
        JsonExportFormatParams params = new JsonExportFormatParams();
        
        // Set common parameters
        setCommonParams(params, map);

        // Set JSON-specific parameters
        params.setCompression(map.get("COMPRESSION"))
              .setArray(getBooleanValue(map, "ARRAY"))
              .setDateFormat(map.get("DATEFORMAT"))
              .setTimestampFormat(map.get("TIMESTAMPFORMAT"));
        
        return params;
    }

    /**
     * Create Parquet parameter object from Map
     */
    public static ParquetExportFormatParams mapToParquetParams(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return new ParquetExportFormatParams();
        }
        
        ParquetExportFormatParams params = new ParquetExportFormatParams();
        
        // Set common parameters
        setCommonParams(params, map);

        // Set Parquet-specific parameters
        params.setCompression(map.get("COMPRESSION"))
              .setCompressionLevel(getIntegerValue(map, "COMPRESSION_LEVEL"))
              .setRowGroupSizeBytes(getLongValue(map, "ROW_GROUP_SIZE_BYTES"))
              .setRowGroupSize(getIntegerValue(map, "ROW_GROUP_SIZE"))
              .setRowGroupsPerFile(getIntegerValue(map, "ROW_GROUPS_PER_FILE"))
              .setParquetVersion(map.get("PARQUET_VERSION"));
        
        // Handle fieldIds - simplified processing, may need more complex JSON parsing in actual use
        String fieldIdsStr = map.get("FIELD_IDS");
        if (StringUtils.hasText(fieldIdsStr)) {
            // Simple parsing, recommend using JSON library in actual projects
            Map<String, Integer> fieldIds = new HashMap<>();
            // More complex parsing logic can be added here
            params.setFieldIds(fieldIds);
        }
        
        return params;
    }

    /**
     * Create Excel parameter object from Map
     */
    public static ExcelExportFormatParams mapToExcelParams(Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            return new ExcelExportFormatParams();
        }
        
        ExcelExportFormatParams params = new ExcelExportFormatParams();
        
        // Set common parameters
        setCommonParams(params, map);

        // Set Excel-specific parameters
        params.setHeader(getBooleanValue(map, "header"))
              .setSheet(map.get("sheet"))
              .setSheetRowLimit(getIntegerValue(map, "sheet_row_limit"));
        
        return params;
    }

    // ========== Private helper methods ==========

    /**
     * Add common parameters to Map
     */
    private static void addCommonParams(Map<String, String> map, Object params) {
        try {
            Class<?> clazz = params.getClass();
            while (clazz != null) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    Object value = field.get(params);
                    
                    // Convert field name to uppercase underscore format
                    String paramName = convertFieldNameToParamName(fieldName);
                    addIfNotNull(map, paramName, value);
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            // Ignore reflection exceptions
        }
    }

    /**
     * Set common parameters from Map
     */
    private static void setCommonParams(Object params, Map<String, String> map) {
        try {
            Class<?> clazz = params.getClass();
            while (clazz != null) {
                for (Field field : clazz.getDeclaredFields()) {
                    field.setAccessible(true);
                    String fieldName = field.getName();
                    String paramName = convertFieldNameToParamName(fieldName);
                    
                    String value = map.get(paramName);
                    if (value != null) {
                        setFieldValue(field, params, value);
                    }
                }
                clazz = clazz.getSuperclass();
            }
        } catch (Exception e) {
            // Ignore reflection exceptions
        }
    }

    private static void addIfNotNull(Map<String, String> map, String key, Object value) {
        if (value != null) {
            map.put(key, value.toString());
        }
    }

    private static Boolean getBooleanValue(Map<String, String> map, String key) {
        String value = map.get(key);
        return value != null ? Boolean.valueOf(value) : null;
    }

    private static Integer getIntegerValue(Map<String, String> map, String key) {
        String value = map.get(key);
        try {
            return value != null ? Integer.valueOf(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Long getLongValue(Map<String, String> map, String key) {
        String value = map.get(key);
        try {
            return value != null ? Long.valueOf(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String convertFieldNameToParamName(String fieldName) {
        // Convert camelCase naming to uppercase underscore format
        return fieldName.replaceAll("([a-z])([A-Z])", "$1_$2").toUpperCase();
    }

    private static void setFieldValue(Field field, Object obj, String value) throws IllegalAccessException {
        Class<?> fieldType = field.getType();
        
        if (fieldType == String.class) {
            field.set(obj, value);
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            field.set(obj, Boolean.valueOf(value));
        } else if (fieldType == Integer.class || fieldType == int.class) {
            try {
                field.set(obj, Integer.valueOf(value));
            } catch (NumberFormatException e) {
                // Ignore format errors
            }
        } else if (fieldType == Long.class || fieldType == long.class) {
            try {
                field.set(obj, Long.valueOf(value));
            } catch (NumberFormatException e) {
                // Ignore format errors
            }
        }
    }
}
