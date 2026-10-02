package team.magic.flute.hercules.twelve.labors.service;

import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskPO;
import team.magic.flute.hercules.twelve.labors.vo.SubmitTaskRequestVO;
import team.magic.flute.hercules.twelve.labors.vo.TaskSubmissionAndDownloadResultVO;
import team.magic.flute.hercules.twelve.labors.vo.TaskStatusVO;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Task Submission and Download Service for Business Access Gateway
 *
 * <p>Service providing integrated task submission, completion waiting, and download link
 * retrieval functionality within the Hercules business access gateway.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
public class TaskSubmissionAndDownloadService {

    @Autowired
    private TaskSubmissionService taskSubmissionService;
    
    @Autowired
    private CmsDataDownloadTaskService taskService;
    
    @Autowired
    private FileDownloadService fileDownloadService;


    /**
     * Task completion status list
     */
    private static final List<TaskStatus> COMPLETED_STATUS = Arrays.asList(TaskStatus.SUCCESS, TaskStatus.FAILED, TaskStatus.CANCELLED);

    /**
     * Default polling interval (seconds)
     */
    private static final int DEFAULT_POLL_INTERVAL_SECONDS = 5;

    /**
     * Default maximum wait time (seconds)
     */
    private static final int DEFAULT_MAX_WAIT_SECONDS = 300; // 5 minutes

    /**
     * Submit task and wait for completion, then get download link (unified permission control)
     * Supports smart permission control: if appKey and userId are provided, permission verification is performed, otherwise no permission control
     *
     * @param request task submission request
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @return download link information
     */
    public TaskSubmissionAndDownloadResultVO submitTaskAndGetDownloadUrl(SubmitTaskRequestVO request, String appKey, String userId) {
        return submitTaskAndGetDownloadUrl(request, appKey, userId,request.getUserName(), DEFAULT_MAX_WAIT_SECONDS, DEFAULT_POLL_INTERVAL_SECONDS);
    }

    /**
     * Submit task and wait for completion, then get download link (unified permission control, custom wait time)
     * Supports smart permission control: if appKey and userId are provided, permission verification is performed, otherwise no permission control
     *
     * @param request task submission request
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @param maxWaitSeconds maximum wait time (seconds)
     * @param pollIntervalSeconds polling interval (seconds)
     * @return download link information
     */
    public TaskSubmissionAndDownloadResultVO submitTaskAndGetDownloadUrl(SubmitTaskRequestVO request, String appKey, String userId,
                                                                        String userName,int maxWaitSeconds, int pollIntervalSeconds) {
        // 1. Submit task
        long startTime = System.currentTimeMillis();
        String authInfo = (StringUtils.hasText(appKey) && StringUtils.hasText(userId)) ? "with permission control" : "without permission control";

        log.info("Starting task submission and waiting for completion ({}): businessKey={}, appKey={}, userId={}",
                authInfo, request.getBusinessKey(), appKey, userId);

        CmsDataDownloadTaskPO task = taskSubmissionService.submitTask(request, appKey, userId, request.getOperatorSource(),userName);
        Long taskId = task.getTaskId();

        log.info("Task submitted successfully ({}): taskId={}, businessKey={}", authInfo, taskId, request.getBusinessKey());

        // 2. Poll and wait for task completion
        TaskCompletionResult completionResult = waitForTaskCompletion(taskId, maxWaitSeconds, pollIntervalSeconds);
        CmsDataDownloadTaskPO completedTask = completionResult.getTask();

        // 3. Check task status
        TaskStatus taskStatus = TaskStatus.valueOf(completedTask.getTaskStatus());
        if (taskStatus != TaskStatus.SUCCESS) {
            throw new RuntimeException("Task execution failed: taskId=" + taskId + ", status=" + taskStatus);
        }

        // 4. Get download URL (smart permission control)
        log.info("Task executed successfully, starting to get download URL ({}): taskId={}", authInfo, taskId);
        String downloadUrl = fileDownloadService.getFileDownloadUrl(taskId, appKey, userId);

        // 5. Build return result
        long executionTimeSeconds = (System.currentTimeMillis() - startTime) / 1000;
        boolean authRequired = StringUtils.hasText(appKey) && StringUtils.hasText(userId);
        TaskSubmissionAndDownloadResultVO result = new TaskSubmissionAndDownloadResultVO()
                .setTaskId(taskId)
                .setBusinessKey(request.getBusinessKey())
                .setTaskStatus(taskStatus)
                .setDownloadUrl(downloadUrl)
                .setExpirationTime("1 hour")
                .setGenerateTime(java.time.LocalDateTime.now())
                .setAuthRequired(authRequired)
                .setFilePath(completedTask.getFilePath())
                .setOperatorSource(completedTask.getOperatorSource())
                .setExecutionTimeSeconds(executionTimeSeconds)
                .setPollCount(completionResult.getPollCount())
                .setTaskCreateTime(completedTask.getInsertTime())
                .setTaskCompleteTime(completedTask.getUpdateTime())
                .setRemarks("Task submission and download URL retrieval successful (" + authInfo + ")");

        log.info("Task submission and download URL retrieval completed ({}): taskId={}, downloadUrl={}, executionTimeSeconds={}",
                authInfo, taskId, downloadUrl, executionTimeSeconds);
        return result;
    }

