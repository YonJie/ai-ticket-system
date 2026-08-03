package com.aiticket.support;

import com.aiticket.interceptor.JwtInterceptor;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

/**
 * MockMvc 鉴权辅助：写入 Authorization 头与 userId 属性。
 * 切片测试中需 stub JwtUtil.getUserIdFromToken 返回同一 userId。
 */
public final class MockAuth {

    private MockAuth() {
    }

    /**
     * 模拟已登录用户：设置 Bearer token，并预写入 userId。
     *
     * @param userId 用户 ID
     * @return RequestPostProcessor
     */
    public static RequestPostProcessor withUserId(UUID userId) {
        return request -> {
            request.addHeader("Authorization", "Bearer test-token");
            request.setAttribute(JwtInterceptor.USER_ID_ATTR, userId);
            return request;
        };
    }
}
