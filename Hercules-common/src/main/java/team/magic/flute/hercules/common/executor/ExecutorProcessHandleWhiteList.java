package team.magic.flute.hercules.common.executor;

import java.util.Collection;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ExecutorProcessHandleWhiteList {
    private Collection<String> whiteList;
}
