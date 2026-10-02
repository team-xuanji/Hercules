package team.magic.flute.hercules.manager.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TableNameHandler;
import org.apache.commons.lang3.StringUtils;

public class RecoverTableNameHandle implements TableNameHandler {
    private static final ThreadLocal<String> THREAD_LOCAL = new ThreadLocal<>();

    private static final String SPLIT_TABLE = "HERCULES_RECOVER_TASKS";

    public static void setSuffix(String value) {
        if(StringUtils.isNotBlank(value)){
            THREAD_LOCAL.set(value);
        }
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }

    @Override
    public String dynamicTableName(String sql, String tableName) {
        if(SPLIT_TABLE.equalsIgnoreCase(tableName)){
            String value = THREAD_LOCAL.get();
            if (value == null) {
                throw new IllegalArgumentException("Please set the suffix for the task recovery table name.");
            } else {
                return tableName + "_" + value;
            }
        }else{
            return tableName;
        }
    }
}
