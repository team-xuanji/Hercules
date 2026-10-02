package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import team.magic.flute.hercules.manager.dao.mapper.HerculesExecutorTaskMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesTaskInfo;
import team.magic.flute.hercules.manager.service.HerculesExecutorTasksService;
import org.springframework.stereotype.Service;

@Service
public class HerculesExecutorTasksServiceImpl extends ServiceImpl<HerculesExecutorTaskMapper, HerculesTaskInfo> implements HerculesExecutorTasksService {
}
