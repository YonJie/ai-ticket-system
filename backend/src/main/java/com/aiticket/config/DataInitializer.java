package com.aiticket.config;

import com.aiticket.entity.User;
import com.aiticket.enums.UserRole;
import com.aiticket.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化演示用户数据。
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String DEMO_PASSWORD = "123456";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * @param userRepository  用户仓库
     * @param passwordEncoder 密码编码器
     */
    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 幂等插入三角色演示用户。
     *
     * @param args 启动参数
     */
    @Override
    public void run(String... args) {
        ensureUser("customer", UserRole.CUSTOMER);
        ensureUser("agent", UserRole.AGENT);
        ensureUser("admin", UserRole.ADMIN);
        log.info("Demo users ready (password: {})", DEMO_PASSWORD);
    }

    /**
     * 若用户名不存在则创建演示用户。
     *
     * @param username 用户名
     * @param role     角色
     */
    private void ensureUser(String username, UserRole role) {
        if (userRepository.existsByUsername(username)) {
            return;
        }
        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(DEMO_PASSWORD))
                .role(role)
                .build();
        userRepository.save(user);
        log.info("Created demo user: {} ({})", username, role);
    }
}
