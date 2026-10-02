package team.magic.flute.hercules.twelve.labors.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.twelve.labors.api.HerculesManagerApi;
import team.magic.flute.hercules.twelve.labors.business.inner.HerculesTaskInfo;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Task Status Synchronization Service
 *
 * <p>This service is responsible for maintaining synchronization between the local CMS task
 * database and the remote Hercules Manager system. It provides automated status updates,
 * cleanup operations, and monitoring capabilities for data export tasks.
 *
 * <p>Key responsibilities include:
 * <ul>
 *   <li>Periodic synchronization of task status from Hercules Manager</li>
 *   <li>Automated cleanup of stale and abnormal tasks</li>
 *   <li>Statistical reporting and monitoring of task execution</li>
 *   <li>Error handling and recovery for failed synchronization attempts</li>
 * </ul>
 *
 * <p>The service operates on scheduled intervals to ensure data consistency and system
 * health. It uses asynchronous processing to avoid blocking the main application threads
 * and implements proper locking mechanisms to prevent concurrent execution conflicts.
 *
 * <p>Scheduling configuration:
 * <ul>
 *   <li>Status sync: Every 10 seconds (configurable)</li>
 *   <li>Cleanup tasks: Every hour (configurable)</li>
 *   <li>Batch size: 10 tasks per sync cycle (configurable)</li>
 * </ul>
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
@EnableAsync
public class TaskStatusSyncService {
    
    @Autowired
    private CmsDataDownloadTaskService taskService;
    
    @Autowired
    private HerculesManagerApi herculesManagerApi;
    private final Lock taskScheduleLock = new ReentrantLock();
    
    private final Random random = new Random();
    
    /**
     * Scheduled task status synchronization.
     *
     * <p>This method runs every 10 seconds to synchronize task status between the local
     * CMS database and the remote Hercules Manager system. It randomly selects up to 10
     * tasks that are in non-final states (INIT, RUNNING) and updates their status based
     * on the latest information from Hercules Manager.
     *
     * <p>The synchronization process:
     * <ol>
     *   <li>Query local tasks in INIT or RUNNING state</li>
     *   <li>Randomly select up to 10 tasks to avoid overwhelming the system</li>
     *   <li>Query Hercules Manager for current status of each task</li>
     *   <li>Update local database with the latest status information</li>
     *   <li>Handle any errors gracefully without affecting other tasks</li>
     * </ol>
     *
     * <p>This method is executed asynchronously to prevent blocking the main application
     * thread and uses a lock to ensure only one synchronization process runs at a time.
     *
     * @see #syncSingleTaskStatus(CmsDataDownloadTaskPO)
     */
    @Scheduled(fixedRate = 10000)
    @Async// 10 seconds
    public void syncTaskStatus() {
        try{
            if(taskScheduleLock.tryLock()){
                log.debug("Starting task status synchronization");

                // Query tasks that need status synchronization (non-final state tasks)
                List<CmsDataDownloadTaskPO> tasksToSync = getTasksToSync();

                if (tasksToSync.isEmpty()) {
                    log.debug("No tasks need status synchronization");
                    return;
                }

                log.info("Found {} tasks that need status synchronization", tasksToSync.size());

                // Randomly select up to 10 tasks for synchronization
                List<CmsDataDownloadTaskPO> selectedTasks = selectRandomTasks(tasksToSync, 10);

                // Synchronize status for each task
                for (CmsDataDownloadTaskPO task : selectedTasks) {
                    syncSingleTaskStatus(task);
                }

                log.debug("Task status synchronization completed");
            }
        }catch (Exception e){
            log.error("Failed to synchronize task status!");
            log.error(e.getMessage(),e);
        }finally {
            taskScheduleLock.unlock();
        }
    }
    
    /**
     * Get tasks that need status synchronization
     * Query non-terminal tasks (INIT, RUNNING, PENDING, etc.)
     *
     * @return list of tasks to synchronize
     */
    private List<CmsDataDownloadTaskPO> getTasksToSync() {
        LambdaQueryWrapper<CmsDataDownloadTaskPO> queryWrapper = new LambdaQueryWrapper<>();

        // Query non-terminal tasks
        queryWrapper.notIn(CmsDataDownloadTaskPO::getTaskStatus, TaskStatus.SUCCESS.name(),
                        TaskStatus.FAILED.name(),
                        TaskStatus.CANCELLED.name())
                   .orderByDesc(CmsDataDownloadTaskPO::getUpdateTime);

        // Limit query count to avoid querying too much data at once
        Page<CmsDataDownloadTaskPO> page = new Page<>(1, 100);

        return taskService.page(page, queryWrapper).getRecords();
    }
    
