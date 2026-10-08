package team.magic.flute.hercules.manager.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.manager.service.HerculesTaskManagerService;
import team.magic.flute.hercules.manager.vo.AsyncRetryOneTaskRequestVO;
import team.magic.flute.hercules.manager.vo.HerculesRecoverTaskInfoVO;

import javax.validation.Valid;

/**
 * Task Management Controller
 *
 * <p>This controller provides REST APIs for managing Hercules tasks, including:
 * <ul>
 *   <li>Submitting one-time tasks</li>
 *   <li>Checking task status</li>
 *   <li>Rerunning failed tasks</li>
 *   <li>Asynchronous task retry operations</li>
 * </ul>
 *
 * <p>All endpoints return standardized {@link BaseResponse} objects for consistent API responses.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/taskManager")
public class TaskManagerController {

    @Autowired
    private HerculesTaskManagerService taskManagerService;

    /**
     * Submit a one-time task for execution.
     *
     * <p>This endpoint accepts a task definition and submits it to the Hercules execution engine.
     * The task will be queued for execution by available executors. Executor-forwarded chain
     * tasks also arrive here and carry their lineage ({@code fromType=FORWARD},
     * {@code fromSourceId=parent task id}).
     *
     * <p>Submission is idempotent on the task id: re-submitting an existing id returns the
     * already-persisted task instead of failing.
     *
     * @param submitTaskRequest the task submission request containing task details
     * @return BaseResponse containing the created task information
     */
    @PostMapping("/submitOnceTask")
    public BaseResponse<HerculesRunnableTaskInfo> submitOnceTask(@Valid @RequestBody HerculesRunnableTaskInfo submitTaskRequest){
        return taskManagerService.submitOnceTask(submitTaskRequest);
    }

    /**
     * Check the status of a specific task.
     *
     * <p>Retrieves the current status and details of a task by its unique identifier.
     *
     * @param taskId the unique identifier of the task to check
     * @return BaseResponse containing the task information and current status
     */
    @GetMapping("/checkTaskStatus")
    public BaseResponse<HerculesRunnableTaskInfo> checkTaskStatus(@RequestParam(name = "taskId") String taskId){
        return taskManagerService.getTaskInfo(taskId);
    }

    /**
     * Rerun a previously failed or completed task.
     *
     * <p>This endpoint allows resubmitting a task for execution, typically used for
     * failed tasks that need to be retried or completed tasks that need to be run again.
     *
     * @param taskId the unique identifier of the task to rerun
     * @return BaseResponse containing the updated task information
     */
    @PutMapping("/rerunTask")
    public BaseResponse<HerculesRunnableTaskInfo> rerunTask(@RequestParam(name = "taskId") String taskId){
        return taskManagerService.rerunTask(taskId);
    }

    /**
     * Asynchronously retry a failed task.
     *
     * <p>This endpoint provides a mechanism to retry failed tasks asynchronously,
     * allowing for custom error handling and retry logic.
     *
     * @param requestVO the retry request containing task ID and retry parameters
     * @return BaseResponse containing the failed task information after retry attempt
     */
    @PostMapping("/asyncRetryOneTask")
    public BaseResponse<HerculesRecoverTaskInfoVO> asyncRetryOneTask(@RequestBody AsyncRetryOneTaskRequestVO requestVO){
        return taskManagerService.asyncRetryOneTask(requestVO);
    }

}
