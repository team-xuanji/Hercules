package team.magic.flute.hercules.common.util;

import lombok.experimental.UtilityClass;
import org.apache.fory.Fory;
import org.apache.fory.ThreadLocalFory;
import org.apache.fory.ThreadSafeFory;
import org.apache.fory.config.Language;
import team.magic.flute.hercules.common.executor.ExecutorCurrentLoadPluginInfo;
import team.magic.flute.hercules.common.executor.HerculesExecutorHeartbeatInfo;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.common.http.PluginDesc;
import team.magic.flute.hercules.common.http.TaskFetchResult;
import team.magic.flute.hercules.common.status.TaskType;

import java.util.*;

@UtilityClass
public class ForyUtils {
    private static final ThreadSafeFory fory;
    static {
        fory = new ThreadLocalFory(classLoader -> {
            Fory f = Fory.builder().withLanguage(Language.JAVA)
                    .withNumberCompressed(true)
                    .withStringCompressed(true)
                    .withAsyncCompilation(true)
                    // Allow to deserialize objects unknown types,
                    // more flexible but less secure.
                    // .requireClassRegistration(false)
                    .build();
            // Registering types can reduce class name serialization overhead, but not mandatory.
            // If secure mode enabled, all custom types must be registered.
            f.register(HerculesRunnableTaskInfo.class);
            f.register(HerculesExecutorHeartbeatInfo.class);
            f.register(HerculesExecutorHeartbeatInfo.class);
            f.register(ExecutorCurrentLoadPluginInfo.class);
            f.register(TaskFetchResult.class);
            f.register(PluginDesc.class);
            f.register(List.class);
            f.register(ArrayList.class);
            f.register(Collection.class);
            f.register(Set.class);
            f.register(HashSet.class);
            f.register(Map.class);
            f.register(HashMap.class);
            f.register(TaskType.class);
            return f;
        });
    }

    public static void register(Class<?> clazz){
        fory.register(clazz);
    }

    public static byte[] serialize(Object data){
        return fory.serialize(data);
    }

    public static ThreadSafeFory getFory() {
        return fory;
    }

    public static <T> T deserialize(byte[] data, Class<T> clazz){
        return fory.deserialize(data,clazz);
    }
}