    /**
     * Randomly select specified number of tasks
     *
     * @param tasks task list
     * @param maxCount maximum selection count
     * @return randomly selected task list
     */
    private List<CmsDataDownloadTaskPO> selectRandomTasks(List<CmsDataDownloadTaskPO> tasks, int maxCount) {
        if (tasks.size() <= maxCount) {
            return tasks;
        }

        // Randomly shuffle the list
        Collections.shuffle(tasks, random);

        // Return first maxCount tasks
        return tasks.subList(0, maxCount);
    }
    
    /**
     * Synchronize single task status
     *
     * @param task task object
     */
    private void syncSingleTaskStatus(CmsDataDownloadTaskPO task) {
        try {
            String taskId = String.valueOf(task.getTaskId());
            log.debug("Synchronizing task status: taskId={}, businessKey={}", taskId, task.getBusinessKey());

            // Call HerculesManagerApi to query task status
            BaseResponse<HerculesTaskInfo> response = herculesManagerApi.checkTaskStatus(taskId);

            if (response.getCode() == 200 && response.getData() != null) {
                HerculesTaskInfo herculesTaskInfo = response.getData();
                String newStatus = herculesTaskInfo.getStatus();
                String oldStatus = task.getTaskStatus();

                // If status changed, update database
                if (!newStatus.equals(oldStatus)) {
                    log.info("Task status changed: taskId={}, businessKey={}, {} -> {}",
                            taskId, task.getBusinessKey(), oldStatus, newStatus);

                    task.setTaskStatus(newStatus);
                    task.setUpdateTime(LocalDateTime.now());

                    boolean updateResult = taskService.updateById(task);
                    if (updateResult) {
                        log.info("Task status update successful: taskId={}, newStatus={}", taskId, newStatus);
                    } else {
                        log.warn("Task status update failed: taskId={}, newStatus={}", taskId, newStatus);
                    }
                } else {
                    log.debug("Task status unchanged: taskId={}, status={}", taskId, newStatus);
                }
                
            } else {
                log.warn("Failed to query task status: taskId={}, response={}", taskId, response.getMsg());
            }
            
        } catch (Exception e) {
            log.error("Exception occurred while synchronizing task status: taskId={}, businessKey={}",
                    task.getTaskId(), task.getBusinessKey(), e);
        }
    }
    
    /**
     * Manually trigger task status synchronization (for testing or emergency situations)
     *
     * @param taskId task ID
     * @return whether synchronization was successful
     */
    public boolean syncTaskStatusManually(Long taskId) {
        try {
            CmsDataDownloadTaskPO task = taskService.getById(taskId);
            if (task == null) {
                log.warn("Task does not exist: taskId={}", taskId);
                return false;
            }

            syncSingleTaskStatus(task);
            return true;

        } catch (Exception e) {
            log.error("Failed to manually synchronize task status: taskId={}", taskId, e);
            return false;
        }
    }

