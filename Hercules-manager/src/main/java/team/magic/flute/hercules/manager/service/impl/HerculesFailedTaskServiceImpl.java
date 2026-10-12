package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.manager.dao.mapper.HerculesFailedTaskMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesFailedTaskPo;
import team.magic.flute.hercules.manager.service.HerculesFailedTaskService;

@Service
public class HerculesFailedTaskServiceImpl
        extends ServiceImpl<HerculesFailedTaskMapper, HerculesFailedTaskPo>
        implements HerculesFailedTaskService {}
