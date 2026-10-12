package team.magic.flute.hercules.manager.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FinishOneTaskRequestVO {
    @NotBlank(message = "Task ID must be specified.")
    private String taskId;

    @NotBlank(
            message =
                    "The execution of the task must conclude with writing CHECKPOINT information.")
    private String checkPointInfo;

    @NotBlank(message = "Executor ID must be specified.")
    private String executorId;
}
