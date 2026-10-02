package team.magic.flute.hercules.plugin.demo;

import com.google.auto.service.AutoService;
import java.sql.Connection;
import java.sql.Statement;
import team.magic.flute.hercules.common.plugin.TaskPlugin;
import team.magic.flute.hercules.common.status.TaskExecutionContext;

@AutoService(TaskPlugin.class)
public class TestPlugin implements TaskPlugin {
    @Override
    public String getName() {
        return "TEST";
    }

    @Override
    public void execute(TaskExecutionContext context) throws Exception {
        Connection connection = context.getDuckdbConnection();
        try (Statement statement = connection.createStatement()) {
            statement.execute("INSTALL MYSQL");
        }
        System.out.println("Go, Pikachu!");
    }
}
