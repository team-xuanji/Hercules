package team.magic.flute.hercules.manager.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import team.magic.flute.hercules.manager.dao.mapper.HerculesCronJobsMapper;
import team.magic.flute.hercules.manager.dao.po.HerculesCronJobs;
import team.magic.flute.hercules.manager.service.HerculesConJobsService;
import org.springframework.stereotype.Service;

@Service
public class HerculesCronJobsServiceImpl extends ServiceImpl<HerculesCronJobsMapper, HerculesCronJobs> implements HerculesConJobsService {
}
