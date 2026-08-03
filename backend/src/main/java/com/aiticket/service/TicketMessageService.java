package com.aiticket.service;

import com.aiticket.dto.CreateMessageRequest;
import com.aiticket.dto.MessageResponse;
import com.aiticket.entity.Message;
import com.aiticket.entity.Ticket;
import com.aiticket.entity.User;
import com.aiticket.enums.TicketStatus;
import com.aiticket.enums.UserRole;
import com.aiticket.exception.BusinessException;
import com.aiticket.repository.MessageRepository;
import com.aiticket.repository.TicketRepository;
import com.aiticket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 工单留言业务。
 */
@Service
public class TicketMessageService {

    private final TicketRepository ticketRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    /**
     * @param ticketRepository  工单仓库
     * @param messageRepository 留言仓库
     * @param userRepository    用户仓库
     */
    public TicketMessageService(
            TicketRepository ticketRepository,
            MessageRepository messageRepository,
            UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    /**
     * 追加工单留言。客户在 resolved/closed 状态下留言会将状态改回 pending；客服留言不改状态。
     *
     * @param ticketId 工单 ID
     * @param userId   当前用户 ID
     * @param request  留言请求
     * @return 留言响应
     */
    @Transactional
    public MessageResponse addMessage(UUID ticketId, UUID userId, CreateMessageRequest request) {
        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("工单不存在", 404));

        assertCanAccessTicket(ticket, sender);

        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) {
            throw new BusinessException("留言内容不能为空", 400);
        }

        if (sender.getRole() == UserRole.CUSTOMER) {
            TicketStatus status = ticket.getStatus();
            if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
                ticket.setStatus(TicketStatus.PENDING);
                ticketRepository.save(ticket);
            }
        }

        Message message = Message.builder()
                .ticket(ticket)
                .sender(sender)
                .content(content)
                .build();
        Message saved = messageRepository.save(message);
        return MessageResponse.from(saved);
    }

    /**
     * 校验留言权限：客户仅可对自己的工单留言；客服/管理员可对任意工单留言。
     *
     * @param ticket 工单
     * @param user   当前用户
     */
    private void assertCanAccessTicket(Ticket ticket, User user) {
        if (user.getRole() == UserRole.AGENT || user.getRole() == UserRole.ADMIN) {
            return;
        }
        if (user.getRole() != UserRole.CUSTOMER) {
            throw new BusinessException("无权操作该工单", 403);
        }
        UUID customerId = ticket.getCustomer() != null ? ticket.getCustomer().getId() : null;
        if (customerId == null || !customerId.equals(user.getId())) {
            throw new BusinessException("无权操作该工单", 403);
        }
    }
}
