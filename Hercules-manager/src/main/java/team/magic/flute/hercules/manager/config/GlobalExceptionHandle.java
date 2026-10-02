package team.magic.flute.hercules.manager.config;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import team.magic.flute.hercules.common.http.BaseResponse;
import team.magic.flute.hercules.common.http.EnumResponseType;
import team.magic.flute.hercules.manager.exception.BusinessException;
import team.magic.flute.hercules.manager.exception.InternalError;
import team.magic.flute.hercules.manager.exception.PluginRegisterFailedException;
import team.magic.flute.hercules.manager.exception.TaskConflictException;

import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandle {

    @ExceptionHandler(value = {RuntimeException.class})
    public BaseResponse onException(Exception exception) {
        log.error("Unknown Exception", exception);
        return BaseResponse.send(EnumResponseType.DEFAULT_ERROR, ExceptionUtils.getRootCauseMessage(exception));
    }

    @ExceptionHandler(value = {BusinessException.class, InternalError.class, TaskConflictException.class})
    public BaseResponse onException(BusinessException exception){
        log.error("Business module exception", exception);
        return BaseResponse.send(EnumResponseType.DEFAULT_ERROR, ExceptionUtils.getRootCauseMessage(exception));
    }

    @ExceptionHandler(value = {PluginRegisterFailedException.class})
    public BaseResponse onException(PluginRegisterFailedException exception){
        log.error("Plugin registration operation exception", exception);
        return BaseResponse.send(EnumResponseType.PLUGIN_REGISTER_ERROR, ExceptionUtils.getRootCauseMessage(exception));
    }

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


    @ExceptionHandler(value = {IllegalArgumentException.class})
    public BaseResponse onException(IllegalArgumentException exception) {
        log.error("Parameter exception", exception);
        return BaseResponse.send(EnumResponseType.ARGUMENT_NOT_VALID, ExceptionUtils.getRootCauseMessage(exception));
    }

    @ExceptionHandler(value = {UnsupportedOperationException.class})
    public BaseResponse onException(UnsupportedOperationException exception) {
        log.error("Unsupported operation", exception);
        return BaseResponse.send(EnumResponseType.DEFAULT_ERROR, ExceptionUtils.getRootCauseMessage(exception));
    }

    @ExceptionHandler(value = {BindException.class})
    public BaseResponse onException(BindException exception) {
        BindingResult bindingResult = exception.getBindingResult();
        return getRestResponse(bindingResult);
    }

    private BaseResponse getRestResponse(BindingResult bindingResult) {
        String errors = bindingResult.getFieldErrors().stream()
                .map(e -> String.format("Attribute [%s] is abnormal [%s]\n", e.getField(), e.getDefaultMessage()))
                .collect(Collectors.joining(";"));
        return BaseResponse.send(EnumResponseType.ARGUMENT_NOT_VALID, errors);
    }

}
