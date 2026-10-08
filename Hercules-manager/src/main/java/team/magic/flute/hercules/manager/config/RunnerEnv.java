package team.magic.flute.hercules.manager.config;

import com.github.f4b6a3.uuid.alt.GUID;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;


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
}
