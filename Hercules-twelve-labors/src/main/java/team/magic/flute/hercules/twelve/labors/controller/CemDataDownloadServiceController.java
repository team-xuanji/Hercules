package team.magic.flute.hercules.twelve.labors.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.twelve.labors.auth.UserAccessInfo;
import team.magic.flute.hercules.twelve.labors.auth.UserLogInInfo;
import team.magic.flute.hercules.twelve.labors.constant.CmsDownloadFormat;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskDefPO;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskPO;
import team.magic.flute.hercules.twelve.labors.service.*;
import team.magic.flute.hercules.twelve.labors.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import team.magic.flute.hercules.twelve.labors.service.*;
import team.magic.flute.hercules.twelve.labors.vo.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * CEM Data Download Service Controller - Business Access Gateway Interface
 *
 * <p>This REST controller serves as a critical component of the Hercules business access gateway,
 * providing comprehensive APIs for managing data download and export operations within the
 * Content Management System (CMS). As part of the business access gateway architecture, it
 * enables various business functions beyond just data export, serving as a primary interface
 * for business users and external systems to interact with the Hercules ecosystem.
 *
 * <p><strong>Business Access Gateway Role:</strong> This controller exemplifies the business
 * access gateway pattern by providing:
 * <ul>
 *   <li>Unified business operation interface for data export and management</li>
 *   <li>Extensible architecture supporting future business functions</li>
 *   <li>Standardized API patterns for consistent business integration</li>
 *   <li>Multi-tenant support with proper isolation and security</li>
 * </ul>
 *
 * <p><strong>Core Business Functions:</strong>
 * <ul>
 *   <li>Task definition management (CRUD operations)</li>
 *   <li>Export format configuration and validation</li>
 *   <li>Task submission and execution monitoring</li>
 *   <li>File download and access control</li>
 *   <li>Task status synchronization and cleanup</li>
 *   <li>User permission and authentication handling</li>
 * </ul>
 *
 * <p><strong>Export Capabilities:</strong> The controller supports multiple export formats
 * including CSV, JSON, Parquet, and XLSX, with configurable compression options and
 * destination storage (OSS, S3). It integrates with the Hercules distributed task execution
 * system for scalable processing and provides real-time status updates and progress monitoring.
 *
 * <p><strong>Security and Access Control:</strong>
 * <ul>
 *   <li>User authentication and authorization</li>
 *   <li>App-key based access control</li>
 *   <li>Task ownership validation</li>
 *   <li>File access permission checks</li>
 *   <li>Multi-tenant data isolation</li>
 * </ul>
 *
 * <p><strong>Extensibility:</strong> As a business access gateway component, this controller
 * is designed to support future expansion beyond data export operations, including business
 * process automation, data transformation, integration workflows, and custom business logic
 * execution through the underlying Hercules framework.
 *
 * <p>All endpoints follow RESTful design principles and return standardized response
 * formats using {@link BaseResponse} wrapper for consistent error handling and data
 * structure across the business access gateway API.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@RestController
@RequestMapping("/cem-download-service")
public class CemDataDownloadServiceController {

    @Autowired
    private ExportFormatConfigService exportFormatConfigService;

    @Autowired
    private CmsDataDownloadTaskDefService taskDefService;

    @Autowired
    private CmsDataDownloadTaskService taskService;

    @Autowired
    private CmsDataDownloadTaskQueryService taskQueryService;

    @Autowired
    private TaskSubmissionService taskSubmissionService;

    @Autowired
    private TaskStatusSyncService taskStatusSyncService;

    @Autowired
    private FileDownloadService fileDownloadService;

    @Autowired
    private TaskSubmissionAndDownloadService taskSubmissionAndDownloadService;

    // ========== Permission Extraction Helper Methods ==========

    /**
     * Extract appKey and userId from UserAccessInfo.
     *
     * @param managerLoginInfo manager login information
     * @return array containing appKey and userId, [0] is appKey, [1] is userId
     */
    private String[] extractAuthInfo(UserAccessInfo managerLoginInfo) {
        if (managerLoginInfo == null) {
            return new String[]{null, null};
        }
        String appKey = managerLoginInfo.getAppKey();
        String userId = String.valueOf(managerLoginInfo.getUserId());
        return new String[]{appKey, userId};
    }

