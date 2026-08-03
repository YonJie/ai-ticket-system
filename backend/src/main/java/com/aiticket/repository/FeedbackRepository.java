package com.aiticket.repository;

import com.aiticket.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 反馈数据访问接口。
 */
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {
}
