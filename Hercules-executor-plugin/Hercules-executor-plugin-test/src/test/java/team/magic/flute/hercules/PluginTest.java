package team.magic.flute.hercules;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.duckdb.DuckDBConnection;

/** Unit test for simple App. */
public class PluginTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public PluginTest(String testName) {
        super(testName);
    }

    /** @return the suite of tests being tested */
    public static Test suite() {
        return new TestSuite(PluginTest.class);
    }

    /** Rigourous Test :-) */
    public void testApp() throws SQLException {
        // Test OSS read/write operations
        DuckDBConnection conn =
                (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb::memory:");
        // create a table
        Statement stmt = conn.createStatement();
        String defineOssSql =
                "CREATE OR REPLACE SECRET test_oss_link (\n"
                        + "    TYPE s3,\n"
                        + "    PROVIDER config,\n"
                        + "    KEY_ID 'xx',\n"
                        + "    SECRET 'xx',\n"
                        + "    REGION 'cn-zhangjiakou',\n"
                        + "    ENDPOINT 'oss-cn-zhangjiakou.aliyuncs.com'\n"
                        + ");";
        stmt.execute(
                "CREATE TABLE IF NOT EXISTS items (item VARCHAR, value DECIMAL(10, 2), count INTEGER)");
        // insert two items into the table
        stmt.execute("INSERT INTO items VALUES ('jeans111', 20.0, 1), ('hammer', 42.2, 2)");

        String copy2OssWithCsvFileSql =
                "COPY items TO 's3://shuyun-cem-qa/test-duckdb-io/test_output/t12.csv' (\n"
                        + "    FORMAT csv,\n"
                        + "    OVERWRITE_OR_IGNORE true,\n"
                        + "    HEADER true,\n"
                        + "    DELIMITER ','\n"
                        + ");";
        stmt.execute(defineOssSql);
        stmt.execute(copy2OssWithCsvFileSql);
    }
}
