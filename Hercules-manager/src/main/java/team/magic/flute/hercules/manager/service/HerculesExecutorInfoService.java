package team.magic.flute.hercules.manager.service;

import com.baomidou.mybatisplus.extension.service.IService;
import team.magic.flute.hercules.common.util.Tuple2;
import team.magic.flute.hercules.manager.dao.po.HerculesExecutorInfo;

import java.util.Collection;
import java.util.List;

public interface HerculesExecutorInfoService extends IService<HerculesExecutorInfo> {
    Collection<String> getAllAvailableExecutorRegion();
    Collection<String> getPluginHandleWhiteListByExecutorRegion(String executorRegion);
    List<HerculesExecutorInfo> getAllExecutorInfo();
    Tuple2<Integer,Integer> getConsumeRange(String executorId, String executorRegion);
}
