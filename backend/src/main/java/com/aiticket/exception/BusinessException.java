package com.aiticket.exception;

/**
 * 业务异常，由全局异常处理器统一转换为 Result 响应。
 */
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 使用默认错误码创建业务异常。
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 使用自定义错误码创建业务异常。
     *
     * @param message 错误消息
     * @param code    错误码
     */
    public BusinessException(String message, Integer code) {
        super(message);
        this.code = code;
    }

    /**
     * 获取错误码。
     *
     * @return 错误码
     */
    public Integer getCode() {
        return code;
    }
}
