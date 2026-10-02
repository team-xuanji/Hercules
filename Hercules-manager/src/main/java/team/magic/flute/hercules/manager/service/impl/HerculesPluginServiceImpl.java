package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import team.magic.flute.hercules.manager.dao.mapper.HerculesPluginInfoMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesPluginInfoPo;
import team.magic.flute.hercules.manager.service.HerculesPluginService;
import org.springframework.stereotype.Service;

@Service
public class HerculesPluginServiceImpl extends ServiceImpl<HerculesPluginInfoMapper, HerculesPluginInfoPo> implements HerculesPluginService {
}
