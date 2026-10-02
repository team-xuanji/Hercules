package team.magic.flute.hercules.common.http;

/**
 * 2021-12-27  CL
 **/
public enum EnumResponseType {

    /**
     * Return ok
     */
    SUCCESS(200, "ok", "success"),

    /**
     * Parameter exception
     */
    ARGUMENT_NOT_VALID(300, "argument_not_valid", "argument_not_valid"),

    /**
     * Plugin registration error
     */
    PLUGIN_REGISTER_ERROR(405, "plugin_register_error", "Plugin registration exception"),

    /**
     * Failure
     */
    DEFAULT_ERROR(500, "unknown error", "[service] unknown error");


    EnumResponseType(int code, String message, String describe) {
        this.code = code;
        this.message = message;
        this.describe = describe;
    }

    private int code;
    private String message;
    private String describe;

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescribe() {
        return describe;
    }
}
