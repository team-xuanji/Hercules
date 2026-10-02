package team.magic.flute.hercules.twelve.labors.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskPO;
import team.magic.flute.hercules.twelve.labors.vo.CmsDataDownloadTaskQueryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * CMS Data Download Task Query Service
 *
 * <p>Service for querying CMS data download tasks within the Hercules business access gateway.
 * Supports user permission-based task queries with intelligent access control.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class CmsDataDownloadTaskQueryService {
    
    @Autowired
    private CmsDataDownloadTaskService taskService;
    
    /**
     * Paginated query of task list (unified permission control)
     * Supports smart permission control: if appKey and userId are provided, permission verification is performed, otherwise all tasks are queried
     *
     * @param queryVO query conditions
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @return paginated result
     */
    public IPage<CmsDataDownloadTaskPO> queryTasks(CmsDataDownloadTaskQueryVO queryVO, String appKey, String userId) {
        // Set permission control fields to queryVO
        queryVO.setAppKey(appKey);
        queryVO.setUserId(userId);

        // Create pagination object
        Page<CmsDataDownloadTaskPO> page = new Page<>(queryVO.getCurrent(), queryVO.getSize());

        // Build query conditions (smart determination of whether permission control is needed)
        LambdaQueryWrapper<CmsDataDownloadTaskPO> queryWrapper = buildQueryWrapper(queryVO);

        // Execute paginated query
        return taskService.page(page, queryWrapper);
    }

    /**
     * Query task by task ID (unified permission control)
     * Supports smart permission control: if appKey and userId are provided, permission verification is performed, otherwise all tasks are queried
     *
     * @param taskId task ID
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @return task information
     */
    public CmsDataDownloadTaskPO getTaskById(Long taskId, String appKey, String userId) {
        // Build query conditions
        LambdaQueryWrapper<CmsDataDownloadTaskPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CmsDataDownloadTaskPO::getTaskId, taskId);

        // Smart permission control: add permission conditions only when valid permission parameters are provided
        if (StringUtils.hasText(appKey)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getAppKey, appKey);
        }
        if (StringUtils.hasText(userId)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getUserId, userId);
        }

        return taskService.getOne(queryWrapper);
    }

    /**
     * Query task list by business key (unified permission control)
     * Supports smart permission control: if appKey and userId are provided, permission verification is performed, otherwise all tasks are queried
     *
     * @param businessKey business scenario key
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @return task list
     */
    public List<CmsDataDownloadTaskPO> getTasksByBusinessKey(String businessKey, String appKey, String userId) {
        // Build query conditions
        LambdaQueryWrapper<CmsDataDownloadTaskPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CmsDataDownloadTaskPO::getBusinessKey, businessKey);

        // Smart permission control: add permission conditions only when valid permission parameters are provided
        if (StringUtils.hasText(appKey)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getAppKey, appKey);
        }
        if (StringUtils.hasText(userId)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getUserId, userId);
        }

        queryWrapper.orderByDesc(CmsDataDownloadTaskPO::getInsertTime);

        return taskService.list(queryWrapper);
    }

    /**
     * Get task status statistics (unified permission control)
     * Supports intelligent permission control: if appKey and userId are provided, statistics for user tasks; otherwise, statistics for all tasks
     *
     * @param appKey application key (optional, statistics for all tasks when null)
     * @param userId user ID (optional, statistics for all tasks when null)
     * @return status statistics information
     */
    public java.util.Map<String, Long> getTaskStatusStats(String appKey, String userId) {
        // Build query conditions
        LambdaQueryWrapper<CmsDataDownloadTaskPO> queryWrapper = new LambdaQueryWrapper<>();

        // Intelligent permission control: add permission conditions only when valid permission parameters are provided
        if (StringUtils.hasText(appKey)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getAppKey, appKey);
        }
        if (StringUtils.hasText(userId)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getUserId, userId);
        }

        // Query tasks
        List<CmsDataDownloadTaskPO> tasks = taskService.list(queryWrapper);

        // Group statistics by status
        java.util.Map<String, Long> stats = new java.util.HashMap<>();
        java.util.Map<String, Long> statusCounts = tasks.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        task -> task.getTaskStatus() != null ? task.getTaskStatus() : "UNKNOWN",
                        java.util.stream.Collectors.counting()
                ));

        stats.putAll(statusCounts);
        stats.put("TOTAL", (long) tasks.size());

        return stats;
    }

    /**
     * Build query conditions (intelligent permission control)
     * Automatically selects permission control mode based on whether queryVO contains appKey and userId:
     * - If valid appKey and userId are included, permission control conditions are added
     * - If not included, no permission control conditions are added (compatibility mode)
     *
     * @param queryVO query VO
     * @return query conditions
     */
    private LambdaQueryWrapper<CmsDataDownloadTaskPO> buildQueryWrapper(CmsDataDownloadTaskQueryVO queryVO) {
        LambdaQueryWrapper<CmsDataDownloadTaskPO> queryWrapper = new LambdaQueryWrapper<>();

        // Permission control conditions (added only when valid appKey and userId are provided)
        if (StringUtils.hasText(queryVO.getAppKey())) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getAppKey, queryVO.getAppKey());
        }
        if (StringUtils.hasText(queryVO.getUserId())) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getUserId, queryVO.getUserId());
        }

        // Business query conditions (optional)
        if (queryVO.getTaskId() != null) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getTaskId, queryVO.getTaskId());
        }
        if (StringUtils.hasText(queryVO.getBusinessKey())) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getBusinessKey, queryVO.getBusinessKey());
        }
        if (StringUtils.hasText(queryVO.getTaskStatus())) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getTaskStatus, queryVO.getTaskStatus());
        }

        // Sort by insertion time in descending order
        queryWrapper.orderByDesc(CmsDataDownloadTaskPO::getInsertTime);

        return queryWrapper;
    }
}
