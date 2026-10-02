package team.magic.flute.hercules.twelve.labors.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * CMS Data Download Task Definition Persistent Object
 *
 * <p>This persistent object represents the task definition configuration for data download
 * operations within the Hercules business access gateway. It encapsulates all necessary
 * configuration parameters required to define reusable data export templates that can be
 * executed multiple times with different parameters.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this entity enables the creation and management of standardized data export templates
 * that support various business operations including:
 * <ul>
 *   <li>Reusable data export configurations for different business scenarios</li>
 *   <li>Template-based data extraction with parameterized SQL queries</li>
 *   <li>Multi-format export support (CSV, JSON, Parquet, XLSX)</li>
 *   <li>Cloud storage integration with configurable destinations</li>
 * </ul>
 *
 * <p><strong>Configuration Capabilities:</strong>
 * <ul>
 *   <li>SQL template definitions with parameter substitution</li>
 *   <li>Database connection configuration through RDS information</li>
 *   <li>Export format and compression settings</li>
 *   <li>Object storage (OSS) destination configuration</li>
 *   <li>Plugin-based execution framework integration</li>
 * </ul>
 *
 * <p><strong>Database Mapping:</strong> This entity maps to the HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF
 * table and uses MyBatis Plus annotations for ORM functionality, supporting automatic
 * result mapping and type handling for complex data structures.
 *
 * <p><strong>Usage:</strong> Task definitions created through this entity serve as templates
 * that can be instantiated multiple times to create actual data export tasks, enabling
 * efficient and consistent data export operations across the business access gateway.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
@TableName(value = "HERCULES_CMS_DATA_DOWNLOAD_TASK_DEF", autoResultMap = true)
public class CmsDataDownloadTaskDefPO {
    /**
     * Business scenario key (primary key)
     */
    @TableId(value = "BUSINESS_KEY")
    private String businessKey;
    /**
     * Business scenario description
     */
    @TableField("BUSINESS_DESC")
    private String businessDesc;

    /**
     * Executor business key
     */
    @TableField("EXECUTOR_REGION")
    private String executorRegion;

    /**
     * Plugin group
     */
    @TableField("PLUGIN_GROUP")
    private String pluginGroup;

    /**
     * Plugin handler
     */
    @TableField("PLUGIN_HANDLE")
    private String pluginHandle;

    /**
     * SQL template, requires sqlParamTemplate for PrepareStatement compilation
     */
    @TableField("SQL_TEMPLATE")
    private String sqlTemplate;

    /**
     * SQL parameter template, JSON format
     * Map(String,String)
     */
    @TableField("SQL_PARAM_TEMPLATE")
    private String sqlParamTemplate;

    /**
     * SQL RDS information.
     */
    @TableField("SQL_RDS_INFOS")
    private String sqlRdsInfos;

    /**
     * Export format
     */
    @TableField("EXPORT_FORMAT_TYPE")
    private String exportFormatType;

    /**
     * Export parameters, mainly controls export format and compression format related detail parameters
     * CsvExportFormatParams
     * ExcelExportFormatParams
     * JsonExportFormatParams
     * ParquetExportFormatParams
     */
    @TableField("EXPORT_FORMAT_CONFIG")
    private String exportFormatConfig;
    /**
     * Export file OSS bucket
     */
    @TableField("OSS_BUCKET")
    private String ossBucket;

    /**
     * Root path of export files
     */
    @TableField("OSS_ROOT_PATH")
    private String ossRootPath;

    /**
     * OSS access key
     */
    @TableField("OSS_ACCESS_ID")
    private String ossAccessId;

    /**
     * OSS access secret
     */
    @TableField("OSS_ACCESS_SECRET")
    private String ossAccessSecret;

    /**
     * OSS endpoint
     */
    @TableField("OSS_ENDPOINT")
    private String ossEndpoint;

    /**
     * OSS Region
     *
     * <p>The geographic region where the OSS bucket is located. This is used for
     * optimizing network performance and ensuring compliance with data residency
     * requirements.
     *
     * <p>Example: endpoint = oss-cn-zhangjiakou.aliyuncs.com, region = cn-zhangjiakou
     */
    @TableField("OSS_REGION")
    private String ossRegion;

    /**
     * Export file name prefix
     *
     * <p>A prefix that will be prepended to all exported file names generated
     * from this task definition. This helps in organizing and identifying files
     * from different export operations.
     */
    @TableField("FILE_PREFIX")
    private String filePrefix;

    /**
     * Insert time
     *
     * <p>The timestamp when this task definition record was first created in the
     * database. This field is automatically set during insertion and never updated
     * afterwards to maintain audit trail integrity.
     */
    @TableField(value = "INSERT_TIME",updateStrategy= FieldStrategy.NEVER)
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime insertTime;

    /**
     * Update time
     *
     * <p>The timestamp when this task definition record was last modified.
     * This field is automatically updated whenever any changes are made to
     * the record, providing a complete audit trail of modifications.
     */
    @TableField("UPDATE_TIME")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime updateTime;
}
