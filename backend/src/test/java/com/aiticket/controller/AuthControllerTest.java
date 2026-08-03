package com.aiticket.controller;

import com.aiticket.config.SecurityConfig;
import com.aiticket.dto.LoginRequest;
import com.aiticket.dto.LoginResponse;
import com.aiticket.dto.RegisterRequest;
import com.aiticket.dto.UserResponse;
import com.aiticket.enums.UserRole;
import com.aiticket.exception.GlobalExceptionHandler;
import com.aiticket.service.AuthService;
import com.aiticket.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController MockMvc 切片测试：注册、登录、参数校验。
 */
@WebMvcTest(controllers = AuthController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    /** WebConfig / JwtInterceptor 切片加载所需 */
    @MockBean
    private JwtUtil jwtUtil;

    /**
     * 注册成功应返回用户信息。
     */
    @Test
    void register_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse user = UserResponse.builder()
                .id(userId)
                .username("newuser")
                .role(UserRole.CUSTOMER)
                .createdAt(LocalDateTime.of(2026, 8, 3, 12, 0))
                .build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(user);

        RegisterRequest body = RegisterRequest.builder()
                .username("newuser")
                .password("123456")
                .role("customer")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.id").value(userId.toString()))
                .andExpect(jsonPath("$.data.username").value("newuser"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

        verify(authService).register(any(RegisterRequest.class));
    }

    /**
     * 登录成功应返回 token 与用户。
     */
    @Test
    void login_success() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse user = UserResponse.builder()
                .id(userId)
                .username("customer")
                .role(UserRole.CUSTOMER)
                .createdAt(LocalDateTime.of(2026, 8, 3, 12, 0))
                .build();
        LoginResponse loginResponse = LoginResponse.builder()
                .token("mock-jwt-token")
                .user(user)
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(loginResponse);

        LoginRequest body = LoginRequest.builder()
                .username("customer")
                .password("123456")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.data.user.username").value("customer"));

        verify(authService).login(any(LoginRequest.class));
    }

    /**
     * 注册缺少必填字段应返回 400。
     */
    @Test
    void register_validationFailed() throws Exception {
        RegisterRequest body = RegisterRequest.builder()
                .username("")
                .password("123")
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }
}
