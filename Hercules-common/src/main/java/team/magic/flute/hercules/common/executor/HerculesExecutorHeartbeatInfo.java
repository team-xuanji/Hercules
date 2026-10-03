package team.magic.flute.hercules.common.executor;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain=true)
public class HerculesExecutorHeartbeatInfo {
    private String executorId;

    private String executorRegion;

    private String executorRegionDesc;

    private Integer executorMaxSlot;

    private Integer executorAvailableSlot;

    private boolean enableDuckdb=true;

    private ExecutorCurrentLoadPluginInfo executorLoadPluginInfo;

    private List<String> executorPluginHandleWhiteList;
}
