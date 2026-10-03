package team.magic.flute.hercules.executor.vo;

import lombok.Data;
import lombok.experimental.Accessors;
import team.magic.flute.hercules.common.executor.HerculesExecutorHeartbeatInfo;
import team.magic.flute.hercules.common.http.HerculesHttpCompressType;
import team.magic.flute.hercules.common.util.AESUtils;
import team.magic.flute.hercules.common.util.BinaryCompressUtils;
import team.magic.flute.hercules.common.util.ForyUtils;

@Data
@Accessors(chain = true)
public class ExecutorInfoReportRequestVO {

    private String aesIV;
    private byte[] reportInfo;
    private int originalLength;
    private String compressTypeStr;

    public void fillRequestVO(HerculesExecutorHeartbeatInfo info,String aesKey) {
        byte[] data = ForyUtils.serialize(info);
        byte[] compressed = BinaryCompressUtils.compress(data, HerculesHttpCompressType.ZSTD);
        String aesIV = AESUtils.generateIV();
        compressed = AESUtils.encrypt(compressed, aesKey, aesIV);
        this.aesIV = aesIV;
        this.originalLength = data.length;
        this.reportInfo = compressed;
        this.compressTypeStr = HerculesHttpCompressType.ZSTD.name();
    }
}
