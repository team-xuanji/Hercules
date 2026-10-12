package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.manager.dao.mapper.HerculesCronJobsMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesCronJobs;
import team.magic.flute.hercules.manager.service.HerculesConJobsService;

@Service
public class HerculesCronJobsServiceImpl
        extends ServiceImpl<HerculesCronJobsMapper, HerculesCronJobs>
        implements HerculesConJobsService {}
