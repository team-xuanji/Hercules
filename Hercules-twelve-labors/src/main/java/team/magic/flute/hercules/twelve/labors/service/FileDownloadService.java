package team.magic.flute.hercules.twelve.labors.service;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import team.magic.flute.hercules.twelve.labors.business.export.entity.RdsExportContext;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskPO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * File Download Service for Business Access Gateway
 *
 * <p>Service for generating file download links within the Hercules business access gateway.
 * Provides secure and efficient file access with intelligent permission control.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Slf4j
@Service
public class FileDownloadService {
    
    @Autowired
    private CmsDataDownloadTaskQueryService taskQueryService;

    @Autowired
    private CmsDataDownloadTaskService taskService;

    @Autowired
    private OssClientCacheService ossClientCacheService;
    
    /**
     * Completed status list
     */
    private static final List<String> COMPLETED_STATUS = Arrays.asList("SUCCESS", "FAILED", "CANCELLED");

    /**
     * Download link validity time: 1 hour (milliseconds)
     */
    private static final long DOWNLOAD_URL_EXPIRATION_MS = 60 * 60 * 1000L;
    
    /**
     * Get file download URL (unified permission control)
     * Supports intelligent permission control: if appKey and userId are provided, permission verification is performed, otherwise all tasks are queried
     *
     * @param taskId task ID
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @return file download URL
     * @throws RuntimeException when task does not exist, no permission access, task not completed, or link generation fails
     */
    public String getFileDownloadUrl(Long taskId, String appKey, String userId) {
        // 1. Query task (intelligent permission control)
        CmsDataDownloadTaskPO task = taskQueryService.getTaskById(taskId, appKey, userId);
        if (task == null) {
            String errorMsg = (StringUtils.hasText(appKey) && StringUtils.hasText(userId))
                ? "Task does not exist or no permission to access: " + taskId
                : "Task does not exist: " + taskId;
            throw new RuntimeException(errorMsg);
        }

        // 2. Check if task status is completed
        if (!isCompletedStatus(task.getTaskStatus())) {
            throw new RuntimeException("Task not completed, cannot get download link. Current status: " + task.getTaskStatus());
        }

        // 3. Check if filePath exists
        if (!StringUtils.hasText(task.getFilePath())) {
            throw new RuntimeException("Task file path is empty, cannot generate download link");
        }

        // 4. Parse context to get OSS configuration information
        RdsExportContext context = task.getContext();
        if (context == null) {
            throw new RuntimeException("Task context information is empty, cannot get OSS configuration");
        }

        // 5. Validate OSS configuration completeness
        validateOssConfig(context);

        // 6. Extract OSS configuration parameters
        String endpoint = context.getOssEndpoint();
        String bucket = context.getBucketName();
        String objectKey = task.getFilePath();
        String ossKey = context.getOssKey();
        String ossSecret = context.getOssSecret();

        // 7. Check if download link already exists in cache
        String cachedUrl = ossClientCacheService.getCachedDownloadUrl(endpoint, bucket, objectKey, ossKey, ossSecret);
        if (cachedUrl != null) {
            log.debug("Using cached download link: taskId={}, url={}", taskId, cachedUrl);
            return cachedUrl;
        }

        // 8. Get or create OSS client
        OSS ossClient = ossClientCacheService.getOrCreateOssClient(endpoint, bucket, ossKey, ossSecret);

        // 9. Generate presigned download link
        String downloadUrl = generatePresignedUrl(ossClient, bucket, objectKey);

        // 10. Cache download link
        ossClientCacheService.cacheDownloadUrl(endpoint, bucket, objectKey, ossKey, ossSecret, downloadUrl);

        String authInfo = (StringUtils.hasText(appKey) && StringUtils.hasText(userId)) ? "with permission control" : "without permission control";
        log.info("Successfully generated file download link ({}): taskId={}, filePath={}, url={}", authInfo, taskId, objectKey, downloadUrl);
        return downloadUrl;
    }

    /**
     * Check if task status is completed
     *
     * @param taskStatus task status
     * @return whether it is completed status
     */
    private boolean isCompletedStatus(String taskStatus) {
        return taskStatus != null && COMPLETED_STATUS.contains(taskStatus.toUpperCase());
    }

    /**
     * Validate OSS configuration completeness
     *
     * @param context RDS export context
     * @throws RuntimeException when configuration is incomplete
     */
    private void validateOssConfig(RdsExportContext context) {
        if (!StringUtils.hasText(context.getOssEndpoint())) {
            throw new RuntimeException("OSS endpoint configuration is empty");
        }
        if (!StringUtils.hasText(context.getBucketName())) {
            throw new RuntimeException("OSS bucket configuration is empty");
        }
        if (!StringUtils.hasText(context.getOssKey())) {
            throw new RuntimeException("OSS access key ID configuration is empty");
        }
        if (!StringUtils.hasText(context.getOssSecret())) {
            throw new RuntimeException("OSS access secret configuration is empty");
        }
    }
    
