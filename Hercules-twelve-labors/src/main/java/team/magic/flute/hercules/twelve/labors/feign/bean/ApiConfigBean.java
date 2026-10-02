package team.magic.flute.hercules.twelve.labors.feign.bean;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

/**
 * API Configuration Bean for Business Access Gateway
 *
 * <p>Configuration bean containing API connection parameters for external
 * service integration within the Hercules business access gateway.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@Data
public class ApiConfigBean {


    /*
    * api:
    * app-open-api:
    * host: http://qa-sapi.shuyun.com
    * name: etl-app-open-api
    * version: 1.0
    * caller: test
    * sign: 74657yKcwK48pARhJJK5B
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
