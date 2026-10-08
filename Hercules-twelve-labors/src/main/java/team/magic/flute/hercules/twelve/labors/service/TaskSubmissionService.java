package team.magic.flute.hercules.twelve.labors.service;

import com.fasterxml.jackson.core.type.TypeReference;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.status.TaskStatus;
import team.magic.flute.hercules.common.util.JacksonUtils;
import team.magic.flute.hercules.twelve.labors.api.HerculesManagerApi;
import team.magic.flute.hercules.twelve.labors.business.export.entity.RdsExportContext;
import team.magic.flute.hercules.twelve.labors.business.export.entity.RdsInfo;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.twelve.labors.constant.CmsDownloadFormat;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskDefPO;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskPO;
import team.magic.flute.hercules.twelve.labors.dto.CsvExportFormatParams;
import team.magic.flute.hercules.twelve.labors.dto.ExcelExportFormatParams;
import team.magic.flute.hercules.twelve.labors.dto.JsonExportFormatParams;
import team.magic.flute.hercules.twelve.labors.dto.ParquetExportFormatParams;
import team.magic.flute.hercules.twelve.labors.util.ExportFormatParamsConverter;
import team.magic.flute.hercules.twelve.labors.util.OssPathUtil;
import team.magic.flute.hercules.twelve.labors.util.SqlTemplateUtil;
import team.magic.flute.hercules.twelve.labors.vo.SubmitTaskRequestVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Task Submission Service
 *
 * <p>This service handles the submission and orchestration of data export tasks within
 * the Hercules ecosystem. It serves as the bridge between the CMS interface and the
 * distributed task execution system, managing the complete lifecycle of task submission
 * from validation to execution coordination.
 *
 * <p>Key responsibilities include:
 * <ul>
 *   <li>Task request validation and preprocessing</li>
 *   <li>Export context construction and configuration</li>
 *   <li>Integration with Hercules Manager for task execution</li>
 *   <li>Task status tracking and database persistence</li>
 *   <li>Error handling and rollback mechanisms</li>
 * </ul>
 *
 * <p>The service supports multiple export formats (CSV, JSON, Parquet, XLSX) with
 * configurable parameters for each format type. It handles complex SQL template
 * processing, parameter substitution, and generates appropriate file paths for
 * cloud storage destinations.
 *
 * <p>Transaction management ensures data consistency across task creation and
 * submission operations, with automatic rollback on failures to maintain system
 * integrity.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Service
public class TaskSubmissionService {
    
    @Autowired
    private CmsDataDownloadTaskDefService taskDefService;
    
    @Autowired
    private CmsDataDownloadTaskService taskService;
    
    @Autowired
    private HerculesManagerApi herculesManagerApi;

    
    /**
     * Submit a data export task.
     *
     * <p>This is a convenience method that submits a task using the operator source
     * and user name from the request object. It delegates to the full submission
     * method with null values for appKey and userId.
     *
     * @param request the task submission request containing all necessary parameters
     * @return the created task entity with generated ID and initial status
     * @throws RuntimeException if task definition is not found or submission fails
     */
    @Transactional(rollbackFor = Exception.class)
    public CmsDataDownloadTaskPO submitTask(SubmitTaskRequestVO request) {
        return submitTask(request, null, null, request.getOperatorSource(),request.getUserName());
    }

    /**
     * Submit a data export task with user information.
     *
     * <p>This method submits a task with specific app key and user ID for access
     * control and auditing purposes. The operator source will be null in this case.
     *
     * @param request the task submission request containing task parameters
     * @param appKey the application key for access control
     * @param userId the user ID for auditing and ownership
     * @return the created task entity with generated ID and initial status
     * @throws RuntimeException if task definition is not found or submission fails
     */
    @Transactional(rollbackFor = Exception.class)
    public CmsDataDownloadTaskPO submitTask(SubmitTaskRequestVO request, String appKey, String userId) {
        return submitTask(request, appKey, userId, null, request.getUserName());
    }

