package team.magic.flute.hercules.twelve.labors.business.export.entity;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * RDS Database Connection Information
 *
 * <p>This class encapsulates the connection parameters required to establish a connection
 * to a Relational Database Service (RDS) instance for data export operations within the
 * Hercules business access gateway. It provides a standardized way to configure database
 * connections for various business data extraction scenarios.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this class enables secure and flexible database connectivity for business operations
 * including:
 * <ul>
 *   <li>Multi-tenant data access with proper isolation</li>
 *   <li>Cross-database data integration and consolidation</li>
 *   <li>Business intelligence and reporting data extraction</li>
 *   <li>Data migration and synchronization operations</li>
 * </ul>
 *
 * <p><strong>Security Considerations:</strong> This class contains sensitive database
 * credentials and should be handled with appropriate security measures including
 * encryption at rest and in transit, secure credential management, and access logging.
 *
 * <p><strong>Usage:</strong> Typically used within {@link RdsExportContext} to define
 * one or more database sources for data export operations, supporting complex scenarios
 * where data needs to be extracted from multiple database instances.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain=true)
public class RdsInfo {
    /**
     * RDS database server hostname or IP address
     *
     * <p>The network address of the RDS database server. This can be a hostname,
     * IP address, or fully qualified domain name (FQDN) that is accessible from
     * the Hercules execution environment.
     */
    private String rdsUrl;

    /**
     * Database attachment name for query execution
     *
     * <p>A logical name used to reference this database connection within the
     * query execution context. This allows for clear identification when working
     * with multiple database sources in complex export operations.
     */
    private String rdsAttachName;

    /**
     * Database server port number
     *
     * <p>The TCP port number on which the database server is listening for
     * connections. Common default ports include 3306 for MySQL, 5432 for PostgreSQL,
     * and 1433 for SQL Server.
     */
    private String rdsPort;

    /**
     * Target database name
     *
     * <p>The specific database/schema name within the RDS instance that contains
     * the data to be exported. This allows for precise targeting of data sources
     * within multi-database RDS instances.
     */
    private String rdsDatabaseName;

    /**
     * Database authentication username
     *
     * <p>The username credential used for authenticating with the database server.
     * This user must have appropriate read permissions for the data being exported
     * and should follow the principle of least privilege.
     */
    private String rdsUser;

    /**
     * Database authentication password
     *
     * <p>The password credential used for authenticating with the database server.
     * This sensitive information should be handled securely and may be encrypted
     * or retrieved from secure credential management systems.
     */
    private String rdsPassword;
}
