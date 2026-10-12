package team.magic.flute.hercules.manager.entity.cron;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;

@Data
@Accessors(chain = true)
public class TaskUpdateInfo {
    private String id;
    private LocalDateTime snapshotBeforeTrigger;
    private List<HerculesTaskInfo> dispatchTasks;
    private LocalDateTime snapshotAfterTrigger;
    private String checkpointAfterTrigger;
    private String checkpointBeforeTrigger;
}