    /**
     * Submit a data export task with complete user information and operation source.
     *
     * <p>This is the main task submission method that handles the complete workflow
     * of creating and submitting a data export task to the Hercules execution system.
     *
     * <p>The submission process includes:
     * <ol>
     *   <li>Validating the task definition exists</li>
     *   <li>Processing SQL template with parameter substitution</li>
     *   <li>Constructing export context with format-specific configuration</li>
     *   <li>Generating unique file paths for output storage</li>
     *   <li>Creating task record in database</li>
     *   <li>Submitting task to Hercules Manager for execution</li>
     * </ol>
     *
     * @param request the task submission request containing all task parameters
     * @param appKey the application key for access control and routing
     * @param userId the user ID for auditing and task ownership
     * @param operatorSource the source of the operation (e.g., "web-console", "api")
     * @param userName the human-readable user name for display purposes
     * @return the created task entity with generated ID and initial status
     * @throws RuntimeException if task definition is not found, SQL processing fails,
     *                         or Hercules Manager submission fails
     */
    @Transactional(rollbackFor = Exception.class)
    public CmsDataDownloadTaskPO submitTask(SubmitTaskRequestVO request,
                                            String appKey,
                                            String userId,
                                            String operatorSource,
                                            String userName) {
        // 1. Get task definition by businessKey
        CmsDataDownloadTaskDefPO taskDef = taskDefService.getById(request.getBusinessKey());
        if (taskDef == null) {
            throw new RuntimeException("Task definition not found: " + request.getBusinessKey());
        }

        // 2. Replace parameters in SQL template
        String processedSql = SqlTemplateUtil.replaceSqlParams(taskDef.getSqlTemplate(), request.getParams());
        
        // 3. Check for unreplaced parameters
        if (SqlTemplateUtil.hasUnreplacedParams(processedSql)) {
            List<String> unreplacedParams = SqlTemplateUtil.extractParamNames(processedSql);
            throw new RuntimeException("Unreplaced parameters exist in SQL template: " + unreplacedParams);
        }

        // 4. Generate OSS path
        String ossPath = OssPathUtil.generateOssPath(taskDef);

        // 5. Build RdsExportContext
        RdsExportContext rdsExportContext = buildRdsExportContext(taskDef, processedSql, ossPath);

        // 6. Create and save CmsDataDownloadTaskPO
        CmsDataDownloadTaskPO taskPO = new CmsDataDownloadTaskPO()
                .setBusinessKey(request.getBusinessKey())
                .setContext(rdsExportContext)
                .setFilePath(ossPath)
                .setTaskStatus(TaskStatus.INIT.name())
                .setAppKey(appKey)
                .setUserId(userId)
                .setUserName(userName)
                .setOperatorSource(operatorSource)
                .setInsertTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
        
        // Save to database first to get taskId
        boolean saveResult = taskService.save(taskPO);
        if (!saveResult) {
            throw new RuntimeException("Failed to save task");
        }

        // 7. Build the submission request (shared wire type with the manager)
        HerculesRunnableTaskInfo submitRequest = new HerculesRunnableTaskInfo()
                .setId(String.valueOf(taskPO.getTaskId()))
                .setExecutorRegion(taskDef.getExecutorRegion())
                .setDesc("CMS data download task: " + request.getBusinessKey())
                .setPluginGroup(taskDef.getPluginGroup())
                .setPluginHandle(taskDef.getPluginHandle())
                .setMaxRetryTimes(3);

        try {
            submitRequest.setContext(JacksonUtils.writeValueAsString(rdsExportContext));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize RdsExportContext", e);
        }
        
        // 8. Call HerculesManagerApi to submit task
        BaseResponse<HerculesRunnableTaskInfo> response = herculesManagerApi.submitOnceTask(submitRequest);

        // 9. Process return result
        if (response.getCode() == 200 && response.getData() != null) {
            HerculesRunnableTaskInfo herculesTaskInfo = response.getData();
            
            // Update task status
            taskPO.setTaskStatus(herculesTaskInfo.getStatus());
            taskPO.setUpdateTime(LocalDateTime.now());
            taskService.updateById(taskPO);
            
            return taskPO;
        } else {
            // Submission failed, update task status
            taskPO.setTaskStatus("FAILED");
            taskPO.setUpdateTime(LocalDateTime.now());
            taskService.updateById(taskPO);

            throw new RuntimeException("Failed to submit task: " + response.getMsg());
        }
    }
    
    /**
     * Build RdsExportContext
     *
     * @param taskDef task definition
     * @param processedSql processed SQL
     * @param ossPath OSS path
     * @return RdsExportContext
     */
    private RdsExportContext buildRdsExportContext(CmsDataDownloadTaskDefPO taskDef, String processedSql, String ossPath) {
        try {
            RdsExportContext context = new RdsExportContext();
            
            // Set RDS information
            if (StringUtils.hasText(taskDef.getSqlRdsInfos())) {
                List<RdsInfo> rdsInfos = JacksonUtils.readValue(taskDef.getSqlRdsInfos(), new TypeReference<List<RdsInfo>>() {});
                context.setRdsInfos(rdsInfos);
            }

            // Set SQL query
            context.setRdsQuery(processedSql);

            // Set OSS information
            context.setDuckdbMysqlExperimentalFilterPushdown(context.getDuckdbMysqlExperimentalFilterPushdown());
            context.setOssPath(ossPath);
            context.setBucketName(taskDef.getOssBucket());
            context.setOssKey(taskDef.getOssAccessId());
            context.setOssSecret(taskDef.getOssAccessSecret());
            context.setOssRegion(taskDef.getOssRegion());
            context.setOssEndpoint(taskDef.getOssEndpoint());
            
            // Set export format
            if (StringUtils.hasText(taskDef.getExportFormatType())) {
                CmsDownloadFormat exportFormat = CmsDownloadFormat.valueOf(taskDef.getExportFormatType().toUpperCase());
                context.setExportFormat(exportFormat);
            }

            // Set format configuration
            if (StringUtils.hasText(taskDef.getExportFormatConfig())) {
                Map<String, String> formatConfig;
                CmsDownloadFormat exportFormat = CmsDownloadFormat.valueOf(taskDef.getExportFormatType().toUpperCase());
                switch (exportFormat) {
                    case CSV:
                        formatConfig = ExportFormatParamsConverter.csvParamsToMap(JacksonUtils.readValue(taskDef.getExportFormatConfig(), CsvExportFormatParams.class));
                        break;
                    case JSON:
                        formatConfig = ExportFormatParamsConverter.jsonParamsToMap(JacksonUtils.readValue(taskDef.getExportFormatConfig(), JsonExportFormatParams.class));
                        break;
                    case PARQUET:
                        formatConfig = ExportFormatParamsConverter.parquetParamsToMap(JacksonUtils.readValue(taskDef.getExportFormatConfig(), ParquetExportFormatParams.class));
                        break;
                    case XLSX:
                        formatConfig = ExportFormatParamsConverter.excelParamsToMap(JacksonUtils.readValue(taskDef.getExportFormatConfig(), ExcelExportFormatParams.class));
                        break;
                    default:
                        throw new RuntimeException("Unsupported export format: " + taskDef.getExportFormatType());
                }
                context.setFormatConfig(formatConfig);
            }

            return context;

        } catch (Exception e) {
            throw new RuntimeException("Failed to build RdsExportContext", e);
        }
    }
}
