package team.magic.flute.hercules.manager.entity.cron;

import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

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
