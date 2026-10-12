package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.manager.dao.mapper.HerculesPluginInfoMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesPluginInfoPo;
import team.magic.flute.hercules.manager.service.HerculesPluginService;

@Service
public class HerculesPluginServiceImpl
        extends ServiceImpl<HerculesPluginInfoMapper, HerculesPluginInfoPo>
        implements HerculesPluginService {}
