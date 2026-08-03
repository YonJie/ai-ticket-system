package com.aiticket.service;

import com.aiticket.dto.CreateFeedbackRequest;
import com.aiticket.dto.FeedbackResponse;
import com.aiticket.entity.Feedback;
import com.aiticket.entity.Ticket;
import com.aiticket.entity.User;
import com.aiticket.enums.TicketStatus;
import com.aiticket.enums.UserRole;
import com.aiticket.exception.BusinessException;
import com.aiticket.repository.FeedbackRepository;
import com.aiticket.repository.TicketRepository;
import com.aiticket.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 工单评价业务。
 */
@Service
public class TicketFeedbackService {

    private final TicketRepository ticketRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;

    /**
     * @param ticketRepository   工单仓库
     * @param feedbackRepository 评价仓库
     * @param userRepository     用户仓库
     */
    public TicketFeedbackService(
            TicketRepository ticketRepository,
            FeedbackRepository feedbackRepository,
            UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.feedbackRepository = feedbackRepository;
        this.userRepository = userRepository;
    }

    /**
     * 提交工单评价。仅客户、仅 resolved 状态、每个工单仅可评价一次。
     *
     * @param ticketId 工单 ID
     * @param userId   当前用户 ID
     * @param request  评价请求
     * @return 评价响应
     */
    @Transactional
    public FeedbackResponse submitFeedback(UUID ticketId, UUID userId, CreateFeedbackRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));

        if (user.getRole() != UserRole.CUSTOMER) {
            throw new BusinessException("仅客户可提交评价", 403);
        }

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("工单不存在", 404));

        UUID customerId = ticket.getCustomer() != null ? ticket.getCustomer().getId() : null;
        if (customerId == null || !customerId.equals(user.getId())) {
            throw new BusinessException("无权评价该工单", 403);
        }

        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new BusinessException("仅已解决的工单可评价", 400);
        }

        if (feedbackRepository.existsByTicket_Id(ticketId)) {
            throw new BusinessException("该工单已评价", 400);
        }

        String comment = request.getComment();
        if (comment != null) {
            comment = comment.trim();
            if (comment.isEmpty()) {
                comment = null;
            }
        }

        Feedback feedback = Feedback.builder()
                .ticket(ticket)
                .rating(request.getRating())
                .comment(comment)
                .build();

        try {
            Feedback saved = feedbackRepository.save(feedback);
            return FeedbackResponse.from(saved);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("该工单已评价", 400);
        }
    }
}
