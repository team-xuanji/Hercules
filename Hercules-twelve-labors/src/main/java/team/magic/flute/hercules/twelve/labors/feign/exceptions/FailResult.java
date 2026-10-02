package team.magic.flute.hercules.twelve.labors.feign.exceptions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * API Failure Result for Business Access Gateway
 *
 * <p>Data transfer object for capturing API failure information in Feign
 * client error responses within the Hercules business access gateway.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FailResult {
    private int code ;
    private String msg ;
    private String remoteUrl ;
    private boolean success ;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }
}
