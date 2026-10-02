package team.magic.flute.hercules.executor.feign.bean;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;


@Data
public class ApiConfigBean {


    /*
    * EXAMPLE:
    * api:
    * app-open-api:
    * host: http://xx-xx.xxxxx.com
    * name: xx-xx-xx-xx
    * version: 1.0
    * caller: xx
    * sign: xxx
    * */
    private String host;
    private String name;
    private String version;
    private String caller;
    private String sign;
    private Map<String, String> headers;

    public String getUrl() {
        if (host == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(host);
        if (StringUtils.isNotBlank(name)) {
            builder.append("/")
                    .append(name);
        }
        if (StringUtils.isNotBlank(version)) {
            builder.append("/")
                    .append(version);
        }
        return builder.toString();
    }
}