    /**
     * Scheduled cleanup of abnormal tasks
     * Executes every hour, marking RUNNING/INIT status tasks older than 3 days as CANCELLED
     */
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 milliseconds
    public void cleanupAbnormalTasks() {
        try {
            log.info("Starting cleanup of abnormal tasks");

            // Calculate time point 3 days ago
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);

            // Query abnormal tasks that need cleanup
            List<CmsDataDownloadTaskPO> abnormalTasks = getAbnormalTasks(threeDaysAgo);

            if (abnormalTasks.isEmpty()) {
                log.info("No abnormal tasks need cleanup");
                return;
            }

            log.info("Found {} abnormal tasks that need cleanup", abnormalTasks.size());

            // Batch update task status to CANCELLED
            int cleanedCount = batchUpdateTaskStatus(abnormalTasks, TaskStatus.CANCELLED.name());

            log.info("Abnormal task cleanup completed, cleaned {} tasks in total", cleanedCount);

        } catch (Exception e) {
            log.error("Exception occurred while cleaning up abnormal tasks", e);
        }
    }

    /**
     * Get abnormal task list
     * Query RUNNING/INIT status tasks older than 3 days
     *
     * @param threeDaysAgo time point 3 days ago
     * @return abnormal task list
     */
    private List<CmsDataDownloadTaskPO> getAbnormalTasks(LocalDateTime threeDaysAgo) {
        LambdaQueryWrapper<CmsDataDownloadTaskPO> queryWrapper = new LambdaQueryWrapper<>();

        // Query conditions:
        // 1. Task status is RUNNING or INIT
        // 2. Insert time is before 3 days ago
        queryWrapper.in(CmsDataDownloadTaskPO::getTaskStatus, "RUNNING", "INIT")
                   .lt(CmsDataDownloadTaskPO::getInsertTime, threeDaysAgo)
                   .orderByAsc(CmsDataDownloadTaskPO::getInsertTime);

        List<CmsDataDownloadTaskPO> abnormalTasks = taskService.list(queryWrapper);

        // Record detailed information
        if (!abnormalTasks.isEmpty()) {
            log.info("Abnormal task details:");
            for (CmsDataDownloadTaskPO task : abnormalTasks) {
                log.info("  - taskId: {}, businessKey: {}, status: {}, insertTime: {}",
                        task.getTaskId(), task.getBusinessKey(), task.getTaskStatus(), task.getInsertTime());
            }
        }

        return abnormalTasks;
    }

    /**
     * Batch update task status
     *
     * @param tasks task list
     * @param newStatus new status
     * @return number of successfully updated tasks
     */
    private int batchUpdateTaskStatus(List<CmsDataDownloadTaskPO> tasks, String newStatus) {
        int successCount = 0;

        for (CmsDataDownloadTaskPO task : tasks) {
            try {
                String oldStatus = task.getTaskStatus();
                task.setTaskStatus(newStatus);
                task.setUpdateTime(LocalDateTime.now());

                boolean updateResult = taskService.updateById(task);
                if (updateResult) {
                    successCount++;
                    log.info("Task status update successful: taskId={}, businessKey={}, {} -> {}",
                            task.getTaskId(), task.getBusinessKey(), oldStatus, newStatus);
                } else {
                    log.warn("Task status update failed: taskId={}, businessKey={}",
                            task.getTaskId(), task.getBusinessKey());
                }

            } catch (Exception e) {
                log.error("Exception occurred while updating task status: taskId={}, businessKey={}",
                        task.getTaskId(), task.getBusinessKey(), e);
            }
        }

        return successCount;
    }

    /**
     * Manually trigger abnormal task cleanup (for testing or emergency situations)
     *
     * @return number of cleaned tasks
     */
    public int cleanupAbnormalTasksManually() {
        try {
            log.info("Manually triggering abnormal task cleanup");

            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            List<CmsDataDownloadTaskPO> abnormalTasks = getAbnormalTasks(threeDaysAgo);

            if (abnormalTasks.isEmpty()) {
                log.info("No abnormal tasks need cleanup");
                return 0;
            }

            int cleanedCount = batchUpdateTaskStatus(abnormalTasks, "CANCELLED");
            log.info("Manual cleanup of abnormal tasks completed, cleaned {} tasks in total", cleanedCount);

            return cleanedCount;

        } catch (Exception e) {
            log.error("Manual cleanup of abnormal tasks failed", e);
            return 0;
        }
    }

    /**
     * Get abnormal task statistics
     *
     * @return abnormal task statistics
     */
    public java.util.Map<String, Object> getAbnormalTaskStats() {
        try {
            LocalDateTime threeDaysAgo = LocalDateTime.now().minusDays(3);
            List<CmsDataDownloadTaskPO> abnormalTasks = getAbnormalTasks(threeDaysAgo);

            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("totalCount", abnormalTasks.size());
            stats.put("threeDaysAgo", threeDaysAgo);

            // Group statistics by status
            java.util.Map<String, Long> statusCounts = abnormalTasks.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            CmsDataDownloadTaskPO::getTaskStatus,
                            java.util.stream.Collectors.counting()
                    ));
            stats.put("statusCounts", statusCounts);

            // Group statistics by business key
            java.util.Map<String, Long> businessKeyCounts = abnormalTasks.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            CmsDataDownloadTaskPO::getBusinessKey,
                            java.util.stream.Collectors.counting()
                    ));
            stats.put("businessKeyCounts", businessKeyCounts);

            return stats;

        } catch (Exception e) {
            log.error("Failed to get abnormal task statistics", e);
            return java.util.Collections.emptyMap();
        }
    }
}
