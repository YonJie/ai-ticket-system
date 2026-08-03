package com.aiticket.repository;

import com.aiticket.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * 工单数据访问接口。
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID> {
}
