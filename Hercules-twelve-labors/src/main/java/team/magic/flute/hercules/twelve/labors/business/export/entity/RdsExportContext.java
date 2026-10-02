package team.magic.flute.hercules.twelve.labors.business.export.entity;


import team.magic.flute.hercules.twelve.labors.constant.CmsDownloadFormat;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;

/**
 * RDS Data Export Context Configuration
 *
 * <p>This class encapsulates the complete configuration context for RDS (Relational Database Service)
 * data export operations within the Hercules business access gateway. It serves as a comprehensive
 * configuration container that defines all necessary parameters for executing database export tasks
 * through the distributed task execution framework.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this context enables flexible and configurable data export operations, supporting various
 * business scenarios including:
 * <ul>
 *   <li>Multi-database data extraction and consolidation</li>
 *   <li>Cross-platform data migration and synchronization</li>
 *   <li>Business intelligence and analytics data preparation</li>
 *   <li>Compliance and audit data export requirements</li>
 * </ul>
 *
 * <p><strong>Export Capabilities:</strong>
 * <ul>
 *   <li>Multiple database source support through RDS connection pooling</li>
 *   <li>Flexible output formats (CSV, JSON, Parquet, XLSX)</li>
 *   <li>Cloud storage integration (OSS/S3) for scalable data delivery</li>
 *   <li>Advanced query optimization and performance tuning options</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> This context is used by the Hercules task execution engine
 * to configure and execute data export operations, providing a standardized interface for
 * various business functions that require data extraction capabilities.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class RdsExportContext {
    /**
     * List of RDS database connection configurations
     *
     * <p>Contains connection information for one or more RDS instances that will be
     * used as data sources for the export operation. Supports multi-database scenarios
     * where data needs to be extracted from multiple sources.
     */
    private List<RdsInfo> rdsInfos;

    /**
     * SQL query to execute for data extraction
     *
     * <p>The SQL query that will be executed against the configured RDS instances
     * to extract the required data. Supports complex queries with joins, aggregations,
     * and filtering to meet specific business requirements.
     */
    private String rdsQuery;

    /**
     * Target path in object storage for the exported file
     *
     * <p>Specifies the destination path within the configured object storage bucket
     * where the exported data file will be stored. Used for organizing and managing
     * exported data files.
     */
    private String ossPath;

    /**
     * Object storage bucket name
     *
     * <p>The name of the object storage bucket where the exported data will be stored.
     * This bucket must be accessible with the provided credentials.
     */
    private String bucketName;

    /**
     * Object storage access key
     *
     * <p>The access key used for authenticating with the object storage service.
     * Required for uploading the exported data files to the target storage location.
     */
    private String ossKey;

    /**
     * Object storage secret key
     *
     * <p>The secret key used for authenticating with the object storage service.
     * Must be kept secure and used in conjunction with the access key.
     */
    private String ossSecret;

    /**
     * Object storage region
     *
     * <p>The geographic region where the object storage bucket is located.
     * Used for optimizing network performance and compliance requirements.
     */
    private String ossRegion;

    /**
     * Object storage service endpoint
     *
     * <p>The endpoint URL for the object storage service. This can be a standard
     * cloud provider endpoint or a custom endpoint for private cloud deployments.
     */
    private String ossEndpoint;

    /**
     * DuckDB MySQL experimental filter pushdown optimization flag
     *
     * <p>Controls whether to enable experimental filter pushdown optimization
     * when using DuckDB with MySQL sources. This can improve query performance
     * but may have compatibility considerations with certain MySQL versions.
     */
    private Boolean duckdbMysqlExperimentalFilterPushdown;

    /**
     * Export file format specification
     *
     * <p>Defines the output format for the exported data. Supported formats include:
     * <ul>
     *   <li>XLSX - Excel spreadsheet format for business users</li>
     *   <li>CSV - Comma-separated values for data processing</li>
     *   <li>JSON - JavaScript Object Notation for API integration</li>
     *   <li>PARQUET - Columnar format for analytics and big data processing</li>
     * </ul>
     */
    private CmsDownloadFormat exportFormat;

    /**
     * Format-specific configuration parameters
     *
     * <p>Contains additional configuration parameters specific to the chosen export format.
     * For example, CSV delimiter settings, JSON formatting options, or Parquet compression
     * settings. The exact parameters depend on the selected export format.
     */
    private Map<String,String> formatConfig;
}
