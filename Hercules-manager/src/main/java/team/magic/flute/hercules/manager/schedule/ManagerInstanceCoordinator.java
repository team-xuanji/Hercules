package team.magic.flute.hercules.manager.schedule;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import team.magic.flute.hercules.manager.config.RunnerEnv;
import team.magic.flute.hercules.manager.dao.po.HerculesManagerRunnerInstancePo;
import team.magic.flute.hercules.manager.service.HerculesManagerInstanceService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Component
@Slf4j
@EnableAsync
public class ManagerInstanceCoordinator {
    @Autowired
    private RunnerEnv runnerEnv;
    @Autowired
    private HerculesManagerInstanceService triggerRunnerService;
    private volatile List<String> runnerIds = new ArrayList<>();
    private final Lock taskScheduleLock = new ReentrantLock();
    @Async
    @SneakyThrows(Exception.class)
    @Scheduled(fixedRate = 5000,initialDelay = 60000)
    public void removeUnUsedRunner(){
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime waterMark = now.minusMinutes(5);
        if(taskScheduleLock.tryLock()){
            try{
                /*
                 * Selecting a node for deletion may trigger concurrent
                 * multi-instance deletion during the first incomplete
                 * information synchronization, leading to database lock
                 * contention. Without this step, multi-instance deletion of
                 * table information could cause transaction conflicts.
                 * */
                if(isMaster()){
                    triggerRunnerService.remove(new LambdaQueryWrapper<HerculesManagerRunnerInstancePo>()
                            .le(HerculesManagerRunnerInstancePo::getHeartBeatTime,waterMark)
                    );
                }
            }catch (Exception e){
                log.error("Failed to clean up expired runners!");
                log.error(e.getMessage(),e);
            }finally {
                taskScheduleLock.unlock();
            }
        }
    }

    @Async
    @SneakyThrows(Exception.class)
    @Scheduled(fixedRate = 5000,initialDelay = 1000)
    public void syncRunnerInfo(){
        if(taskScheduleLock.tryLock()){
            try{
                // First delete Runner information that has lost heartbeat
                // By default, skip TriggerRunners that haven't synchronized heartbeat within 50 seconds, delete TriggerRunners that haven't updated within 2 minutes, this part is hardcoded for now, may not need configuration
                // Update first then query, to avoid unstable RunnerId acquisition issues
                log.debug("Starting to synchronize TaskRunnerInfo");
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime waterMark = now.minusSeconds(50);
                HerculesManagerRunnerInstancePo runnerInstance =new HerculesManagerRunnerInstancePo()
                        .setInstanceId(runnerEnv.getRunnerId())
                        .setHeartBeatTime(LocalDateTime.now());
                triggerRunnerService.saveOrUpdate(runnerInstance);
                runnerIds = triggerRunnerService
                        .list(new LambdaQueryWrapper<HerculesManagerRunnerInstancePo>()
                                .gt(HerculesManagerRunnerInstancePo::getUpdateTime, waterMark))
                        .stream()
                        .map(HerculesManagerRunnerInstancePo::getInstanceId)
                        .sorted()
                        .collect(Collectors.toList());
                log.debug("TaskRunnerInfo synchronization completed");
            }catch (Exception e){
                log.error("Scheduled task execution encountered an exception!");
                log.error(e.getMessage(),e);
                throw e;
            }finally {
                taskScheduleLock.unlock();
            }
        }else{
            log.debug("Failed to acquire lock, skipping execution");
        }
    }
    public String getCurrentRunnerId(){
        return runnerEnv.getRunnerId();
    }
    public List<String> getAllRunners(){
        return new ArrayList<>(runnerIds);
    }

    public boolean isMaster(){
        boolean master = getAllRunners().indexOf(getCurrentRunnerId())==0;
        if(master && log.isDebugEnabled()) {
            log.debug("Current instance is master, runners: {}", getAllRunners());
        }
        return master;
    }

}
