package team.magic.flute.hercules.common.http;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

import static team.magic.flute.hercules.common.global.Constant.BATCH_FETCH_MAX_SIZE;

@Data
@Accessors(chain=true)
public class BatchLockRequest {
    @NotBlank
    private String executorId;
    @NotBlank
    private String executorRegion;
    @NotNull
    @Size(max=BATCH_FETCH_MAX_SIZE)
    @NotEmpty
    private List<String> taskIds;
}
