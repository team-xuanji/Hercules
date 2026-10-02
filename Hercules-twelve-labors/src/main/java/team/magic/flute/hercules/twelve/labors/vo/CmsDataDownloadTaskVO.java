package team.magic.flute.hercules.twelve.labors.vo;

import com.aliyun.oss.common.utils.StringUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import team.magic.flute.hercules.twelve.labors.business.export.entity.RdsExportContext;
import team.magic.flute.hercules.twelve.labors.dao.entity.CmsDataDownloadTaskPO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * CMS Data Download Task Value Object
 *
 * <p>Value object for CMS data download task within the Hercules business
 * access gateway. Provides data transfer for task information display and operations.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
@RequiredArgsConstructor
public class CmsDataDownloadTaskVO {
    @JsonIgnore
    private final CmsDataDownloadTaskPO taskPO;

    @JsonSerialize(using = ToStringSerializer.class)
    public Long getTaskId() {
        return taskPO.getTaskId();
    }

    public String getBusinessKey() {
        return taskPO.getBusinessKey();
    }
    public RdsExportContext getContext() {
        return taskPO.getContext();
    }
    public String getFileName() {
        String path = taskPO.getFilePath();
        if(StringUtils.isNullOrEmpty(path)){
            return path;
        }
        return path.substring(path.lastIndexOf("/")+1);
    }

    public String getTaskStatus() {
        return taskPO.getTaskStatus();
    }
    public String getAppKey() {
        return taskPO.getAppKey();
    }
    public String getUserId() {
        return taskPO.getUserId();
    }
    public String getOperatorSource() {
        return taskPO.getOperatorSource();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getInsertTime() {
        return taskPO.getInsertTime();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getUpdateTime() {
        return taskPO.getUpdateTime();
    }

    public String getUserName(){
        if(StringUtils.isNullOrEmpty(taskPO.getUserId()) || StringUtils.isNullOrEmpty(taskPO.getAppKey())){
            return "System Administrator";
        }
        if(StringUtils.isNullOrEmpty(taskPO.getUserName())){
            return "Unknown User";
        }
        return taskPO.getUserName();
    }
}
