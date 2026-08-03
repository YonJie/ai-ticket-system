package com.aiticket.exception;

import com.aiticket.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器：统一返回 Result.error(message)。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常；按业务码映射常见 HTTP 状态（如 409 Conflict）。
     *
     * @param ex 业务异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        HttpStatus httpStatus = resolveHttpStatus(ex.getCode());
        return ResponseEntity.status(httpStatus).body(Result.error(ex.getMessage(), ex.getCode()));
    }

    /**
     * 将业务错误码映射为 HTTP 状态。
     *
     * @param code 业务错误码
     * @return HTTP 状态
     */
    private HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.OK;
        }
        if (code == 400) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code == 401) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == 403) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == 404) {
            return HttpStatus.NOT_FOUND;
        }
        if (code == 409) {
            return HttpStatus.CONFLICT;
        }
        if (code >= 500) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.OK;
    }

    /**
     * 处理 @RequestBody 参数校验失败。
     *
     * @param ex 校验异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Result.error(message.isEmpty() ? "参数校验失败" : message, 400);
    }

    /**
     * 处理表单/查询参数绑定校验失败。
     *
     * @param ex 绑定异常
     * @return 统一错误响应
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleBindException(BindException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return Result.error(message.isEmpty() ? "参数校验失败" : message, 400);
    }

    /**
     * 处理单参数约束校验失败。
     *
     * @param ex 约束违反异常
     * @return 统一错误响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        return Result.error(message.isEmpty() ? "参数校验失败" : message, 400);
    }

    /**
     * 处理缺少请求参数。
     *
     * @param ex 缺少参数异常
     * @return 统一错误响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
        return Result.error("缺少参数: " + ex.getParameterName(), 400);
    }

    /**
     * 处理非法参数。
     *
     * @param ex 非法参数异常
     * @return 统一错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<Void> handleIllegalArgument(IllegalArgumentException ex) {
        return Result.error(ex.getMessage());
    }

    /**
     * 处理未捕获异常。
     *
     * @param ex 异常
     * @return 统一错误响应
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleException(Exception ex) {
        String message = ex.getMessage();
        return Result.error(message == null || message.trim().isEmpty() ? "服务器内部错误" : message);
    }

    /**
     * 格式化字段错误信息。
     *
     * @param fieldError 字段错误
     * @return 可读消息
     */
    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
