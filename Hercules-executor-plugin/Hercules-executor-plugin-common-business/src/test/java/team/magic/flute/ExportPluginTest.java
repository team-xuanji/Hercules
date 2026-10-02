package team.magic.flute;

import java.sql.DriverManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.duckdb.DuckDBConnection;
import team.magic.flute.hercules.common.status.TaskExecutionContext;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.plugin.data.export.entity.RdsExportContext;
import team.magic.flute.hercules.plugin.data.export.entity.RdsInfo;
import team.magic.flute.hercules.plugin.data.export.rds.RdsExportPlugin;

/** Unit test for simple App. */
public class ExportPluginTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public ExportPluginTest(String testName) {
        super(testName);
    }

    /** @return the suite of tests being tested */
    public static Test suite() {
        return new TestSuite(ExportPluginTest.class);
    }

    /** Rigourous Test :-) */
    public void testApp() throws Exception {
        DuckDBConnection conn = (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb:");
        TaskExecutionContext context = new TaskExecutionContext();
        context.setDuckdbConnection(conn);
        RdsExportPlugin plugin = new RdsExportPlugin();
        RdsExportContext exportContext = new RdsExportContext();
        RdsInfo rdsInfo = new RdsInfo();
        //        ATTACH 'host=localhost user=root port=3306 database=test password=xxx
        // ' AS mysqldb (TYPE mysql);
        //        USE mysqldb;
        rdsInfo.setRdsUrl("localhost")
                .setRdsPort("3306")
                .setRdsDatabaseName("test")
                .setRdsUser("root")
                .setRdsPassword("xxx")
                .setRdsAttachName("temp01");
        exportContext
                .setRdsInfos(Collections.singletonList(rdsInfo))
                .setRdsQuery("SELECT * FROM temp01.third_part_coupon_code_detail");
        Map<String, String> exportConfigMap = new HashMap<>();
        //        exportConfigMap.put("HEADER","true");
        exportConfigMap.put("COMPRESSION", "GZIP");
        //        exportConfigMap.put("OVERWRITE_OR_IGNORE","true");
        exportContext
                .setBucketName("test-bucket")
                .setOssEndpoint("oss-cn-zhangjiakou.aliyuncs.com")
                .setOssKey("xx")
                .setOssSecret("xx")
                .setOssRegion("cn-zhangjiakou")
                .setOssPath("test-duckdb-io/test_output/t121.csv.gz")
                .setExportFormat("CSV")
                .setFormatConfig(exportConfigMap);
        context.setExecutionContext(JacksonUtils.writeValueAsString(exportContext));
        Map<String, Object> data = new HashMap<>();
        data.put("businessKey", "test");
        data.put("context", JacksonUtils.writeValueAsString(exportContext));
        data.put("pluginHandle", "common_rds_exporter_v1");
        data.put("pluginGroup", "test02");
        //        @NotNull
        //        private String pluginHandle;
        //        @NotNull
        //        private String pluginGroup;
        System.out.println(JacksonUtils.writeValueAsString(data));
        //        plugin.execute(context);
    }
}
