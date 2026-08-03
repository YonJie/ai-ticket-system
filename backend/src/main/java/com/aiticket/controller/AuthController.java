package com.aiticket.controller;

import com.aiticket.common.Result;
import com.aiticket.dto.LoginRequest;
import com.aiticket.dto.LoginResponse;
import com.aiticket.dto.RegisterRequest;
import com.aiticket.dto.UserResponse;
import com.aiticket.exception.BusinessException;
import com.aiticket.interceptor.JwtInterceptor;
import com.aiticket.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.UUID;

/**
 * 认证相关接口：注册、登录、当前用户。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    /**
     * @param authService 认证服务
     */
    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 用户信息
     */
    @PostMapping("/register")
    public Result<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return Result.success(authService.register(request));
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return token 与用户信息
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 获取当前登录用户信息（需 JWT 鉴权）。
     *
     * @param request HTTP 请求（含拦截器写入的 userId）
     * @return 当前用户信息
     */
    @GetMapping("/me")
    public Result<UserResponse> me(HttpServletRequest request) {
        Object attr = request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        if (!(attr instanceof UUID)) {
            throw new BusinessException("未登录", 401);
        }
        return Result.success(authService.getCurrentUser((UUID) attr));
    }
}
