package team.magic.flute.hercules.common.executor;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Collection;

@Data
@Accessors(chain=true)
public class ExecutorProcessHandleWhiteList {
    private Collection<String> whiteList;
}
