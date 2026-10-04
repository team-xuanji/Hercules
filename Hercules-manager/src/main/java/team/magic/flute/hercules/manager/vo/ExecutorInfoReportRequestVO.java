package team.magic.flute.hercules.manager.vo;


import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.executor.ExecutorProcessHandleWhiteList;
import team.magic.flute.hercules.common.executor.HerculesExecutorHeartbeatInfo;
import team.magic.flute.hercules.common.http.HerculesHttpCompressType;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.common.util.BinaryCompressUtils;
import team.magic.flute.hercules.common.util.ForyUtils;
import team.magic.flute.hercules.manager.dao.po.HerculesExecutorInfo;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ExecutorInfoReportRequestVO {


    @NotBlank(message = "AesIV can not be empty")
    private String aesIV;
    @NotNull
    private byte[] reportInfo;
    private int originalLength;
    @NotBlank(message = "compressTypeStr can not be empty")
    private String compressTypeStr;

    public HerculesExecutorInfo parse2Po(String aesKey){
        byte [] data = AESUtils.decrypt(reportInfo,aesKey,aesIV);
        HerculesHttpCompressType compressType = HerculesHttpCompressType.valueOf(compressTypeStr);
        data = BinaryCompressUtils.deCompress(data,compressType,originalLength);
        HerculesExecutorHeartbeatInfo heartbeatInfo = ForyUtils.deserialize(data, HerculesExecutorHeartbeatInfo.class);
        return new HerculesExecutorInfo()
                .setIdentityId(heartbeatInfo.getExecutorIdentityId())
                .setExecutorId(heartbeatInfo.getExecutorId())
                .setExecutorRegion(heartbeatInfo.getExecutorRegion())
                .setExecutorRegionDesc(heartbeatInfo.getExecutorRegionDesc())
                .setExecutorMaxSlot(heartbeatInfo.getExecutorMaxSlot())
                .setExecutorAvailableSlot(heartbeatInfo.getExecutorAvailableSlot())
                .setEnableDuckdb(heartbeatInfo.isEnableDuckdb())
                .setExecutorLoadPluginInfo(heartbeatInfo.getExecutorLoadPluginInfo())
                .setExecutorPluginHandleWhiteList(new ExecutorProcessHandleWhiteList().setWhiteList(heartbeatInfo.getExecutorPluginHandleWhiteList()))
                .setInsertTime(LocalDateTime.now())
                .setUpdateTime(LocalDateTime.now());
    }
}