    /**
     * Generate presigned download link
     *
     * @param ossClient OSS client
     * @param bucket bucket name
     * @param objectKey object key
     * @return presigned download link
     * @throws RuntimeException when link generation fails
     */
    private String generatePresignedUrl(OSS ossClient, String bucket, String objectKey) {
        try {
            // Set link expiration time to 1 hour later
            Date expiration = new Date(System.currentTimeMillis() + DOWNLOAD_URL_EXPIRATION_MS);

            // Create presigned URL request
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucket, objectKey);
            request.setExpiration(expiration);
            request.setMethod(HttpMethod.GET);

            // Generate presigned URL
            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();

        } catch (Exception e) {
            log.error("Failed to generate presigned download link: bucket={}, objectKey={}", bucket, objectKey, e);
            throw new RuntimeException("Failed to generate download link: " + e.getMessage(), e);
        }
    }
    
    /**
     * Batch get file download URLs (unified permission control)
     * Supports intelligent permission control: if appKey and userId are provided, permission verification is performed, otherwise all tasks are queried
     *
     * @param taskIds task ID list
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @return mapping of task ID to download URL
     */
    public java.util.Map<Long, String> batchGetFileDownloadUrls(List<Long> taskIds, String appKey, String userId) {
        java.util.Map<Long, String> result = new java.util.HashMap<>();

        for (Long taskId : taskIds) {
            try {
                String downloadUrl = getFileDownloadUrl(taskId, appKey, userId);
                result.put(taskId, downloadUrl);
            } catch (Exception e) {
                String authInfo = (StringUtils.hasText(appKey) && StringUtils.hasText(userId)) ? "with permission control" : "without permission control";
                log.warn("Failed to get task download link ({}): taskId={}, error={}", authInfo, taskId, e.getMessage());
                result.put(taskId, null);
            }
        }

        return result;
    }

    /**
     * Check if file exists (unified permission control)
     * Supports smart permission control: if appKey and userId are provided, permission verification is performed, otherwise all tasks are queried
     *
     * @param taskId task ID
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @return whether file exists
     */
    public boolean checkFileExists(Long taskId, String appKey, String userId) {
        try {
            CmsDataDownloadTaskPO task = taskQueryService.getTaskById(taskId, appKey, userId);
            if (task == null || !isCompletedStatus(task.getTaskStatus()) || !StringUtils.hasText(task.getFilePath())) {
                return false;
            }

            RdsExportContext context = task.getContext();
            if (context == null) {
                return false;
            }

            OSS ossClient = ossClientCacheService.getOrCreateOssClient(
                    context.getOssEndpoint(),
                    context.getBucketName(),
                    context.getOssKey(),
                    context.getOssSecret()
            );

            return ossClient.doesObjectExist(context.getBucketName(), task.getFilePath());

        } catch (Exception e) {
            String authInfo = (StringUtils.hasText(appKey) && StringUtils.hasText(userId)) ? "with permission control" : "without permission control";
            log.warn("Exception occurred while checking file existence ({}): taskId={}", authInfo, taskId, e);
            return false;
        }
    }

    /**
     * Get latest task download link by business key (unified permission control)
     * Supports intelligent permission control: if appKey and userId are provided, permission verification is performed, otherwise all tasks are queried
     *
     * @param businessKey business scenario key
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @return file download link
     * @throws RuntimeException when task does not exist or link generation fails
     */
    public String getLatestFileDownloadUrlByBusinessKey(String businessKey, String appKey, String userId) {
        // 1. Query the latest completed task under this business key (intelligent permission control)
        LambdaQueryWrapper<CmsDataDownloadTaskPO> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CmsDataDownloadTaskPO::getBusinessKey, businessKey)
                   .in(CmsDataDownloadTaskPO::getTaskStatus, COMPLETED_STATUS);

        // Intelligent permission control: add permission conditions only when valid permission parameters are provided
        if (StringUtils.hasText(appKey)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getAppKey, appKey);
        }
        if (StringUtils.hasText(userId)) {
            queryWrapper.eq(CmsDataDownloadTaskPO::getUserId, userId);
        }

        queryWrapper.orderByDesc(CmsDataDownloadTaskPO::getInsertTime)
                   .last("LIMIT 1");

        CmsDataDownloadTaskPO latestTask = taskService.getOne(queryWrapper);
        if (latestTask == null) {
            String authInfo = (StringUtils.hasText(appKey) && StringUtils.hasText(userId)) ? " (with permission control)" : " (without permission control)";
            throw new RuntimeException("No completed task found for business scenario " + businessKey + authInfo);
        }

        // 2. Get download link for this task
        return getFileDownloadUrl(latestTask.getTaskId(), appKey, userId);
    }

    /**
     * Get task basic information (unified permission control)
     * Supports smart permission control: if appKey and userId are provided, permission verification is performed, otherwise all tasks are queried
     *
     * @param taskId task ID
     * @param appKey application key (optional, no permission control when null)
     * @param userId user ID (optional, no permission control when null)
     * @return task basic information
     */
    public java.util.Map<String, Object> getTaskBasicInfo(Long taskId, String appKey, String userId) {
        CmsDataDownloadTaskPO task = taskQueryService.getTaskById(taskId, appKey, userId);
        if (task == null) {
            String errorMsg = (StringUtils.hasText(appKey) && StringUtils.hasText(userId))
                ? "Task does not exist or no permission to access: " + taskId
                : "Task does not exist: " + taskId;
            throw new RuntimeException(errorMsg);
        }

        java.util.Map<String, Object> info = new java.util.HashMap<>();
        info.put("taskId", task.getTaskId());
        info.put("businessKey", task.getBusinessKey());
        info.put("taskStatus", task.getTaskStatus());
        info.put("filePath", task.getFilePath());
        info.put("insertTime", task.getInsertTime());
        info.put("updateTime", task.getUpdateTime());
        info.put("isCompleted", isCompletedStatus(task.getTaskStatus()));
        info.put("hasFile", StringUtils.hasText(task.getFilePath()));
        info.put("authRequired", StringUtils.hasText(appKey) && StringUtils.hasText(userId));

        return info;
    }
}
