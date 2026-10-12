package team.magic.flute.hercules.manager.vo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class AsyncRetryOneTaskRequestVO {
    @NotEmpty private String taskId;
    private String errorMessage;
}
