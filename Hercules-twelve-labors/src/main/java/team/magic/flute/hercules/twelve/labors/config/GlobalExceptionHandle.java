package team.magic.flute.hercules.twelve.labors.config;


import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.EnumResponseType;
import team.magic.flute.hercules.twelve.labors.exception.BusinessException;
import team.magic.flute.hercules.twelve.labors.exception.TaskConflictException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * Global Exception Handler for Business Access Gateway
 *
 * <p>This global exception handler provides centralized error handling and response formatting
 * for all exceptions that occur within the Hercules business access gateway. It ensures
 * consistent error responses and proper logging for all business operations and system errors.
 *
 * <p><strong>Business Context:</strong> As part of the business access gateway architecture,
 * this handler ensures that all business operations receive consistent error handling and
 * appropriate error responses, supporting:
 * <ul>
 *   <li>Standardized error response formats for API consumers</li>
 *   <li>Comprehensive error logging for monitoring and debugging</li>
 *   <li>Business-specific exception handling for domain operations</li>
 *   <li>Validation error handling for input parameters</li>
 * </ul>
 *
 * <p><strong>Exception Categories:</strong>
 * <ul>
 *   <li>Business exceptions from domain operations</li>
 *   <li>Validation exceptions from input parameter validation</li>
 *   <li>System exceptions from infrastructure components</li>
 *   <li>Task-specific exceptions from execution framework</li>
 * </ul>
 *
 * <p><strong>Error Response Strategy:</strong> All exceptions are converted to standardized
 * BaseResponse objects with appropriate error codes and messages, ensuring consistent
 * error handling across all business access gateway endpoints.
 *
 * @author Hercules Team
 * @version 1.0
 * @since 1.0
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandle {
    /**
     * Parameter validation exception handler
     */
    @ExceptionHandler(value = {Exception.class})
    public BaseResponse onException(Exception exception) {
        log.error("Unknown exception", exception);
        return BaseResponse.send(EnumResponseType.DEFAULT_ERROR, ExceptionUtils.getRootCauseMessage(exception));
    }

    @ExceptionHandler(value = {BusinessException.class, InternalError.class, TaskConflictException.class})
    public BaseResponse onException(BusinessException exception){
        log.error("Business module exception", exception);
        return BaseResponse.send(EnumResponseType.DEFAULT_ERROR, ExceptionUtils.getRootCauseMessage(exception));
    }

    /**
     * Parameter validation exception handler
     */
    @ExceptionHandler(value = {MethodArgumentNotValidException.class})
    public BaseResponse onException(MethodArgumentNotValidException exception) {
        BindingResult bindingResult = exception.getBindingResult();
        return getRestResponse(bindingResult);
    }

    @ExceptionHandler(value = {ConstraintViolationException.class})
    public BaseResponse onException(ConstraintViolationException exception) {
        log.error(exception.getMessage());
        return BaseResponse.send(EnumResponseType.ARGUMENT_NOT_VALID, ExceptionUtils.getRootCauseMessage(exception));
    }

    /**
     * Parameter validation exception handler
     */
    @ExceptionHandler(value = {IllegalArgumentException.class})
    public BaseResponse onException(IllegalArgumentException exception) {
        log.error("Parameter exception", exception);
        return BaseResponse.send(EnumResponseType.ARGUMENT_NOT_VALID, ExceptionUtils.getRootCauseMessage(exception));
    }

    /**
     * Parameter validation exception handler
     */
    @ExceptionHandler(value = {UnsupportedOperationException.class})
    public BaseResponse onException(UnsupportedOperationException exception) {
        log.error("Unsupported operation", exception);
        return BaseResponse.send(EnumResponseType.DEFAULT_ERROR, ExceptionUtils.getRootCauseMessage(exception));
    }


    /**
     * Parameter validation exception handler
     */
    @ExceptionHandler(value = {BindException.class})
    public BaseResponse onException(BindException exception) {
        BindingResult bindingResult = exception.getBindingResult();
        return getRestResponse(bindingResult);
    }

    private BaseResponse getRestResponse(BindingResult bindingResult) {
        String errors = bindingResult.getFieldErrors().stream()
                .map(e -> String.format("Property [%s] exception [%s]\n", e.getField(), e.getDefaultMessage()))
                .collect(Collectors.joining(";"));
        return BaseResponse.send(EnumResponseType.ARGUMENT_NOT_VALID, errors);
    }

}
