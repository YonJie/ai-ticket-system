package com.aiticket.common;

/**
 * 统一 API 响应结构。
 *
 * @param <T> 业务数据类型
 */
public class Result<T> {

    private boolean success;
    private T data;
    private String message;
    private Integer code;

    public Result() {
    }

    public Result(boolean success, T data, String message, Integer code) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.code = code;
    }

    /**
     * 成功响应（无数据）。
     *
     * @param <T> 业务数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success() {
        return new Result<T>(true, null, null, 200);
    }

    /**
     * 成功响应（带数据）。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<T>(true, data, null, 200);
    }

    /**
     * 成功响应（带数据与消息）。
     *
     * @param data    业务数据
     * @param message 提示消息
     * @param <T>     业务数据类型
     * @return 成功结果
     */
    public static <T> Result<T> success(T data, String message) {
        return new Result<T>(true, data, message, 200);
    }

    /**
     * 失败响应。
     *
     * @param message 错误消息
     * @param <T>     业务数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(String message) {
        return new Result<T>(false, null, message, 500);
    }

    /**
     * 失败响应（自定义状态码）。
     *
     * @param message 错误消息
     * @param code    业务/HTTP 状态码
     * @param <T>     业务数据类型
     * @return 失败结果
     */
    public static <T> Result<T> error(String message, Integer code) {
        return new Result<T>(false, null, message, code);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }
}
