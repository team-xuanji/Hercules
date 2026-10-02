package team.magic.flute.hercules.twelve.labors.vo;

import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskDefPO;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;

/**
 * CMS Data Download Task Definition Value Object
 *
 * <p>Value object for CMS data download task definition within the Hercules business
 * access gateway. Provides data transfer and validation for task definition operations.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@Accessors(chain = true)
public class CmsDataDownloadTaskDefVO {

    /**
     * Business scenario key (primary key)
     */
    @NotBlank(message = "Business scenario key cannot be empty")
    private String businessKey;

    /**
     * Business scenario description
     */
    private String businessDesc;

    /**
     * Executor business key
     */
    private String executorRegion;

    /**
     * Plugin group
     */
    private String pluginGroup;

    /**
     * Plugin handler
     */
    private String pluginHandle;

    /**
     * SQL template, requires sqlParamTemplate for PrepareStatement compilation
     */
    private String sqlTemplate;

    /**
     * SQL RDS configuration information.
     * List(RdsInfo)
     */
    private String sqlRdsInfos;

    /**
     * SQL parameter template, JSON format
     * Map(String,String)
     */
    private String sqlParamTemplate;

    /**
     * Export format
     */
    private String exportFormatType;

    /**
     * Export parameters, mainly controls export format and compression format related detail parameters
     */
    private String exportFormatConfig;
    
    /**
     * Export file OSS bucket
     */
    private String ossBucket;

    /**
     * Root path of export files
     */
    private String ossRootPath;

    /**
     * OSS access key
     */
    private String ossAccessId;

    /**
     * OSS access secret
     */
    private String ossAccessSecret;

    /**
     * OSS endpoint
     */
    private String ossEndpoint;

    /**
     * OSS Region
     * e.g.: endpoint = oss-cn-zhangjiakou.aliyuncs.com, region = cn-zhangjiakou
     */
    private String ossRegion;

    /**
     * Export file name prefix
     */
    private String filePrefix;

    public CmsDataDownloadTaskDefPO parse2Po(){
        return new CmsDataDownloadTaskDefPO()
                .setBusinessKey(businessKey)
                .setBusinessDesc(businessDesc)
                .setExecutorRegion(executorRegion)
                .setPluginGroup(pluginGroup)
                .setPluginHandle(pluginHandle)
                .setSqlTemplate(sqlTemplate)
                .setSqlRdsInfos(sqlRdsInfos)
                .setSqlParamTemplate(sqlParamTemplate)
                .setExportFormatType(exportFormatType)
                .setExportFormatConfig(exportFormatConfig)
                .setOssBucket(ossBucket)
                .setOssRootPath(ossRootPath)
                .setOssAccessId(ossAccessId)
                .setOssAccessSecret(ossAccessSecret)
                .setOssEndpoint(ossEndpoint)
                .setOssRegion(ossRegion)
                .setFilePrefix(filePrefix)
                .setInsertTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
    }

}
