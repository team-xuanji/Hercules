package team.magic.flute.hercules;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.duckdb.DuckDBConnection;

/** Unit test for simple App. */
public class AppTest01 extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest01(String testName) {
        super(testName);
    }

    /** @return the suite of tests being tested */
    public static Test suite() {
        return new TestSuite(AppTest01.class);
    }

    /** Rigourous Test :-) */
    public void testApp() throws SQLException {
        DuckDBConnection conn =
                (DuckDBConnection) DriverManager.getConnection("jdbc:duckdb::memory:");
        // create a table
        Statement stmt = conn.createStatement();
        //        stmt.execute("INSTALL arrow;");
        stmt.execute("INSTALL httpfs;");
        stmt.execute("INSTALL vss;");
        stmt.execute("INSTALL mysql;");
        stmt.execute("INSTALL aws;");
        stmt.execute("INSTALL fts;");
        stmt.setFetchSize(1000);
        stmt.execute(
                "CREATE TABLE IF NOT EXISTS items (item VARCHAR, value DECIMAL(10, 2), count INTEGER)");
        // insert two items into the table
        stmt.execute("INSERT INTO items VALUES ('jeans', 20.0, 1), ('hammer', 42.2, 2)");

        try (ResultSet rs = stmt.executeQuery("SELECT * FROM items")) {
            while (rs.next()) {
                System.out.println(rs.getString(1));
                System.out.println(rs.getInt(3));
            }
        }
    }
}
