package com.aiticket.interceptor;

import com.aiticket.common.Result;
import com.aiticket.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

/**
 * JWT 鉴权拦截器：校验 Authorization Bearer token，并将 userId 写入 request 属性。
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String USER_ID_ATTR = "userId";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /**
     * @param jwtUtil      JWT 工具
     * @param objectMapper JSON 序列化
     */
    public JwtInterceptor(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    /**
     * 校验请求头中的 JWT，成功则将 userId 存入 request；失败返回 401。
     *
     * @param request  当前请求
     * @param response 当前响应
     * @param handler  处理器
     * @return 是否放行
     * @throws Exception 写入响应异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        String authorization = request.getHeader(AUTH_HEADER);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(response, "未登录或令牌缺失");
            return false;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            writeUnauthorized(response, "未登录或令牌缺失");
            return false;
        }

        try {
            Long userId = jwtUtil.getUserIdFromToken(token);
            request.setAttribute(USER_ID_ATTR, userId);
            return true;
        } catch (Exception ex) {
            writeUnauthorized(response, "令牌无效或已过期");
            return false;
        }
    }

    /**
     * 写入 401 JSON 响应。
     *
     * @param response 当前响应
     * @param message  错误消息
     * @throws Exception 写入异常
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Result.error(message, 401));
    }
}
