package com.aiticket.service;

import com.aiticket.dto.LoginRequest;
import com.aiticket.dto.LoginResponse;
import com.aiticket.dto.RegisterRequest;
import com.aiticket.dto.UserResponse;
import com.aiticket.entity.User;
import com.aiticket.enums.UserRole;
import com.aiticket.exception.BusinessException;
import com.aiticket.repository.UserRepository;
import com.aiticket.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 认证业务：注册、登录、查询当前用户。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * @param userRepository  用户仓库
     * @param passwordEncoder 密码编码器
     * @param jwtUtil         JWT 工具
     */
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 注册新用户。
     *
     * @param request 注册请求
     * @return 用户信息（不含密码）
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在", 400);
        }

        UserRole role = parseRole(request.getRole());
        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .build();
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }

    /**
     * 用户登录，校验密码并签发 JWT。
     *
     * @param request 登录请求
     * @return token 与用户信息
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new BusinessException("用户名或密码错误", 401));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException("用户名或密码错误", 401);
        }

        String token = jwtUtil.generateToken(user.getId());
        return LoginResponse.builder()
                .token(token)
                .user(UserResponse.from(user))
                .build();
    }

    /**
     * 按用户 ID 查询当前用户。
     *
     * @param userId 用户 ID
     * @return 用户信息（不含密码）
     */
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {
        if (userId == null) {
            throw new BusinessException("未登录", 401);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));
        return UserResponse.from(user);
    }

    /**
     * 解析角色字符串，默认 CUSTOMER；非法值抛出业务异常。
     *
     * @param role 角色字符串
     * @return UserRole
     */
    private UserRole parseRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return UserRole.CUSTOMER;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("无效的角色: " + role, 400);
        }
    }
}
