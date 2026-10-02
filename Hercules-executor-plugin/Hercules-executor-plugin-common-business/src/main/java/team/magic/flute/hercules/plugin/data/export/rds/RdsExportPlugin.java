package team.magic.flute.hercules.plugin.data.export.rds;

import com.google.auto.service.AutoService;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import team.magic.flute.hercules.common.plugin.TaskPlugin;
import team.magic.flute.hercules.common.status.TaskExecutionContext;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.plugin.data.export.entity.RdsExportContext;
import team.magic.flute.hercules.plugin.data.export.entity.RdsInfo;
import team.magic.flute.hercules.plugin.data.export.util.ExportParamFormatUtil;

/** This plugin uses duckdb to export data from MySQL data sources. */
@AutoService(TaskPlugin.class)
@Slf4j
public class RdsExportPlugin implements TaskPlugin {
    private static final String ATTACH_RDS_SQL_TEMPLATE =
            "ATTACH IF NOT EXISTS 'host=${hostname} user=${userName} port=${port} database=${databaseName} password=${password}' AS ${attachDataBaseName} (TYPE mysql,READ_ONLY)";
    private static final String EXPORT_SQL_TEMPLATE =
            "COPY (${exportSql}) TO '${ossPath}' (\n"
                    + "    FORMAT ${exportFormat},\n"
                    + "${exportFormatProps}"
                    + ");";
    private static final String OSS_SECRET_SQL_TEMPLATE =
            "CREATE SECRET IF NOT EXISTS ${secretName} (\n"
                    + "    TYPE s3,\n"
                    + "    PROVIDER config,\n"
                    + "    KEY_ID '${ossKey}',\n"
                    + "    SECRET '${ossSecret}',\n"
                    + "    REGION '${ossRegion}',\n"
                    + "    ENDPOINT '${ossEndpoint}'\n"
                    + ");";
    private volatile boolean initializeFlag = false;

