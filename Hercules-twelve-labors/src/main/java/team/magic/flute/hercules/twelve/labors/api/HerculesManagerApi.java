package team.magic.flute.hercules.twelve.labors.api;

import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

/**
 * Hercules Manager API Client Interface
 *
 * <p>This Feign client interface provides communication capabilities with the Hercules Manager
 * service, which acts as the central control plane for the Hercules distributed task execution
 * system. This API is a critical component of the business access gateway architecture,
 * enabling the Twelve Labors module to orchestrate and monitor business operations through
 * the underlying Hercules infrastructure.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway, this API
 * facilitates the execution of various business functions beyond just data export, including:
 * <ul>
 *   <li>Task submission and orchestration for diverse business operations</li>
 *   <li>Real-time status monitoring and progress tracking</li>
 *   <li>Integration with the distributed task execution framework</li>
 *   <li>Support for future business function expansions</li>
 * </ul>
 *
 * <p><strong>Architecture Role:</strong> This interface serves as the primary integration
 * point between the business access gateway and the Hercules execution infrastructure,
 * ensuring that business operations can be executed reliably and monitored effectively
 * across the distributed system.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public interface HerculesManagerApi {
    /**
     * Submit a one-time business task for execution
     *
     * <p>This method submits a business task to the Hercules Manager for execution within
     * the distributed task execution framework. It supports various business operations
     * including data export, document processing, and other business functions that can
     * be executed through the business access gateway.
     *
     * <p><strong>Business Operations Supported:</strong>
     * <ul>
     *   <li>Data export operations (CSV, JSON, Parquet, XLSX)</li>
     *   <li>Custom business logic execution</li>
     *   <li>Integration with external systems</li>
     *   <li>Batch processing operations</li>
     * </ul>
     *
     * @param submitTaskRequest the task submission request containing business operation details,
     *                          execution context, and configuration parameters
     * @return BaseResponse containing the created task info with task ID and initial status
     * @throws RuntimeException if task submission fails due to validation errors or system issues
     */
    @RequestLine("POST /taskManager/submitOnceTask")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<HerculesRunnableTaskInfo> submitOnceTask(
            HerculesRunnableTaskInfo submitTaskRequest
    );

    /**
     * Check the current status of a submitted business task
     *
     * <p>This method retrieves the current execution status and details of a previously
     * submitted business task. It provides real-time visibility into task progress,
     * completion status, and any error information, enabling effective monitoring of
     * business operations through the access gateway.
     *
     * <p><strong>Status Information Includes:</strong>
     * <ul>
     *   <li>Current execution status (PENDING, RUNNING, SUCCESS, FAILED)</li>
     *   <li>Task progress and completion percentage</li>
     *   <li>Error messages and diagnostic information</li>
     *   <li>Result metadata and output file locations</li>
     * </ul>
     *
     * @param taskId the unique identifier of the task to check
     * @return BaseResponse containing the current task info with updated status and details
     * @throws RuntimeException if task ID is invalid or system error occurs during status retrieval
     */
    @RequestLine("GET /taskManager/checkTaskStatus?taskId={taskId}")
    @Headers("Content-Type: application/json;charset=UTF-8")
    BaseResponse<HerculesRunnableTaskInfo> checkTaskStatus(
            @Param("taskId")String taskId
    );
}
