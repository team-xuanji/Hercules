package team.magic.flute.hercules.twelve.labors.constant;

/**
 * CMS Data Download Format Enumeration
 *
 * <p>This enumeration defines the supported data export formats available through the
 * Hercules business access gateway. Each format is optimized for different use cases
 * and business requirements, providing flexibility for various data consumption scenarios.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * these formats enable diverse business operations and data integration scenarios:
 * <ul>
 *   <li>Business user-friendly formats for reporting and analysis</li>
 *   <li>Developer-friendly formats for API integration and processing</li>
 *   <li>Analytics-optimized formats for big data and machine learning</li>
 *   <li>Interoperability formats for cross-system data exchange</li>
 * </ul>
 *
 * <p><strong>Format Characteristics:</strong>
 * <ul>
 *   <li>XLSX - Excel format for business users and spreadsheet applications</li>
 *   <li>CSV - Universal text format for data processing and import/export</li>
 *   <li>PARQUET - Columnar format optimized for analytics and big data processing</li>
 *   <li>JSON - Structured format ideal for API integration and web applications</li>
 * </ul>
 *
 * <p><strong>Usage:</strong> This enumeration is used throughout the business access gateway
 * to specify export format preferences, validate format support, and configure export
 * operations for various business functions.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public enum CmsDownloadFormat {
    /**
     * Microsoft Excel format (.xlsx)
     *
     * <p>Excel spreadsheet format that provides rich formatting capabilities and is
     * widely used by business users for data analysis, reporting, and presentation.
     * Supports multiple worksheets, formulas, and advanced formatting options.
     */
    XLSX,

    /**
     * Comma-Separated Values format (.csv)
     *
     * <p>Universal plain text format that uses commas to separate values. Widely
     * supported across platforms and applications, making it ideal for data exchange,
     * import/export operations, and processing by various tools and systems.
     */
    CSV,

    /**
     * Apache Parquet format (.parquet)
     *
     * <p>Columnar storage format optimized for analytics workloads and big data
     * processing. Provides excellent compression ratios and query performance,
     * making it ideal for data warehousing, analytics, and machine learning scenarios.
     */
    PARQUET,

    /**
     * JavaScript Object Notation format (.json)
     *
     * <p>Lightweight, text-based data interchange format that is easy for humans
     * to read and write. Widely used in web applications, APIs, and modern data
     * processing pipelines due to its flexibility and broad language support.
     */
    JSON;
}
