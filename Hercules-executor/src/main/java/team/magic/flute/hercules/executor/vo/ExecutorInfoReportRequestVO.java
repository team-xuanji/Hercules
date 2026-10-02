package team.magic.flute.hercules.executor.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.executor.ExecutorCurrentLoadPluginInfo;

import java.util.List;

@Data
@Accessors(chain = true)
public class ExecutorInfoReportRequestVO {

    private String executorId;

    private String executorRegion;

    private String executorRegionDesc;

    private Integer executorMaxSlot;

    private Integer executorAvailableSlot;

    private boolean enableDuckdb=true;

    private ExecutorCurrentLoadPluginInfo executorLoadPluginInfo;

    private List<String> executorPluginHandleWhiteList;
}
