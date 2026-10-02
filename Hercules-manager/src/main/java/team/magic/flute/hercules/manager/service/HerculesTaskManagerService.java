package team.magic.flute.hercules.manager.service;

import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.manager.vo.AsyncRetryOneTaskRequestVO;
import team.magic.flute.hercules.manager.vo.HerculesRecoverTaskInfoVO;
import team.magic.flute.hercules.manager.vo.SubmitOnceTypeTaskRequestVO;

/**
 * Hercules Task Manager Service Interface
 *
 * <p>This service interface defines the core task management operations in Hercules,
 * providing methods for task submission, monitoring, and retry operations.
 *
 * <p>The service handles the lifecycle of tasks from submission to completion,
 * including error handling and retry mechanisms for failed tasks.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public interface HerculesTaskManagerService {

    /**
     * Submit a one-time task for execution.
     *
     * <p>Creates a new task in the system and queues it for execution by available executors.
     * The task will be assigned a unique identifier and tracked throughout its lifecycle.
     *
     * @param submitTaskRequestVO the task submission request containing all necessary task details
     * @return BaseResponse containing the created task information with assigned ID and initial status
     */
    BaseResponse<HerculesRunnableTaskInfo> submitOnceTask(SubmitOnceTypeTaskRequestVO submitTaskRequestVO);

    /**
     * Retrieve detailed information about a specific task.
     *
     * <p>Fetches the current state, status, and metadata of a task by its unique identifier.
     * This includes execution status, timestamps, error information, and context data.
     *
     * @param taskId the unique identifier of the task to retrieve
     * @return BaseResponse containing the complete task information, or error if task not found
     */
    BaseResponse<HerculesRunnableTaskInfo> getTaskInfo(String taskId);

    /**
     * Rerun a previously executed task.
     *
     * <p>Resubmits a task for execution, typically used for failed tasks that need to be retried
     * or completed tasks that need to be executed again. The task will be reset to initial state
     * and queued for execution.
     *
     * @param taskId the unique identifier of the task to rerun
     * @return BaseResponse containing the updated task information after resubmission
     */
    BaseResponse<HerculesRunnableTaskInfo> rerunTask(String taskId);

    /**
     * Asynchronously retry a failed task with custom parameters.
     *
     * <p>Provides a mechanism to retry failed tasks asynchronously with additional context
     * or modified parameters. This method is typically used for complex retry scenarios
     * that require custom error handling or parameter adjustment.
     *
     * @param requestVO the retry request containing task ID, error information, and retry parameters
     * @return BaseResponse containing the failed task information after the retry attempt
     */
    BaseResponse<HerculesRecoverTaskInfoVO> asyncRetryOneTask(AsyncRetryOneTaskRequestVO requestVO);
}
