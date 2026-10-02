package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import team.magic.flute.hercules.manager.dao.mapper.HerculesManagerInstanceMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesManagerRunnerInstancePo;
import team.magic.flute.hercules.manager.service.HerculesManagerInstanceService;
import org.springframework.stereotype.Service;

@Service
public class HerculesManagerInstanceServiceImpl extends ServiceImpl<HerculesManagerInstanceMapper, HerculesManagerRunnerInstancePo> implements HerculesManagerInstanceService {
}