    /**
     * Poll and wait for task completion
     *
     * @param taskId task ID
     * @param maxWaitSeconds maximum wait time (seconds)
     * @param pollIntervalSeconds polling interval (seconds)
     * @return completed task information and polling statistics
     */
    private TaskCompletionResult waitForTaskCompletion(Long taskId, int maxWaitSeconds, int pollIntervalSeconds) {
        long startTime = System.currentTimeMillis();
        long maxWaitMillis = maxWaitSeconds * 1000L;
        int pollCount = 0;

        log.info("Starting polling to wait for task completion: taskId={}, maxWaitSeconds={}, pollIntervalSeconds={}",
                taskId, maxWaitSeconds, pollIntervalSeconds);

        while (System.currentTimeMillis() - startTime < maxWaitMillis) {
            try {
                pollCount++;

                // Query task status
                CmsDataDownloadTaskPO task = taskService.getById(taskId);
                if (task == null) {
                    throw new RuntimeException("Task does not exist: " + taskId);
                }

                TaskStatus status = TaskStatus.valueOf(task.getTaskStatus());
                log.debug("Polling task status: taskId={}, status={}, pollCount={}", taskId, status, pollCount);

                // Check if completed
                if (COMPLETED_STATUS.contains(status)) {
                    long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
                    log.info("Task completed: taskId={}, status={}, elapsedSeconds={}, pollCount={}",
                            taskId, status, elapsedSeconds, pollCount);
                    return new TaskCompletionResult(task, pollCount);
                }

                // Wait for next poll
                TimeUnit.SECONDS.sleep(pollIntervalSeconds);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for task completion: " + taskId, e);
            } catch (Exception e) {
                log.warn("Exception occurred while polling task status: taskId={}, error={}", taskId, e.getMessage());
                throw new RuntimeException("Failed to poll task status: " + taskId, e);
            }
        }

        // Timeout
        throw new RuntimeException("Timeout waiting for task completion: taskId=" + taskId + ", maxWaitSeconds=" + maxWaitSeconds + ", pollCount=" + pollCount);
    }

    /**
     * Get current task status
     *
     * @param taskId task ID
     * @return task status information
     */
    public TaskStatusVO getTaskStatus(Long taskId) {
        CmsDataDownloadTaskPO task = taskService.getById(taskId);
        if (task == null) {
            throw new RuntimeException("Task does not exist: " + taskId);
        }

        TaskStatus taskStatus = TaskStatus.valueOf(task.getTaskStatus());

        return new TaskStatusVO()
                .setTaskId(taskId)
                .setBusinessKey(task.getBusinessKey())
                .setTaskStatus(taskStatus)
                .setFilePath(task.getFilePath())
                .setInsertTime(task.getInsertTime())
                .setUpdateTime(task.getUpdateTime())
                .setIsCompleted(COMPLETED_STATUS.contains(taskStatus))
                .setOperatorSource(task.getOperatorSource())
                .setAppKey(task.getAppKey())
                .setUserId(task.getUserId())
                .setUserName(task.getUserName())
                .setProgressDescription(getProgressDescription(taskStatus))
                .setErrorMessage(taskStatus == TaskStatus.FAILED ? "Task execution failed, please check detailed logs" : null);
    }

    /**
     * Get progress description based on task status
     *
     * @param taskStatus task status
     * @return progress description
     */
    private String getProgressDescription(TaskStatus taskStatus) {
        switch (taskStatus) {
            case INIT:
                return "Task created, waiting for execution";
            case RUNNING:
                return "Task is executing";
            case SUCCESS:
                return "Task executed successfully";
            case FAILED:
                return "Task execution failed";
            case CANCELLED:
                return "Task cancelled";
            default:
                return "Unknown status";
        }
    }

    /**
     * Task completion result inner class
     */
    @Getter
    private static class TaskCompletionResult {
        private final CmsDataDownloadTaskPO task;
        private final int pollCount;

        public TaskCompletionResult(CmsDataDownloadTaskPO task, int pollCount) {
            this.task = task;
            this.pollCount = pollCount;
        }

    }
}
