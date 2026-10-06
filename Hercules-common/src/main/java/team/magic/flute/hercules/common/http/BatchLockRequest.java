package team.magic.flute.hercules.common.http;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Data
@Accessors(chain=true)
public class BatchLockRequest {
    @NotBlank
    private String executorId;
    @NotBlank
    private String passSign;
    @NotBlank
    private String executorRegion;
    @NotNull
    @Size(max=200)
    @NotEmpty
    private List<String> taskIds;
}
