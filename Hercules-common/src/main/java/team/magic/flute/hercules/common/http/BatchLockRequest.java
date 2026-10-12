package team.magic.flute.hercules.common.http;

import static team.magic.flute.hercules.common.global.Constant.BATCH_FETCH_MAX_SIZE;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class BatchLockRequest {
    @NotBlank private String executorId;
    @NotBlank private String executorRegion;

    @NotNull
    @Size(max = BATCH_FETCH_MAX_SIZE)
    @NotEmpty
    private List<String> taskIds;
}
