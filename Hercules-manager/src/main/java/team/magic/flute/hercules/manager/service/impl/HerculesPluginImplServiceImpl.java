package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.manager.dao.mapper.HerculesPluginImplInfoMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesPluginImplInfoPo;
import team.magic.flute.hercules.manager.service.HerculesPluginImplService;

@Service
public class HerculesPluginImplServiceImpl
        extends ServiceImpl<HerculesPluginImplInfoMapper, HerculesPluginImplInfoPo>
        implements HerculesPluginImplService {}
