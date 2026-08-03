package com.aiticket.repository;

import com.aiticket.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * 消息数据访问接口。
 */
public interface MessageRepository extends JpaRepository<Message, UUID> {

    /**
     * 按工单 ID 查询留言，创建时间升序。
     *
     * @param ticketId 工单 ID
     * @return 留言列表
     */
    List<Message> findByTicket_IdOrderByCreatedAtAsc(UUID ticketId);
}
