package team.magic.flute.hercules.manager.service;

import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.manager.vo.CronJobDefineRequestVO;
import team.magic.flute.hercules.manager.vo.HerculesCronJobVO;

import java.util.List;

/**
 * Hercules Cron Job Manager Service Interface
 *
 * <p>This service interface defines operations for managing scheduled (cron) jobs in Hercules.
 * It provides functionality for creating, updating, controlling, and monitoring cron jobs
 * that execute tasks on a scheduled basis.
 *
 * <p>Cron jobs use standard cron expressions to define their execution schedule and can be
 * dynamically enabled, disabled, or deleted through this service.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public interface HerculesCronJobManagerService {

    /**
     * Create a new cron job or update an existing one.
     *
     * <p>If a cron job with the specified ID already exists, it will be updated with the new
     * configuration. Otherwise, a new cron job will be created. The job definition includes
     * the cron expression, plugin information, and execution context.
     *
     * @param requestVO the cron job definition containing schedule, plugin, and context information
     * @return BaseResponse containing the created or updated cron job information
     */
    BaseResponse<HerculesCronJobVO> createOrUpdateCronJob(CronJobDefineRequestVO requestVO);

    /**
     * Enable a cron job for scheduled execution.
     *
     * <p>Activates the specified cron job so that it will be scheduled for execution
     * according to its cron expression. Enabled jobs will generate tasks at the
     * scheduled intervals.
     *
     * @param id the unique identifier of the cron job to enable
     * @return BaseResponse indicating success or failure of the enable operation
     */
    BaseResponse<Boolean> enableCronJob(String id);

    /**
     * Disable a cron job to prevent scheduled execution.
     *
     * <p>Deactivates the specified cron job so that it will not generate new tasks.
     * The job definition remains in the system but will not be scheduled for execution
     * until it is re-enabled.
     *
     * @param id the unique identifier of the cron job to disable
     * @return BaseResponse indicating success or failure of the disable operation
     */
    BaseResponse<Boolean> disableCronJob(String id);

    /**
     * Permanently delete a cron job.
     *
     * <p>Removes the specified cron job from the system completely. This operation
     * cannot be undone. The job will no longer be scheduled and its definition
     * will be permanently removed.
     *
     * @param id the unique identifier of the cron job to delete
     * @return BaseResponse indicating success or failure of the delete operation
     */
    BaseResponse<Boolean> deleteCronJob(String id);

    /**
     * Retrieve the most recent task executions for a cron job.
     *
     * <p>Returns a list of the most recent tasks that were generated and executed
     * by the specified cron job. This is useful for monitoring the execution
     * history and status of scheduled tasks.
     *
     * @param id the unique identifier of the cron job
     * @param topN the maximum number of recent tasks to retrieve
     * @return BaseResponse containing a list of recent task executions
     */
    BaseResponse<List<HerculesRunnableTaskInfo>> showTopNCronTask(String id, int topN);
}
