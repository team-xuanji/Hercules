package team.magic.flute.hercules.manager.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.RequiredArgsConstructor;
import org.apache.fory.util.StringUtils;
import org.springframework.format.annotation.DateTimeFormat;
import team.magic.flute.hercules.common.http.HerculesRunnableTaskInfo;
import team.magic.flute.hercules.manager.dao.po.HerculesFailedTaskPo;
import team.magic.flute.hercules.manager.util.DTOConvertUtils;

import java.time.LocalDateTime;

@RequiredArgsConstructor
public class HerculesRecoverTaskInfoVO {
    @JsonIgnore
    private final HerculesFailedTaskPo failedTaskPo;
    @JsonIgnore
    private final String encryptKey;
    @JsonIgnore
    private final String encryptIV;

    public Long getId(){
        return failedTaskPo.getId();
    }

    public String getPluginGroup(){
        return failedTaskPo.getPluginGroup();
    }

    public String getPluginHandle(){
        return failedTaskPo.getPluginHandle();
    }

    public String getExecutorRegion(){
        return failedTaskPo.getExecutorRegion();
    }

    public String getDescription(){
        return failedTaskPo.getDescription();
    }

    public String getErrorInfo(){
        return failedTaskPo.getErrorInfo();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getNextProcessTime(){
        return failedTaskPo.getNextProcessTime();
    }

    public Integer getBucketId(){
        return failedTaskPo.getBucketId();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getInsertTime(){
        return failedTaskPo.getInsertTime();
    }

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    public LocalDateTime getUpdateTime(){
        return failedTaskPo.getUpdateTime();
    }

    public HerculesRunnableTaskInfo getTaskInfo(){
        return DTOConvertUtils.parse2RunnableTaskInfo(failedTaskPo.getTaskInfo(), StringUtils.isNotBlank(encryptKey) && StringUtils.isNotBlank(encryptIV), encryptKey, encryptIV);
    }
}
