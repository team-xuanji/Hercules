package team.magic.flute.hercules.manager.config;

import com.github.f4b6a3.uuid.alt.GUID;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;


@Configuration
@Data
public class RunnerEnv implements EnvironmentAware {
    private String runnerId;
    @Value("${hercules.security.http-encrypt-key}")
    private String httpEncryptKey;
    @Value("${hercules.task.max-chain-depth:32}")
    private int maxChainDepth;
    @Override
    public void setEnvironment(Environment environment) {
        runnerId = GUID.v7().toString().replace("-","");
    }

    /*
     * Fail fast at startup: a non-positive limit would reject every FORWARD
     * submission (depth >= 1 > 0), or even every submission at all when negative
     * (depth 0 > negative limit). The application would boot fine and fail
     * silently in production, which is the worst failure shape.
     * */
    @PostConstruct
    public void validateChainDepthConfig(){
        if(maxChainDepth < 1){
            throw new IllegalStateException("[INVALID_CONFIG] hercules.task.max-chain-depth must be >= 1, got ["+maxChainDepth+"]");
        }
    }
}
