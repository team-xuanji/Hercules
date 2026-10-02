package team.magic.flute.hercules.twelve.labors.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskDefPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CMS Data Download Task Definition Mapper Interface
 *
 * <p>This MyBatis Plus mapper interface provides data access operations for CMS data download
 * task definitions within the Hercules business access gateway. It extends the BaseMapper
 * to inherit standard CRUD operations while supporting custom query methods for business-specific
 * data access patterns.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway's data access layer,
 * this mapper enables efficient and scalable database operations for task definition management:
 * <ul>
 *   <li>Task definition template storage and retrieval</li>
 *   <li>Business scenario configuration management</li>
 *   <li>Plugin and execution parameter persistence</li>
 *   <li>Export format and storage configuration handling</li>
 * </ul>
 *
 * <p><strong>Data Access Features:</strong>
 * <ul>
 *   <li>Automatic CRUD operations through MyBatis Plus BaseMapper</li>
 *   <li>Complex query support with lambda expressions and conditions</li>
 *   <li>Pagination and sorting capabilities for large datasets</li>
 *   <li>Transaction support for data consistency</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> This mapper integrates seamlessly with the business access
 * gateway's service layer, providing reliable data persistence for task definition templates
 * that support various business export operations and data processing workflows.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Mapper
public interface CmsDataDownloadTaskDefMapper extends BaseMapper<CmsDataDownloadTaskDefPO> {
}
