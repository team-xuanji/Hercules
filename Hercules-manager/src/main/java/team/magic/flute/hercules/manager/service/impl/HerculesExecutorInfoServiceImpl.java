package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.common.executor.ExecutorProcessHandleWhiteList;
import team.magic.flute.hercules.common.util.Tuple2;
import team.magic.flute.hercules.manager.dao.mapper.HerculesExecutorInfoMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesExecutorInfo;
import team.magic.flute.hercules.manager.service.HerculesExecutorInfoService;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static team.magic.flute.hercules.common.global.Constant.TASK_MAX_BUCKET_SIZE;

@Service
@Slf4j
public class HerculesExecutorInfoServiceImpl extends ServiceImpl<HerculesExecutorInfoMapper, HerculesExecutorInfo> implements HerculesExecutorInfoService {

    private static final String CONFLICT_SENTINEL = "__CONFIG_CONFLICT_EMPTY__";
    private Cache<String, List<HerculesExecutorInfo>> allExecutorInfoCache;
    private Map<String,Set<String>> pluginHandleWhiteListCache;
    private final static String CACHE_KEY = "hercules_executor_info";
    @PostConstruct
    private void init(){
        allExecutorInfoCache = Caffeine.newBuilder()
                .maximumSize(10)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .build();
        pluginHandleWhiteListCache = new HashMap<>();
    }

    public List<HerculesExecutorInfo> getAllExecutorInfo(){
        if(allExecutorInfoCache.getIfPresent(CACHE_KEY)==null){
            synchronized (this){
                if(allExecutorInfoCache.getIfPresent(CACHE_KEY)==null){
                    List<HerculesExecutorInfo> allExecutorInfo = list();
                    if(allExecutorInfo!=null && !allExecutorInfo.isEmpty()){
                        allExecutorInfoCache.put(CACHE_KEY,allExecutorInfo);
                        Map<String,Set<String>> newCacheInfo = new HashMap<>();
                        Map<String, Boolean> regionHasNull = new HashMap<>();

                        allExecutorInfo.forEach(exec -> {
                            String region = exec.getExecutorRegion();
                            String executorId = exec.getExecutorId();
                            Set<String> whiteList = Optional.ofNullable(exec.getExecutorPluginHandleWhiteList())
                                    .map(ExecutorProcessHandleWhiteList::getWhiteList)
                                    .filter(CollectionUtils::isNotEmpty)
                                    .map(HashSet::new)
                                    .orElse(null);
                            if (whiteList == null) {
                                if (newCacheInfo.containsKey(region)) {
                                    log.error("Region {} mixed at executor {}: configured exists but current unconfigured",
                                            region, executorId);
                                    newCacheInfo.put(region, Stream.of(CONFLICT_SENTINEL).collect(Collectors.toSet()));
                                } else {
                                    regionHasNull.put(region, true);
                                }
                                return;
                            }

                            if (newCacheInfo.containsKey(region)) {
                                newCacheInfo.merge(region, whiteList, (existing, incoming) -> {
                                    existing.retainAll(incoming);
                                    if (existing.isEmpty()) {
                                        log.error("Region {} whitelist conflict at executor {}", region, executorId);
                                        existing.add(CONFLICT_SENTINEL);
                                    }
                                    return existing;
                                });
                            } else {
                                if (regionHasNull.getOrDefault(region, false)) {
                                    log.error("Region {} mixed at executor {}: unconfigured existed but current configured",
                                            region, executorId);
                                    newCacheInfo.put(region, Stream.of(CONFLICT_SENTINEL).collect(Collectors.toSet()));
                                } else {
                                    newCacheInfo.put(region, new HashSet<>(whiteList));
                                }
                            }
                        });
                        pluginHandleWhiteListCache = newCacheInfo;
                    }
                }
            }
        }
        return new ArrayList<>(Optional.ofNullable(allExecutorInfoCache.getIfPresent(CACHE_KEY))
                .orElse(new ArrayList<>()));
    }

    @Override
    public Tuple2<Integer,Integer> getConsumeRange(String executorId, String executorRegion) {
        List<HerculesExecutorInfo> allExecutorInfo = getAllExecutorInfo();
        Set<String> runningExecutorIds = allExecutorInfo.stream()
                .filter(executorInfo -> executorInfo.getExecutorRegion().equals(executorRegion))
                .map(HerculesExecutorInfo::getExecutorId)
                .collect(Collectors.toSet());
        if(allExecutorInfo.isEmpty() || !runningExecutorIds.contains(executorId)){
            // Actually, the reason why a valid interval range is returned here is that the current
            // executor still needs to lock the acquired tasks. Locking itself prevents the issue
            // of a task being repeatedly executed by multiple executors, so this approach works in practice.
            // However, this method actually violates some theoretical correctness and can only be
            // considered an engineering trade-off.
            // From a correctness perspective, this should return an empty interval or a negative interval.
            if(allExecutorInfo.isEmpty()){
                log.warn("No executor reporting information was obtained; using default configuration.");
            }else{
                log.warn("Executor [{}] has not reported information, using default configuration.",executorId);
            }

            return new Tuple2<>(1,TASK_MAX_BUCKET_SIZE+1);
        }
        List<String> sortedExecutorIds = runningExecutorIds.stream().sorted()
                .collect(Collectors.toList());
        int index = sortedExecutorIds.indexOf(executorId);
        int subBucketSize = (TASK_MAX_BUCKET_SIZE +sortedExecutorIds.size()-1) / sortedExecutorIds.size();
        int begin = (index * subBucketSize) + 1;
        int end = Math.min(((index + 1) * subBucketSize) + 1, TASK_MAX_BUCKET_SIZE+1);
        return new Tuple2<>(begin,end);
    }

    @Override
    public Collection<String> getAllAvailableExecutorRegion() {
        return getAllExecutorInfo().stream()
                .map(HerculesExecutorInfo::getExecutorRegion)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public Collection<String> getPluginHandleWhiteListByExecutorRegion(String executorRegion) {
        if(pluginHandleWhiteListCache == null || pluginHandleWhiteListCache.isEmpty()){
            getAllExecutorInfo();
        }
        return pluginHandleWhiteListCache.getOrDefault(executorRegion,new HashSet<>());
    }
}
