package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.manager.dao.mapper.HerculesExecutorTaskMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.service.HerculesExecutorTasksService;

@Service
public class HerculesExecutorTasksServiceImpl
        extends ServiceImpl<HerculesExecutorTaskMapper, HerculesTaskInfo>
        implements HerculesExecutorTasksService {}
