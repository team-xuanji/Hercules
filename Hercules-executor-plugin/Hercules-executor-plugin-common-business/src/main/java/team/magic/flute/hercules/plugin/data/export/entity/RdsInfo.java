package team.magic.flute.hercules.plugin.data.export.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RdsInfo {
    private String rdsUrl;
    private String rdsAttachName;
    private String rdsPort;
    private String rdsDatabaseName;
    private String rdsUser;
    private String rdsPassword;
}
