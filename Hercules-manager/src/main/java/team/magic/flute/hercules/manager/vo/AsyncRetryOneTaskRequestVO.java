package team.magic.flute.hercules.manager.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;

@Data
@Accessors(chain=true)
public class AsyncRetryOneTaskRequestVO {
    @NotEmpty
    private String taskId;
    private String errorMessage;
}
