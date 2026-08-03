package com.aiticket.util;

import com.aiticket.exception.BusinessException;
import com.aiticket.interceptor.JwtInterceptor;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * 从 HTTP 请求中解析当前登录用户信息。
 */
public final class AuthRequestUtils {

    private AuthRequestUtils() {
    }

    /**
     * 从 request 属性读取当前用户 ID。
     *
     * @param request HTTP 请求
     * @return 用户 ID
     */
    public static UUID requireUserId(HttpServletRequest request) {
        Object value = request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        if (value == null) {
            throw new BusinessException("未登录", 401);
        }
        if (value instanceof UUID) {
            return (UUID) value;
        }
        try {
            return UUID.fromString(String.valueOf(value));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("无效的用户身份", 401);
        }
    }
}
