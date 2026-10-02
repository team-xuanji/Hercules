package team.magic.flute.hercules;

import org.junit.jupiter.api.Test;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.manager.entity.recover.RecoverStrategy;


public class Test02 {
    @Test
    public void test01(){
        String data ="{\"scheduleType\":\"FIXED_INTERVAL\"}";
        RecoverStrategy recoverStrategy = JacksonUtils.readValue(data, RecoverStrategy.class);
        System.out.println(JacksonUtils.writeValueAsString(recoverStrategy));
    }
}
