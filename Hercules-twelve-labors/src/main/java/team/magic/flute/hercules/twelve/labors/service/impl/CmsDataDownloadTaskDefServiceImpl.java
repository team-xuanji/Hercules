package team.magic.flute.hercules.twelve.labors.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskDefPO;
import team.magic.flute.hercules.twelve.labors.dao.mapper.CmsDataDownloadTaskDefMapper;
import team.magic.flute.hercules.twelve.labors.service.CmsDataDownloadTaskDefService;

/**
 * CMS Data Download Task Definition Service Implementation
 *
 * @author Hercules
 */
@Service
public class CmsDataDownloadTaskDefServiceImpl
        extends ServiceImpl<CmsDataDownloadTaskDefMapper, CmsDataDownloadTaskDefPO>
        implements CmsDataDownloadTaskDefService {}
