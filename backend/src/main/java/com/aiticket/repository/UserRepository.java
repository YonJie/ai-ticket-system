package com.aiticket.repository;

import com.aiticket.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 用户数据访问接口。
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * 按用户名查询用户。
     *
     * @param username 用户名
     * @return 用户 Optional
     */
    Optional<User> findByUsername(String username);

    /**
     * 判断用户名是否已存在。
     *
     * @param username 用户名
     * @return 是否存在
     */
    boolean existsByUsername(String username);
}
