package team.magic.flute.hercules.manager.vo;


import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.executor.ExecutorCurrentLoadPluginInfo;
import team.magic.flute.hercules.common.executor.ExecutorProcessHandleWhiteList;
import team.magic.flute.hercules.manager.dao.po.HerculesExecutorInfo;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ExecutorInfoReportRequestVO {

    @NotBlank(message = "Executor Instance Id can not be empty")
    private String executorId;

    @NotBlank(message = "Executor Region Id can not be empty")
    private String executorRegion;

    private String executorRegionDesc;

    private Integer executorMaxSlot;

    private Integer executorAvailableSlot;

    private boolean enableDuckdb=true;

    private ExecutorCurrentLoadPluginInfo executorLoadPluginInfo;

    private List<String> executorPluginHandleWhiteList;

    public HerculesExecutorInfo parse2Po(){
        return new HerculesExecutorInfo()
                .setExecutorId(executorId)
                .setExecutorRegion(executorRegion)
                .setExecutorRegionDesc(executorRegionDesc)
                .setExecutorMaxSlot(executorMaxSlot)
                .setExecutorAvailableSlot(executorAvailableSlot)
                .setEnableDuckdb(enableDuckdb)
                .setExecutorLoadPluginInfo(executorLoadPluginInfo)
                .setExecutorPluginHandleWhiteList(new ExecutorProcessHandleWhiteList().setWhiteList(executorPluginHandleWhiteList))
                .setInsertTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
    }
}
