package team.magic.flute.hercules.plugin.data.export.entity;

import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RdsExportContext {
    private List<RdsInfo> rdsInfos;
    private String rdsQuery;

    private Boolean duckdbMysqlExperimentalFilterPushdown;

    private String ossPath;
    private String bucketName;
    private String ossKey;
    private String ossSecret;
    private String ossRegion;
    private String ossEndpoint;

    /** excel,csv,json */
    private String exportFormat;

    private Map<String, String> formatConfig;
}
