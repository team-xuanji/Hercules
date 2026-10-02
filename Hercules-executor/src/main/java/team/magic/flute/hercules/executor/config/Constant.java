package team.magic.flute.hercules.executor.config;

public class Constant {
    public final static String DUCKDB_MEM_GB_SIZE="DUCKDB_MEM_GB_SIZE";
    public final static String DUCKDB_SPILL_GB_SIZE="DUCKDB_SPILL_GB_SIZE";
    public final static String DUCKDB_SPILL_PATH="DUCKDB_SPILL_PATH";
    public final static String DUCKDB_STORAGE_PATH="DUCKDB_STORAGE_PATH";
    public final static String EXECUTOR_SLOT_SIZE="EXECUTOR_SLOT_SIZE";
    public final static String THREAD_COUNT="THREAD_COUNT";
    public final static String EXECUTOR_REGION="EXECUTOR_REGION";
    public final static String EXECUTOR_REGION_DESC="EXECUTOR_REGION_DESC";
    public final static String ENABLE_DUCKDB="ENABLE_DUCKDB";
    public final static String PLUGIN_WHITE_LIST="PLUGIN_WHITE_LIST";

    public final static boolean DEFAULT_ENABLE_DUCKDB=true;
    public final static int DEFAULT_DUCKDB_MEM_GB_SIZE=1;
    public final static int DEFAULT_DUCKDB_SPILL_GB_SIZE=200;
    public final static String DEFAULT_DUCKDB_SPILL_PATH= "/var/duckdb_spill_hercules/";
    public final static String DEFAULT_DUCKDB_STORAGE_PATH="/var/duckdb_data_hercules/identifier.db";
    public final static int DEFAULT_EXECUTOR_SLOT_SIZE=4;
}