    @Override
    public String getName() {
        return "common_rds_exporter_v1";
    }

    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        String executionContextStr = context.getExecutionContext();
        log.info("Executing data import task, context.:" + executionContextStr);
        if (executionContextStr == null || executionContextStr.trim().isEmpty()) {
            log.warn(
                    "Executing the common_rds_exporter_v1 plugin, but the context information is empty, skipping execution.");
            return;
        }
        // todo: The customer hopes to support automatic determination of size and selection of
        //  compression algorithms in the later stages. To implement this feature,
        //  the basic approach is to first import the table into an internal DuckDB table,
        //  then count the number of rows and the size of the table. If the characteristics meet
        //  certain criteria, a compression algorithm will be selected. This is because doing this
        //  in the original database may not be appropriate, as both the statistics and export
        //  operations would require remote IO, which is not ideal.
        RdsExportContext exportContext =
                JacksonUtils.readValue(executionContextStr, RdsExportContext.class);
        String secretName =
                "a"
                        + DigestUtils.md5Hex(
                                exportContext.getOssEndpoint()
                                        + exportContext.getOssKey()
                                        + exportContext.getOssSecret()
                                        + exportContext.getOssRegion());
        String finalExportPath =
                "s3://" + exportContext.getBucketName() + "/" + exportContext.getOssPath();
        String exportSql =
                EXPORT_SQL_TEMPLATE
                        .replace("${exportSql}", exportContext.getRdsQuery())
                        .replace("${ossPath}", finalExportPath)
                        .replace("${exportFormat}", exportContext.getExportFormat().toLowerCase())
                        .replace(
                                "${exportFormatProps}",
                                ExportParamFormatUtil.formatExportParam(
                                        exportContext.getExportFormat().toLowerCase(),
                                        exportContext.getFormatConfig()));
        String createOssSecretSql =
                OSS_SECRET_SQL_TEMPLATE
                        .replace("${secretName}", secretName)
                        .replace("${ossKey}", exportContext.getOssKey())
                        .replace("${ossSecret}", exportContext.getOssSecret())
                        .replace("${ossRegion}", exportContext.getOssRegion())
                        .replace("${ossEndpoint}", exportContext.getOssEndpoint());
        try (Connection connection = context.getDuckdbConnection();
                Statement statement = connection.createStatement()) {
            if (Objects.equals(
                    Boolean.FALSE, exportContext.getDuckdbMysqlExperimentalFilterPushdown())) {
                // For duckdb1.2.2.0,see https://github.com/duckdb/duckdb-mysql/issues/127
                // if you use duckdb 1.3.2+, you can drop this.
                statement.execute("SET mysql_experimental_filter_pushdown = false");
            }
            // force load excel,but maybe no need
            if (!initializeFlag) {
                synchronized (this) {
                    if (!initializeFlag) {
                        try {
                            statement.execute("LOAD excel");
                        } catch (Exception e) {
                            log.error(
                                    "DuckDB does not support multiple instances loading the same extension in parallel.");
                        }
                        initializeFlag = true;
                    }
                }
            }
            try {
                for (RdsInfo rdsInfo :
                        Optional.ofNullable(exportContext.getRdsInfos())
                                .orElse(new ArrayList<>())) {
                    String databaseName =
                            rdsInfo.getRdsAttachName() == null
                                    ? DigestUtils.md5Hex(rdsInfo.getRdsUrl() + rdsInfo.getRdsUser())
                                    : rdsInfo.getRdsAttachName();
                    String attachRdsSql =
                            ATTACH_RDS_SQL_TEMPLATE
                                    .replace("${databaseName}", rdsInfo.getRdsDatabaseName())
                                    .replace("${hostname}", rdsInfo.getRdsUrl())
                                    .replace("${userName}", rdsInfo.getRdsUser())
                                    .replace("${password}", rdsInfo.getRdsPassword())
                                    .replace("${port}", rdsInfo.getRdsPort())
                                    .replace("${attachDataBaseName}", databaseName);
                    statement.execute(attachRdsSql);
                }
                log.info("Create s3 secret:{}", createOssSecretSql);
                statement.execute(createOssSecretSql);
                log.info("Execute data export sql: {}", exportSql);
                statement.execute(exportSql);
            } finally {
                log.info("Starting to clean up resources.");
                resetConnection(connection, statement, secretName, exportContext, null);
            }
            log.info("Export completed. The file path is:" + exportContext.getOssPath());
            context.setCheckPointResult(exportContext.getOssPath());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void resetConnection(
            Connection connection,
            Statement statement,
            String secretName,
            RdsExportContext exportContext,
            List<String> tempTableNames)
            throws Exception {
        if (!statement.isClosed()) {
            doClean(statement, secretName, exportContext, tempTableNames);
        } else {
            try (Statement statement1 = connection.createStatement(); ) {
                doClean(statement1, secretName, exportContext, tempTableNames);
            }
        }
    }

    private static void doClean(
            Statement statement,
            String secretName,
            RdsExportContext exportContext,
            List<String> tempTableNames)
            throws SQLException {
        if (Objects.equals(
                Boolean.FALSE, exportContext.getDuckdbMysqlExperimentalFilterPushdown())) {
            // For duckdb1.2.2.0,see https://github.com/duckdb/duckdb-mysql/issues/127
            statement.execute("SET mysql_experimental_filter_pushdown = true");
        }
        for (RdsInfo rdsInfo :
                Optional.ofNullable(exportContext.getRdsInfos()).orElse(new ArrayList<>())) {
            String databaseName =
                    rdsInfo.getRdsAttachName() == null
                            ? DigestUtils.md5Hex(rdsInfo.getRdsUrl() + rdsInfo.getRdsUser())
                            : rdsInfo.getRdsAttachName();
            String detachRdsSql = "DETACH DATABASE IF EXISTS " + databaseName;
            log.info("detach database:{}", detachRdsSql);
            log.info("DETACH DATABASE IF EXISTS {}", databaseName);
            statement.execute(detachRdsSql);
        }
        statement.execute("DROP SECRET IF EXISTS " + secretName);
        log.info("DROP SECRET IF EXISTS {}", secretName);
        if (tempTableNames != null && !tempTableNames.isEmpty()) {
            log.info("Clean up temporary internal tables.");
            for (String tempTableName : tempTableNames) {
                if (tempTableName != null && !tempTableName.trim().isEmpty()) {
                    log.info("Starting to clean up temporary internal tables[{}]", tempTableName);
                    statement.execute("DROP TABLE IF EXISTS " + tempTableName.trim());
                }
            }
        }
    }
}
