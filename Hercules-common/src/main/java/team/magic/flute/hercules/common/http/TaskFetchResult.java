package team.magic.flute.hercules.common.http;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain=true)
public class TaskFetchResult {
    private List<HerculesRunnableTaskInfo> taskInfoList;
    private boolean crossPartition;
    public boolean isEmpty() {
        return taskInfoList == null || taskInfoList.isEmpty();
    }
}
