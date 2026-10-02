package team.magic.flute.hercules.executor.vo;

import lombok.Data;
import lombok.experimental.Accessors;


@Data
@Accessors(chain=true)
public class FinishOneTaskRequestVO {
    private String taskId;
    private String checkPointInfo;
}
