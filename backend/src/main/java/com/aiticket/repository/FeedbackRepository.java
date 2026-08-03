package com.aiticket.repository;

import com.aiticket.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * 反馈数据访问接口。
 */
public interface FeedbackRepository extends JpaRepository<Feedback, UUID> {

    /**
     * 按工单 ID 查询评价。
     *
     * @param ticketId 工单 ID
     * @return 评价 Optional
     */
    Optional<Feedback> findByTicket_Id(UUID ticketId);

    /**
     * 判断工单是否已有评价。
     *
     * @param ticketId 工单 ID
     * @return 是否存在
     */
    boolean existsByTicket_Id(UUID ticketId);
}
