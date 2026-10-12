package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.manager.dao.mapper.HerculesManagerInstanceMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesManagerRunnerInstancePo;
import team.magic.flute.hercules.manager.service.HerculesManagerInstanceService;

@Service
public class HerculesManagerInstanceServiceImpl
        extends ServiceImpl<HerculesManagerInstanceMapper, HerculesManagerRunnerInstancePo>
        implements HerculesManagerInstanceService {}
