package team.magic.flute.hercules.common.http;

import org.apache.commons.lang3.StringUtils;

/**
 * Standard HTTP Response Wrapper
 *
 * <p>This class provides a standardized response format for all HTTP APIs in the Hercules system.
 * It encapsulates the response code, message, and data payload in a consistent structure that
 * clients can reliably parse and handle.
 *
 * <p>Response Structure:
 * <pre>{@code
 * {
 *   "code": 200,           // HTTP-like status code
 *   "msg": "success",      // Human-readable message
 *   "data": { ... }        // Response payload (optional)
 * }
 * }</pre>
 *
 * <p>Common usage patterns:
 * <pre>{@code
 * // Success with data
 * return BaseResponse.success(userData);
 *
 * // Success without data
 * return BaseResponse.success();
 *
 * // Error with custom message
 * return BaseResponse.fail("User not found");
 *
 * // Error with predefined type
 * return BaseResponse.send(EnumResponseType.ARGUMENT_NOT_VALID);
 * }</pre>
 *
 * <p>The class supports method chaining for convenient response building and
 * provides type safety through generics for the data payload.
 *
 * @param <T> the type of the response data payload
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
public class BaseResponse<T> {
    /**
     * Response status code (HTTP-like).
     * Typically 200 for success, 300+ for client errors, 500+ for server errors.
     */
    private int code;

    /**
     * Human-readable response message.
     * Provides additional context about the response status.
     */
    private String msg;

    /**
     * Response data payload.
     * Contains the actual response data when applicable.
     */
    private T data;

    /**
     * Default constructor creating a successful response.
     * Sets code to 200 and message to "success".
     */
    public BaseResponse() {
        this.code = EnumResponseType.SUCCESS.getCode();
        this.msg = EnumResponseType.SUCCESS.getMessage();
    }

    /**
     * Constructor creating a successful response with data.
     *
     * @param data the response data payload
     */
    public BaseResponse(T data) {
        this.code = EnumResponseType.SUCCESS.getCode();
        this.msg = EnumResponseType.SUCCESS.getMessage();
        this.data = data;
    }

    /**
     * Create a successful response without data.
     *
     * @param <T> the response data type
     * @return a successful BaseResponse with code 200 and "success" message
     */
    public static <T> BaseResponse<T> success() {
        return new BaseResponse<>();
    }

    /**
     * Create a failure response with default error message.
     *
     * @param <T> the response data type
     * @return a failure BaseResponse with code 500 and default error message
     */
    public static <T> BaseResponse<T> fail() {
        BaseResponse<T> resp = new BaseResponse<>();
        resp.setCode(EnumResponseType.DEFAULT_ERROR.getCode());
        resp.setMsg(EnumResponseType.DEFAULT_ERROR.getMessage());
        return resp;
    }

    /**
     * Create a failure response with custom error message.
     *
     * @param <T> the response data type
     * @param message the custom error message
     * @return a failure BaseResponse with code 500 and the provided message
     */
    public static <T> BaseResponse<T> fail(String message) {
        BaseResponse<T> resp = new BaseResponse<>();
        resp.setCode(EnumResponseType.DEFAULT_ERROR.getCode());
        resp.setMsg(message);
        return resp;
    }

    /**
     * Create a successful response with data payload.
     *
     * @param <T> the response data type
     * @param data the response data payload
     * @return a successful BaseResponse containing the provided data
     */
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setData(data);
        return response;
    }

    /**
     * Create a response based on a predefined response type.
     *
     * @param <T> the response data type
     * @param type the predefined response type containing code and message
     * @return a BaseResponse with the code and message from the provided type
     */
    public static <T> BaseResponse<T> send(EnumResponseType type) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(type.getCode());
        response.setMsg(type.getMessage());
        return response;
    }

    public static <T> BaseResponse<T> send(EnumResponseType type, String msg) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(type.getCode());
        response.setMsg(msg);
        return response;
    }

    public static <T> BaseResponse<T> send(Throwable throwable) {
        BaseResponse<T> response = new BaseResponse<>();
        setResponseMessage(throwable, response);
        response.setData(null);
        return response;
    }

    private static <T> void setResponseMessage(Throwable throwable, BaseResponse<T> response) {
        final String message = throwable.getMessage();
        if (!StringUtils.isBlank(message)) {
            response.setMsg(message);
        } else {
            response.setMsg(EnumResponseType.DEFAULT_ERROR.getMessage());
        }
        response.setCode(EnumResponseType.DEFAULT_ERROR.getCode());
    }

    public static <T> BaseResponse<T> send(Throwable throwable, T data) {
        BaseResponse<T> response = new BaseResponse<>();
        setResponseMessage(throwable, response);
        response.setCode(EnumResponseType.DEFAULT_ERROR.getCode());
        response.setData(data);
        return response;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getData() {
        return this.data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
