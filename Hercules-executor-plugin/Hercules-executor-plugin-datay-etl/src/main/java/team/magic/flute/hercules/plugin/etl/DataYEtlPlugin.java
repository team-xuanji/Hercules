package team.magic.flute.hercules.plugin.etl;

import com.data.job.ETLFlowTask;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import team.magic.flute.hercules.common.plugin.TaskPlugin;
import team.magic.flute.hercules.common.status.TaskExecutionContext;

/** This plugin uses duckdb to export data from MySQL data sources. */
@AutoService(TaskPlugin.class)
@Slf4j
public class DataYEtlPlugin implements TaskPlugin {


    @Override
    public String getName() {
        return "datay_etl_plugin_v1";
    }

    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        String job = context.getExecutionContext();
        ETLFlowTask task = new ETLFlowTask();
        task.runJob(job);
    }
}