    /**
     * Get dynamic form configuration parameters supported by specified export format.
     *
     * @param format export format (csv, json, parquet, xlsx)
     * @return dynamic form configuration list
     */
    @GetMapping("/export-format-config/{format}")
    public BaseResponse<List<DynamicEntity>> getExportFormatConfig(@PathVariable String format) {
        try {
            CmsDownloadFormat downloadFormat = CmsDownloadFormat.valueOf(format.toUpperCase());
            List<DynamicEntity> configList = exportFormatConfigService.getFormatConfigFields(downloadFormat);
            return BaseResponse.success(configList);
        } catch (IllegalArgumentException e) {
            return BaseResponse.fail("Unsupported export format: " + format + ", supported formats: csv, json, parquet, xlsx");
        }
    }

    /**
     * Get all supported export formats and their configuration parameters
     *
     * <p>This endpoint returns a comprehensive mapping of all supported export formats
     * and their respective configuration parameters. Each format includes detailed
     * parameter definitions that can be used to dynamically generate configuration
     * forms in client applications.
     *
     * @return mapping of all format configuration parameters
     */
    @GetMapping("/export-format-config/all")
    public BaseResponse<java.util.Map<String, List<DynamicEntity>>> getAllExportFormatConfigs() {
        java.util.Map<String, List<DynamicEntity>> allConfigs = exportFormatConfigService.getAllFormatConfigFields();
        return BaseResponse.success(allConfigs);
    }

    // ========== CmsDataDownloadTaskDef CRUD Operations ==========

    /**
     * Create task definition
     *
     * @param taskDefVO task definition information
     * @return creation result
     */
    @PostMapping("/task-def")
    public BaseResponse<CmsDataDownloadTaskDefPO> createTaskDef(@Valid @RequestBody CmsDataDownloadTaskDefVO taskDefVO) {
        // Check if business key already exists
//        CmsDataDownloadTaskDefPO existing = taskDefService.getById(taskDefVO.getBusinessKey());
//        if (existing != null) {
//            return BaseResponse.fail("Business scenario key already exists: " + taskDefVO.getBusinessKey());
//        }

        // Convert VO to PO
        CmsDataDownloadTaskDefPO taskDefPO = taskDefVO.parse2Po();
        taskDefPO.setInsertTime(LocalDateTime.now());
        taskDefPO.setUpdateTime(LocalDateTime.now());

        // Save
        boolean success = taskDefService.saveOrUpdate(taskDefPO);
        if (success) {
            return BaseResponse.success(taskDefPO);
        } else {
            return BaseResponse.fail("Failed to create task definition");
        }
    }

    /**
     * Get task definition by business key
     *
     * @param businessKey business scenario key
     * @return task definition information
     */
    @GetMapping("/task-def/{businessKey}")
    public BaseResponse<CmsDataDownloadTaskDefPO> getTaskDef(@PathVariable String businessKey) {
        CmsDataDownloadTaskDefPO taskDef = taskDefService.getById(businessKey);
        if (taskDef != null) {
            return BaseResponse.success(taskDef);
        } else {
            return BaseResponse.fail("Task definition does not exist: " + businessKey);
        }
    }

    /**
     * Update task definition
     *
     * <p>Updates an existing task definition with new configuration parameters.
     * This endpoint allows modification of all task definition properties while
     * preserving the original creation timestamp and business key.
     *
     * @param businessKey business scenario key
     * @param taskDefVO   task definition information
     * @return update result
     */
    @PutMapping("/task-def/{businessKey}")
    public BaseResponse<CmsDataDownloadTaskDefPO> updateTaskDef(@PathVariable String businessKey,
                                                                @Valid @RequestBody CmsDataDownloadTaskDefVO taskDefVO) {
        // Check if task definition exists
        CmsDataDownloadTaskDefPO existing = taskDefService.getById(businessKey);
        if (existing == null) {
            return BaseResponse.fail("Task definition does not exist: " + businessKey);
        }

        // Convert VO to PO
        CmsDataDownloadTaskDefPO taskDefPO = taskDefVO.parse2Po();
//        BeanUtils.copyProperties(taskDefVO, taskDefPO);
        taskDefPO.setBusinessKey(businessKey); // Ensure primary key remains unchanged
        taskDefPO.setInsertTime(existing.getInsertTime()); // Preserve original insertion time
        taskDefPO.setUpdateTime(LocalDateTime.now());

        // Update
        boolean success = taskDefService.updateById(taskDefPO);
        if (success) {
            return BaseResponse.success(taskDefPO);
        } else {
            return BaseResponse.fail("Failed to update task definition");
        }
    }

