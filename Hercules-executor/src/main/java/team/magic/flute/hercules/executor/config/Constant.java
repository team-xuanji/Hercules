package team.magic.flute.hercules.executor.config;

public class Constant {
    public static final String DUCKDB_MEM_GB_SIZE = "DUCKDB_MEM_GB_SIZE";
    public static final String DUCKDB_SPILL_GB_SIZE = "DUCKDB_SPILL_GB_SIZE";
    public static final String DUCKDB_SPILL_PATH = "DUCKDB_SPILL_PATH";
    public static final String DUCKDB_STORAGE_PATH = "DUCKDB_STORAGE_PATH";
    public static final String EXECUTOR_SLOT_SIZE = "EXECUTOR_SLOT_SIZE";
    public static final String THREAD_COUNT = "THREAD_COUNT";
    public static final String EXECUTOR_REGION = "EXECUTOR_REGION";
    public static final String EXECUTOR_REGION_DESC = "EXECUTOR_REGION_DESC";
    public static final String ENABLE_DUCKDB = "ENABLE_DUCKDB";
    public static final String PLUGIN_WHITE_LIST = "PLUGIN_WHITE_LIST";

    public static final boolean DEFAULT_ENABLE_DUCKDB = true;
    public static final int DEFAULT_DUCKDB_MEM_GB_SIZE = 1;
    public static final int DEFAULT_DUCKDB_SPILL_GB_SIZE = 200;
    public static final String DEFAULT_DUCKDB_SPILL_PATH = "/var/duckdb_spill_hercules/";
    public static final String DEFAULT_DUCKDB_STORAGE_PATH =
            "/var/duckdb_data_hercules/identifier.db";
    public static final int DEFAULT_EXECUTOR_SLOT_SIZE = 4;
}
