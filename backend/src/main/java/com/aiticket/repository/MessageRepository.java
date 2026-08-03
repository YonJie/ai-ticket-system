package com.aiticket.repository;

import com.aiticket.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 消息数据访问接口。
 */
public interface MessageRepository extends JpaRepository<Message, UUID> {
}