    /**
     * Delete task definition
     *
     * <p>Removes a task definition from the system. This operation will permanently
     * delete the task definition and cannot be undone. Ensure that no active tasks
     * are using this definition before deletion.
     *
     * @param businessKey business scenario key
     * @return deletion result
     */
    @DeleteMapping("/task-def/{businessKey}")
    public BaseResponse<Boolean> deleteTaskDef(@PathVariable String businessKey) {
        // Check if task definition exists
        CmsDataDownloadTaskDefPO existing = taskDefService.getById(businessKey);
        if (existing == null) {
            return BaseResponse.fail("Task definition does not exist: " + businessKey);
        }

        // Delete
        boolean success = taskDefService.removeById(businessKey);
        if (success) {
            return BaseResponse.success(true);
        } else {
            return BaseResponse.fail("Failed to delete task definition");
        }
    }

    /**
     * Paginated query of task definition list
     *
     * @param current         current page number, default 1
     * @param size            page size, default 10
     * @param businessKey     business scenario key (fuzzy query)
     * @param businessDesc    business scenario description (fuzzy query)
     * @param pluginGroup     plugin group (exact query)
     * @param exportFormatType export format (exact query)
     * @return paginated result
     */
    @GetMapping("/task-def")
    public BaseResponse<IPage<CmsDataDownloadTaskDefPO>> getTaskDefList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String businessKey,
            @RequestParam(required = false) String businessDesc,
            @RequestParam(required = false) String pluginGroup,
            @RequestParam(required = false) String exportFormatType) {

        // Create pagination object
        Page<CmsDataDownloadTaskDefPO> page = new Page<>(current, size);

        // Build query conditions
        LambdaQueryWrapper<CmsDataDownloadTaskDefPO> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(businessKey)) {
            queryWrapper.like(CmsDataDownloadTaskDefPO::getBusinessKey, businessKey);
        }
        if (StringUtils.hasText(businessDesc)) {
            queryWrapper.like(CmsDataDownloadTaskDefPO::getBusinessDesc, businessDesc);
        }
        if (StringUtils.hasText(pluginGroup)) {
            queryWrapper.eq(CmsDataDownloadTaskDefPO::getPluginGroup, pluginGroup);
        }
        if (StringUtils.hasText(exportFormatType)) {
            queryWrapper.eq(CmsDataDownloadTaskDefPO::getExportFormatType, exportFormatType);
        }

        // Sort by update time in descending order
        queryWrapper.orderByDesc(CmsDataDownloadTaskDefPO::getUpdateTime);

        // Execute paginated query
        IPage<CmsDataDownloadTaskDefPO> result = taskDefService.page(page, queryWrapper);
        return BaseResponse.success(result);
    }

    /**
     * Get all task definition list (without pagination)
     *
     * @return all task definitions
     */
    @GetMapping("/task-def/all")
    public BaseResponse<List<CmsDataDownloadTaskDefPO>> getAllTaskDefs() {
        List<CmsDataDownloadTaskDefPO> taskDefs = taskDefService.list();
        return BaseResponse.success(taskDefs);
    }

    /**
     * Batch delete task definitions
     *
     * <p>Removes multiple task definitions from the system in a single operation.
     * This is useful for cleanup operations or bulk management of task definitions.
     * All specified definitions will be permanently deleted.
     *
     * @param businessKeys list of business scenario keys
     * @return deletion result
     */
    @DeleteMapping("/task-def/batch")
    public BaseResponse<Boolean> batchDeleteTaskDef(@RequestBody List<String> businessKeys) {
        if (businessKeys == null || businessKeys.isEmpty()) {
            return BaseResponse.fail("Business scenario key list cannot be empty");
        }

        boolean success = taskDefService.removeByIds(businessKeys);
        if (success) {
            return BaseResponse.success(true);
        } else {
            return BaseResponse.fail("Failed to batch delete task definitions");
        }
    }

    /**
     * Query task definitions by plugin group
     *
     * <p>Retrieves all task definitions that belong to a specific plugin group.
     * This is useful for organizing and managing related task definitions that
     * share common execution logic or business domain.
     *
     * @param pluginGroup plugin group identifier
     * @return list of task definitions
     */
    @GetMapping("/task-def/by-plugin-group/{pluginGroup}")
    public BaseResponse<List<CmsDataDownloadTaskDefPO>> getTaskDefsByPluginGroup(@PathVariable String pluginGroup) {
        LambdaQueryWrapper<CmsDataDownloadTaskDefPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CmsDataDownloadTaskDefPO::getPluginGroup, pluginGroup);

        List<CmsDataDownloadTaskDefPO> taskDefs = taskDefService.list(queryWrapper);
        return BaseResponse.success(taskDefs);
    }

    /**
     * Query task definitions by export format
     *
     * @param exportFormatType export format
     * @return task definition list
     */
    @GetMapping("/task-def/by-export-format/{exportFormatType}")
    public BaseResponse<List<CmsDataDownloadTaskDefPO>> getTaskDefsByExportFormat(@PathVariable String exportFormatType) {
        LambdaQueryWrapper<CmsDataDownloadTaskDefPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CmsDataDownloadTaskDefPO::getExportFormatType, exportFormatType);

        List<CmsDataDownloadTaskDefPO> taskDefs = taskDefService.list(queryWrapper);
        return BaseResponse.success(taskDefs);
    }

    // ========== Task Submission and Status Query Functions ==========

    /**
     * Submit data download task (supports backend service calls)
     *
     * @param request task submission request (can include appKey and userId for backend service calls)
     * @return task information
     */
    @PostMapping("/submit-task")
    public BaseResponse<CmsDataDownloadTaskVO> submitTask(@Valid @RequestBody SubmitTaskRequestVO request) {
        try {
            // If the request contains appKey and userId, use these parameters (for backend service calls)
            // If not included, pass null (original no permission control logic)
            String appKey = request.getAppKey();
            String userId = request.getUserId();

            CmsDataDownloadTaskPO task = taskSubmissionService.submitTask(request, appKey, userId, request.getOperatorSource(), request.getUserName());
            return BaseResponse.success(new CmsDataDownloadTaskVO(task));
        } catch (Exception e) {
            return BaseResponse.fail("Task submission failed: " + e.getMessage());
        }
    }

    /**
     * Paginated query of task list
     *
     * @param current      current page number, default 1
     * @param size         page size, default 10
     * @param businessKey  business scenario key (exact query)
     * @param taskStatus   task status (exact query)
     * @return paginated result
     */
    @GetMapping("/task")
    public BaseResponse<IPage<CmsDataDownloadTaskVO>> getTaskList(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String businessKey,
            @RequestParam(required = false) String taskStatus) {

        // Create pagination object
        Page<CmsDataDownloadTaskPO> page = new Page<>(current, size);

        // Build query conditions
        LambdaQueryWrapper<CmsDataDownloadTaskPO> queryWrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(businessKey)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getBusinessKey, businessKey);
        }
        if (StringUtils.hasText(taskStatus)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getTaskStatus, taskStatus);
        }

        // Sort by insertion time in descending order
        queryWrapper.orderByDesc(CmsDataDownloadTaskPO::getInsertTime);

        // Execute paginated query
        IPage<CmsDataDownloadTaskPO> result = taskService.page(page, queryWrapper);
        return BaseResponse.success(result.convert(CmsDataDownloadTaskVO::new));
    }

    /**
     * Manually synchronize task status
     *
     * <p>Forces a manual synchronization of task status with the Hercules execution
     * framework. This is useful when automatic status updates are delayed or when
     * immediate status verification is required.
     *
     * @param taskId task ID
     * @return synchronization result
     */
    @PostMapping("/task/{taskId}/sync-status")
    public BaseResponse<Boolean> syncTaskStatus(@PathVariable Long taskId) {
        try {
            boolean success = taskStatusSyncService.syncTaskStatusManually(taskId);
            if (success) {
                return BaseResponse.success(true);
            } else {
                return BaseResponse.fail("Failed to synchronize task status");
            }
        } catch (Exception e) {
            return BaseResponse.fail("Task status synchronization error: " + e.getMessage());
        }
    }

    /**
     * Get task status statistics
     *
     * @return status statistics information
     */
    @GetMapping("/task/status-stats")
    public BaseResponse<java.util.Map<String, Long>> getTaskStatusStats() {
        try {
            // Count tasks by status

            // Query all tasks
            List<CmsDataDownloadTaskPO> allTasks = taskService.list();

            // Group and count by status
            java.util.Map<String, Long> statusCounts = allTasks.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            task -> task.getTaskStatus() != null ? task.getTaskStatus() : "UNKNOWN",
                            java.util.stream.Collectors.counting()
                    ));

            java.util.Map<String, Long> stats = new java.util.HashMap<>(statusCounts);
            stats.put("TOTAL", (long) allTasks.size());

            return BaseResponse.success(stats);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to get task status statistics: " + e.getMessage());
        }
    }

    // ========== Abnormal Task Cleanup Functions ==========

    /**
     * Manually clean up abnormal tasks
     * Mark RUNNING/INIT status tasks older than 3 days as CANCELLED
     *
     * @return cleanup result
     */
    @PostMapping("/task/cleanup-abnormal")
    public BaseResponse<java.util.Map<String, Object>> cleanupAbnormalTasks() {
        try {
            int cleanedCount = taskStatusSyncService.cleanupAbnormalTasksManually();

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("cleanedCount", cleanedCount);
            result.put("message", "Abnormal task cleanup completed");

            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to clean up abnormal tasks: " + e.getMessage());
        }
    }

    /**
     * Get abnormal task statistics
     *
     * <p>Retrieves statistics for abnormal tasks that have been in RUNNING/INIT
     * status for more than 3 days. This helps identify tasks that may be stuck
     * or require manual intervention.
     *
     * @return abnormal task statistics
     */
    @GetMapping("/task/abnormal-stats")
    public BaseResponse<java.util.Map<String, Object>> getAbnormalTaskStats() {
        try {
            java.util.Map<String, Object> stats = taskStatusSyncService.getAbnormalTaskStats();
            return BaseResponse.success(stats);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to get abnormal task statistics: " + e.getMessage());
        }
    }

    // ========== Task Query Functions with Permission Control ==========

    /**
     * Paginated query of task list (supports backend service calls and permission-free calls)
     * Smart permission control: automatically selects query mode based on whether appKey and userId are provided
     *
     * @param queryVO query conditions (can include appKey and userId for backend service calls)
     * @return paginated result
     */
    @PostMapping("/public/task/query")
    public BaseResponse<IPage<CmsDataDownloadTaskVO>> queryTasks(@RequestBody CmsDataDownloadTaskQueryVO queryVO) {
        try {
            // Use unified query method, passing appKey and userId from queryVO
            IPage<CmsDataDownloadTaskPO> result = taskQueryService.queryTasks(queryVO, queryVO.getAppKey(), queryVO.getUserId());
            return BaseResponse.success(result.convert(CmsDataDownloadTaskVO::new));
        } catch (Exception e) {
            return BaseResponse.fail("Failed to query task list: " + e.getMessage());
        }
    }

    /**
     * Query task information by task ID (supports backend service calls and no-permission calls)
     * Smart permission control: automatically selects query mode based on whether appKey and userId are provided
     *
     * @param taskId task ID
     * @param appKey application key (optional, for backend service calls)
     * @param userId user ID (optional, for backend service calls)
     * @return task information
     */
    @GetMapping("/public/task/{taskId}")
    public BaseResponse<CmsDataDownloadTaskVO> getTaskById(@PathVariable Long taskId,
                                                          @RequestParam(required = false) String appKey,
                                                          @RequestParam(required = false) String userId) {
        try {
            // Use unified query method with smart permission control
            CmsDataDownloadTaskPO task = taskQueryService.getTaskById(taskId, appKey, userId);
            if (task == null) {
                String errorMsg = (StringUtils.hasText(appKey) && StringUtils.hasText(userId))
                    ? "Task does not exist or no permission to access: " + taskId
                    : "Task does not exist: " + taskId;
                return BaseResponse.fail(errorMsg);
            }

            return BaseResponse.success(new CmsDataDownloadTaskVO(task));
        } catch (Exception e) {
            return BaseResponse.fail("Failed to query task information: " + e.getMessage());
        }
    }

    /**
     * Query task list by business key (supports backend service calls and no-permission calls)
     * Smart permission control: automatically selects query mode based on whether appKey and userId are provided
     *
     * @param businessKey business scenario key
     * @param appKey application key (optional, for backend service calls)
     * @param userId user ID (optional, for backend service calls)
     * @return task list
     */
    @GetMapping("/public/task/business/{businessKey}")
    public BaseResponse<List<CmsDataDownloadTaskVO>> getTasksByBusinessKey(@PathVariable String businessKey,
                                                                          @RequestParam(required = false) String appKey,
                                                                          @RequestParam(required = false) String userId) {
        try {
            // Use unified query method with smart permission control
            List<CmsDataDownloadTaskPO> tasks = taskQueryService.getTasksByBusinessKey(businessKey, appKey, userId);

            List<CmsDataDownloadTaskVO> taskVOs = tasks.stream()
                    .map(CmsDataDownloadTaskVO::new)
                    .collect(java.util.stream.Collectors.toList());
            return BaseResponse.success(taskVOs);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to query task list: " + e.getMessage());
        }
    }

    /**
     * Get user task status statistics (with permission control)
     *
     * <p>Retrieves task status statistics for the authenticated user, providing
     * insights into task distribution across different status categories.
     * This helps users understand their task execution patterns and identify
     * potential issues.
     *
     * @param managerLoginInfo manager login information
     * @return status statistics information
     */
    @GetMapping("/task/user-status-stats")
    public BaseResponse<java.util.Map<String, Long>> getUserTaskStatusStats(@UserLogInInfo UserAccessInfo managerLoginInfo) {
        try {
            java.util.Map<String, Long> stats = taskQueryService.getTaskStatusStats(managerLoginInfo.getAppKey(), Objects.toString(managerLoginInfo.getUserId()));
            return BaseResponse.success(stats);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to get user task status statistics: " + e.getMessage());
        }
    }

    // ========== File Download Functions ==========

    /**
     * Batch get file download URLs (with permission control)
     *
     * <p>Retrieves download URLs for multiple tasks in a single request.
     * This endpoint is optimized for bulk operations and includes permission
     * validation to ensure users can only access their own task files.
     *
     * @param taskIds list of task IDs
     * @param managerLoginInfo manager login information
     * @return mapping of task ID to download URL
     */
    @PostMapping("/task/batch-download-urls")
    public BaseResponse<java.util.Map<String, Object>> batchGetFileDownloadUrls(@RequestBody java.util.List<Long> taskIds,
                                                                                @UserLogInInfo UserAccessInfo managerLoginInfo) {
        try {
            if (taskIds == null || taskIds.isEmpty()) {
                return BaseResponse.fail("Task ID list cannot be empty");
            }

            if (taskIds.size() > 100) {
                return BaseResponse.fail("Batch download URL count cannot exceed 100");
            }

            java.util.Map<Long, String> downloadUrls = fileDownloadService.batchGetFileDownloadUrls(taskIds, managerLoginInfo.getAppKey(),Objects.toString(managerLoginInfo.getUserId()));

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("downloadUrls", downloadUrls);
            result.put("totalCount", taskIds.size());
            result.put("successCount", downloadUrls.values().stream().mapToInt(url -> url != null ? 1 : 0).sum());
            result.put("expirationTime", "1 hour");
            result.put("generateTime", java.time.LocalDateTime.now());

            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to batch get file download URLs: " + e.getMessage());
        }
    }

    /**
     * Check if file exists (with permission control)
     *
     * @param taskId task ID
     * @param managerLoginInfo manager login information
     * @return whether file exists
     */
    @GetMapping("/task/{taskId}/file-exists")
    public BaseResponse<java.util.Map<String, Object>> checkFileExists(@PathVariable Long taskId,
                                                                       @UserLogInInfo UserAccessInfo managerLoginInfo) {
        try {
            boolean exists = fileDownloadService.checkFileExists(taskId, managerLoginInfo.getAppKey(),Objects.toString(managerLoginInfo.getUserId()));

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("taskId", taskId);
            result.put("fileExists", exists);
            result.put("checkTime", java.time.LocalDateTime.now());

            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to check if file exists: " + e.getMessage());
        }
    }

    // ========== No-Authentication File Download Functions ==========

    /**
     * Get file download URL (smart permission control)
     * Supports passing appKey and userId through request parameters for permission control
     *
     * @param taskId task ID
     * @param appKey application key (optional, for permission control)
     * @param userId user ID (optional, for permission control)
     * @return file download URL
     */
    @GetMapping("/public/task/{taskId}/download-url")
    public BaseResponse<java.util.Map<String, Object>> getFileDownloadUrlWithoutAuth(@PathVariable Long taskId,
                                                                                     @RequestParam(required = false) String appKey,
                                                                                     @RequestParam(required = false) String userId) {
        try {
            String downloadUrl = fileDownloadService.getFileDownloadUrl(taskId, appKey, userId);
            boolean authRequired = StringUtils.hasText(appKey) && StringUtils.hasText(userId);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("taskId", taskId);
            result.put("downloadUrl", downloadUrl);
            result.put("expirationTime", "1 hour");
            result.put("generateTime", java.time.LocalDateTime.now());
            result.put("authRequired", authRequired);

            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to get file download URL: " + e.getMessage());
        }
    }

    /**
     * Batch get file download URLs (no authentication)
     *
     * <p>Retrieves download URLs for multiple tasks without authentication.
     * This endpoint is designed for internal service-to-service communication
     * and public access scenarios where authentication is not required.
     *
     * @param taskIds list of task IDs
     * @return mapping of task ID to download URL
     */
    @PostMapping("/public/task/batch-download-urls")
    public BaseResponse<java.util.Map<String, Object>> batchGetFileDownloadUrlsWithoutAuth(@RequestBody java.util.List<Long> taskIds) {
        try {
            if (taskIds == null || taskIds.isEmpty()) {
                return BaseResponse.fail("Task ID list cannot be empty");
            }

            if (taskIds.size() > 100) {
                return BaseResponse.fail("Batch download URL count cannot exceed 100");
            }

            java.util.Map<Long, String> downloadUrls = fileDownloadService.batchGetFileDownloadUrls(taskIds,null,null);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("downloadUrls", downloadUrls);
            result.put("totalCount", taskIds.size());
            result.put("successCount", downloadUrls.values().stream().mapToInt(url -> url != null ? 1 : 0).sum());
            result.put("expirationTime", "1 hour");
            result.put("generateTime", java.time.LocalDateTime.now());
            result.put("authRequired", false);

            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to batch get file download URLs: " + e.getMessage());
        }
    }

    /**
     * Check if file exists (without authentication)
     *
     * @param taskId task ID
     * @return whether file exists
     */
    @GetMapping("/public/task/{taskId}/file-exists")
    public BaseResponse<java.util.Map<String, Object>> checkFileExistsWithoutAuth(@PathVariable Long taskId) {
        try {
            boolean exists = fileDownloadService.checkFileExists(taskId,null,null);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("taskId", taskId);
            result.put("fileExists", exists);
            result.put("checkTime", java.time.LocalDateTime.now());
            result.put("authRequired", false);

            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to check if file exists: " + e.getMessage());
        }
    }

    /**
     * Get latest task download URL by business key (no authentication)
     *
     * @param businessKey business scenario key
     * @return file download URL
     */
    @GetMapping("/public/business/{businessKey}/latest-download-url")
    public BaseResponse<java.util.Map<String, Object>> getLatestFileDownloadUrlByBusinessKey(@PathVariable String businessKey) {
        try {
            String downloadUrl = fileDownloadService.getLatestFileDownloadUrlByBusinessKey(businessKey,null,null);

            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("businessKey", businessKey);
            result.put("downloadUrl", downloadUrl);
            result.put("expirationTime", "1 hour");
            result.put("generateTime", java.time.LocalDateTime.now());
            result.put("authRequired", false);

            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to get latest file download URL: " + e.getMessage());
        }
    }



    // ========== Integrated Task Submission and Download URL Retrieval Functions ==========

    /**
     * Submit task and wait for completion, then get download URL (with permission verification)
     *
     * @param request task submission request
     * @param managerLoginInfo manager login information
     * @return complete information including download URL
     */
    @PostMapping("/submit-and-download")
    public BaseResponse<TaskSubmissionAndDownloadResultVO> submitTaskAndGetDownloadUrl(
            @Valid @RequestBody SubmitTaskRequestVO request,
            @UserLogInInfo UserAccessInfo managerLoginInfo) {
        try {
            request.setUserName(managerLoginInfo.getUser());
            TaskSubmissionAndDownloadResultVO result = taskSubmissionAndDownloadService.submitTaskAndGetDownloadUrl(request, managerLoginInfo.getAppKey(),Objects.toString(managerLoginInfo.getUserId()));
            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to submit task and get download URL: " + e.getMessage());
        }
    }

    /**
     * Submit task and wait for completion, then get download URL (with permission verification, custom wait time)
     *
     * @param request task submission request
     * @param managerLoginInfo manager login information
     * @param maxWaitSeconds maximum wait time (seconds), default 300 seconds
     * @param pollIntervalSeconds polling interval (seconds), default 5 seconds
     * @return complete information including download URL
     */
    @PostMapping("/submit-and-download-with-timeout")
    public BaseResponse<TaskSubmissionAndDownloadResultVO> submitTaskAndGetDownloadUrlWithTimeout(
            @Valid @RequestBody SubmitTaskRequestVO request,
            @UserLogInInfo UserAccessInfo managerLoginInfo,
            @RequestParam(defaultValue = "300") int maxWaitSeconds,
            @RequestParam(defaultValue = "5") int pollIntervalSeconds) {
        try {
            // Parameter validation
            if (maxWaitSeconds <= 0 || maxWaitSeconds > 1800) { // Maximum 30 minutes
                return BaseResponse.fail("Maximum wait time must be between 1-1800 seconds");
            }
            if (pollIntervalSeconds <= 0 || pollIntervalSeconds > 60) { // Maximum 1 minute interval
                return BaseResponse.fail("Polling interval must be between 1-60 seconds");
            }

            request.setUserName(managerLoginInfo.getUser());
            TaskSubmissionAndDownloadResultVO result = taskSubmissionAndDownloadService.submitTaskAndGetDownloadUrl(
                    request,
                    managerLoginInfo.getAppKey(),
                    Objects.toString(managerLoginInfo.getUserId()),
                    Objects.toString(managerLoginInfo.getUser()),
                    maxWaitSeconds,
                    pollIntervalSeconds);
            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to submit task and get download URL: " + e.getMessage());
        }
    }

    /**
     * Submit task and wait for completion, then get download URL (no permission verification, supports backend service calls)
     *
     * <p>This endpoint allows backend services to submit tasks and retrieve download URLs
     * without user authentication. It supports service-to-service communication and
     * automated workflows that don't require user context.
     *
     * @param request task submission request (can include appKey and userId for backend service calls)
     * @return complete information including download URL
     */
    @PostMapping("/public/submit-and-download")
    public BaseResponse<TaskSubmissionAndDownloadResultVO> submitTaskAndGetDownloadUrlWithoutAuth(
            @Valid @RequestBody SubmitTaskRequestVO request) {
        try {
            TaskSubmissionAndDownloadResultVO result = taskSubmissionAndDownloadService.
                    submitTaskAndGetDownloadUrl(request,null,null);
            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to submit task and get download URL: " + e.getMessage());
        }
    }

    /**
     * Submit task and wait for completion, then get download URL (no permission verification, custom wait time, supports backend service calls)
     *
     * @param request task submission request (can include appKey and userId for backend service calls)
     * @param maxWaitSeconds maximum wait time (seconds), default 300 seconds
     * @param pollIntervalSeconds polling interval (seconds), default 5 seconds
     * @return complete information including download URL
     */
    @PostMapping("/public/submit-and-download-with-timeout")
    public BaseResponse<TaskSubmissionAndDownloadResultVO> submitTaskAndGetDownloadUrlWithoutAuthAndTimeout(
            @Valid @RequestBody SubmitTaskRequestVO request,
            @RequestParam(defaultValue = "300") int maxWaitSeconds,
            @RequestParam(defaultValue = "5") int pollIntervalSeconds) {
        try {
            // Parameter validation
            if (maxWaitSeconds <= 0 || maxWaitSeconds > 1800) { // Maximum 30 minutes
                return BaseResponse.fail("Maximum wait time must be between 1-1800 seconds");
            }
            if (pollIntervalSeconds <= 0 || pollIntervalSeconds > 60) { // Maximum 1 minute interval
                return BaseResponse.fail("Polling interval must be between 1-60 seconds");
            }

            TaskSubmissionAndDownloadResultVO result = taskSubmissionAndDownloadService.submitTaskAndGetDownloadUrl(
                    request,null,null,null, maxWaitSeconds, pollIntervalSeconds);
            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to submit task and get download URL: " + e.getMessage());
        }
    }

    /**
     * Get current task status (for client-side active querying)
     *
     * <p>Retrieves the current status of a specific task. This endpoint is designed
     * for client applications that need to actively poll task status for real-time
     * updates and progress monitoring.
     *
     * @param taskId task ID
     * @return task status information
     */
    @GetMapping("/task/{taskId}/status")
    public BaseResponse<TaskStatusVO> getTaskStatus(@PathVariable Long taskId) {
        try {
            TaskStatusVO result = taskSubmissionAndDownloadService.getTaskStatus(taskId);
            return BaseResponse.success(result);
        } catch (Exception e) {
            return BaseResponse.fail("Failed to get task status: " + e.getMessage());
        }
    }
}
