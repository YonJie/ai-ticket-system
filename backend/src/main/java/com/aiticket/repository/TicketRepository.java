package com.aiticket.repository;

import com.aiticket.entity.Ticket;
import com.aiticket.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 工单数据访问接口。
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    /**
     * 按客户 ID 查询工单，创建时间倒序。
     *
     * @param customerId 客户用户 ID
     * @return 工单列表
     */
    List<Ticket> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId);

    /**
     * 按客户 ID 分页查询工单，创建时间倒序。
     *
     * @param customerId 客户用户 ID
     * @param pageable   分页参数
     * @return 工单分页
     */
    Page<Ticket> findByCustomer_IdOrderByCreatedAtDesc(UUID customerId, Pageable pageable);

    /**
     * 分页查询全部工单，创建时间倒序。
     *
     * @param pageable 分页参数
     * @return 工单分页
     */
    Page<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 按状态分页查询工单，创建时间倒序。
     *
     * @param status   工单状态
     * @param pageable 分页参数
     * @return 工单分页
     */
    Page<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);

    /**
     * 统计指定时间范围内创建的工单数量（用于每日统计）。
     *
     * @param startInclusive 起始时间（含）
     * @param endExclusive   结束时间（不含）
     * @return 工单数量
     */
    long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime startInclusive, LocalDateTime endExclusive);

    /**
     * 按状态分组统计工单数量（用于每日统计）。
     *
     * @return 每项为 [TicketStatus, Long]
     */
    @Query("SELECT t.status, COUNT(t) FROM Ticket t GROUP BY t.status")
    List<Object[]> countGroupByStatus();
}
