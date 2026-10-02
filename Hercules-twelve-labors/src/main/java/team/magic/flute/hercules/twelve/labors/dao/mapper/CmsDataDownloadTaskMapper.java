package team.magic.flute.hercules.twelve.labors.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskPO;
import org.apache.ibatis.annotations.Mapper;

/**
 * CMS Data Download Task Mapper Interface
 *
 * <p>This MyBatis Plus mapper interface provides data access operations for CMS data download
 * task instances within the Hercules business access gateway. It handles the persistence and
 * retrieval of individual task execution records, supporting the complete task lifecycle
 * management from submission to completion.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway's data access layer,
 * this mapper enables comprehensive task instance management for business data export operations:
 * <ul>
 *   <li>Task instance creation and status tracking</li>
 *   <li>User and application context preservation</li>
 *   <li>Export file location and metadata management</li>
 *   <li>Task execution history and audit trail maintenance</li>
 * </ul>
 *
 * <p><strong>Data Access Capabilities:</strong>
 * <ul>
 *   <li>High-performance CRUD operations through MyBatis Plus BaseMapper</li>
 *   <li>Complex query support with conditional builders and lambda expressions</li>
 *   <li>Pagination and sorting for large task datasets</li>
 *   <li>JSON object handling for complex export context storage</li>
 *   <li>Multi-tenant data access with user and application filtering</li>
 * </ul>
 *
 * <p><strong>Performance Features:</strong>
 * <ul>
 *   <li>Optimized queries for task status monitoring and reporting</li>
 *   <li>Efficient batch operations for bulk task management</li>
 *   <li>Index-optimized searches by user, application, and business key</li>
 *   <li>Automatic result mapping with Jackson type handlers</li>
 * </ul>
 *
 * <p><strong>Integration:</strong> This mapper integrates with the business access gateway's
 * service layer to provide reliable and scalable data persistence for task execution tracking,
 * supporting real-time monitoring and historical analysis of business export operations.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Mapper
public interface CmsDataDownloadTaskMapper extends BaseMapper<CmsDataDownloadTaskPO> {
}
