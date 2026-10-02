package team.magic.flute.hercules.executor.config;


import com.github.f4b6a3.uuid.alt.GUID;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


@Configuration
@Data
public class RunnerEnv implements EnvironmentAware {
    private int totalSlot;
    private String duckdbPath;
    private String duckdbSpillPath;
    private int duckdbMemGBSize;
    private int duckdbSpillGBSize;
    private int duckdbThreadCount;
    private String executorRegion;
    private String runnerInstanceId;
    private boolean enableDuckdb;
    private String executorRegionDesc;
    private List<String> pluginWhiteList;

    @Value("${hercules.security.http-encrypt-key}")
    private String httpEncryptKey;

    @Override
    public void setEnvironment(Environment environment) {
        String whiteListStr = environment.getProperty(Constant.PLUGIN_WHITE_LIST,String.class,"");
        if(StringUtils.isBlank(whiteListStr)){
            this.pluginWhiteList = new ArrayList<>();
        }else{
            this.pluginWhiteList = Arrays.stream(whiteListStr.split(","))
                    .filter(StringUtils::isNotBlank)
                    .map(String::trim)
                    .collect(Collectors.toList());
        }
        this.executorRegionDesc = environment.getProperty(Constant.EXECUTOR_REGION_DESC,"I am Iron Man. (｀∀´)Ψ *snap!*");
        this.executorRegion = environment.getProperty(Constant.EXECUTOR_REGION,"TEST");
        this.duckdbThreadCount = environment.getProperty(Constant.THREAD_COUNT,int.class,Constant.DEFAULT_DUCKDB_MEM_GB_SIZE);
        this.enableDuckdb = environment.getProperty(Constant.ENABLE_DUCKDB,boolean.class,Constant.DEFAULT_ENABLE_DUCKDB);
        this.runnerInstanceId = "EXECUTOR_INSTANCE_"+ executorRegion +"_"+ GUID.v7();
        this.totalSlot = environment.getProperty(Constant.EXECUTOR_SLOT_SIZE,int.class,Constant.DEFAULT_EXECUTOR_SLOT_SIZE);
        this.duckdbPath = environment.getProperty(Constant.DUCKDB_STORAGE_PATH,Constant.DEFAULT_DUCKDB_STORAGE_PATH);
        this.duckdbSpillPath = environment.getProperty(Constant.DUCKDB_SPILL_PATH,Constant.DEFAULT_DUCKDB_SPILL_PATH);
        this.duckdbMemGBSize = environment.getProperty(Constant.DUCKDB_MEM_GB_SIZE,int.class,Constant.DEFAULT_DUCKDB_MEM_GB_SIZE);
        this.duckdbSpillGBSize = environment.getProperty(Constant.DUCKDB_SPILL_GB_SIZE,int.class,Constant.DEFAULT_DUCKDB_SPILL_GB_SIZE);
    }
}
