package com.aiticket.service;

import com.aiticket.dto.FeedbackResponse;
import com.aiticket.dto.MessageResponse;
import com.aiticket.dto.PageResult;
import com.aiticket.dto.TicketCreateRequest;
import com.aiticket.dto.TicketDetailResponse;
import com.aiticket.dto.TicketResponse;
import com.aiticket.dto.TicketUpdateRequest;
import com.aiticket.entity.Feedback;
import com.aiticket.entity.Message;
import com.aiticket.entity.Ticket;
import com.aiticket.entity.User;
import com.aiticket.enums.TicketStatus;
import com.aiticket.enums.UserRole;
import com.aiticket.exception.BusinessException;
import com.aiticket.repository.FeedbackRepository;
import com.aiticket.repository.MessageRepository;
import com.aiticket.repository.TicketRepository;
import com.aiticket.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 工单业务服务。
 */
@Service
public class TicketService {

    private static final String DEFAULT_AI_REPLY = "感谢您的反馈，我们已收到工单，预计 24 小时内处理。";

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final MessageRepository messageRepository;
    private final FeedbackRepository feedbackRepository;

    /**
     * @param ticketRepository   工单仓库
     * @param userRepository     用户仓库
     * @param messageRepository  留言仓库
     * @param feedbackRepository 评价仓库
     */
    public TicketService(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            MessageRepository messageRepository,
            FeedbackRepository feedbackRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.messageRepository = messageRepository;
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * 客户创建工单：自动分类并生成 AI 建议回复。
     *
     * @param userId  当前用户 ID
     * @param request 创建请求
     * @return 工单响应
     */
    @Transactional
    public TicketResponse createTicket(UUID userId, TicketCreateRequest request) {
        User currentUser = requireUser(userId);
        if (currentUser.getRole() != UserRole.CUSTOMER) {
            throw new BusinessException("仅客户可提交工单", 403);
        }

        String title = request.getTitle().trim();
        String description = request.getDescription().trim();
        if (title.isEmpty() || description.isEmpty()) {
            throw new BusinessException("标题和描述不能为空", 400);
        }

        Ticket ticket = Ticket.builder()
                .customer(currentUser)
                .title(title)
                .description(description)
                .category(classify(title, description))
                .status(TicketStatus.PENDING)
                .aiSuggestedReply(DEFAULT_AI_REPLY)
                .build();

        Ticket saved = ticketRepository.save(ticket);
        return toTicketResponse(saved);
    }

    /**
     * 获取工单列表：客户仅看自己的；客服/管理员可看全部并按状态筛选、分页。
     *
     * @param userId 当前用户 ID
     * @param status 可选状态筛选（客服/管理员）
     * @param page   页码（从 0 开始）
     * @param size   每页大小
     * @return 分页结果
     */
    @Transactional(readOnly = true)
    public PageResult<TicketResponse> listTickets(UUID userId, String status, int page, int size) {
        User currentUser = requireUser(userId);
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize);

        Page<Ticket> ticketPage;
        if (isStaff(currentUser.getRole())) {
            TicketStatus statusEnum = parseStatusOptional(status);
            if (statusEnum != null) {
                ticketPage = ticketRepository.findByStatusOrderByCreatedAtDesc(statusEnum, pageable);
            } else {
                ticketPage = ticketRepository.findAllByOrderByCreatedAtDesc(pageable);
            }
        } else {
            ticketPage = ticketRepository.findByCustomer_IdOrderByCreatedAtDesc(currentUser.getId(), pageable);
        }

        List<TicketResponse> content = new ArrayList<TicketResponse>();
        for (Ticket ticket : ticketPage.getContent()) {
            content.add(toTicketResponse(ticket));
        }
        return new PageResult<TicketResponse>(
                content,
                ticketPage.getTotalElements(),
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalPages()
        );
    }

    /**
     * 获取工单详情（含留言与评价）。
     *
     * @param userId   当前用户 ID
     * @param ticketId 工单 ID
     * @return 详情响应
     */
    @Transactional(readOnly = true)
    public TicketDetailResponse getTicketDetail(UUID userId, UUID ticketId) {
        User currentUser = requireUser(userId);
        Ticket ticket = requireTicket(ticketId);
        assertCanView(currentUser, ticket);

        TicketDetailResponse detail = toTicketDetailResponse(ticket);
        List<Message> messages = messageRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId);
        List<MessageResponse> messageResponses = new ArrayList<MessageResponse>();
        for (Message message : messages) {
            messageResponses.add(MessageResponse.from(message));
        }
        detail.setMessages(messageResponses);

        Feedback feedback = feedbackRepository.findByTicket_Id(ticketId).orElse(null);
        if (feedback != null) {
            detail.setFeedback(FeedbackResponse.from(feedback));
        }
        return detail;
    }

    /**
     * 更新工单：客服改状态/指派；客户追加留言（resolved/closed 时回退为 pending）。
     * 通过请求中的 updatedAt 与数据库比较实现乐观锁。
     *
     * @param userId   当前用户 ID
     * @param ticketId 工单 ID
     * @param request  更新请求
     * @return 更新后的工单
     */
    @Transactional
    public TicketResponse updateTicket(UUID userId, UUID ticketId, TicketUpdateRequest request) {
        User currentUser = requireUser(userId);
        Ticket ticket = requireTicket(ticketId);
        assertCanView(currentUser, ticket);

        if (request.getUpdatedAt() == null) {
            throw new BusinessException("updatedAt 不能为空", 400);
        }
        assertOptimisticLock(ticket.getUpdatedAt(), request.getUpdatedAt());

        if (isStaff(currentUser.getRole())) {
            applyStaffUpdate(ticket, request);
        } else {
            applyCustomerUpdate(ticket, currentUser, request);
        }

        Ticket saved = ticketRepository.save(ticket);
        return toTicketResponse(saved);
    }

    /**
     * 根据标题与描述关键词自动分类。
     *
     * @param title       标题
     * @param description 描述
     * @return 分类名称
     */
    String classify(String title, String description) {
        String text = (title == null ? "" : title) + (description == null ? "" : description);
        if (containsAny(text, "退货", "退款")) {
            return "退货";
        }
        if (containsAny(text, "物流", "快递")) {
            return "物流";
        }
        if (containsAny(text, "账户", "登录")) {
            return "账户";
        }
        return "其他";
    }

    private void applyStaffUpdate(Ticket ticket, TicketUpdateRequest request) {
        boolean changed = false;
        if (request.getStatus() != null && !request.getStatus().trim().isEmpty()) {
            TicketStatus newStatus = parseStatusRequired(request.getStatus());
            ticket.setStatus(newStatus);
            changed = true;
        }
        if (request.getAssignedTo() != null) {
            User assignee = userRepository.findById(request.getAssignedTo())
                    .orElseThrow(() -> new BusinessException("指派用户不存在", 404));
            if (assignee.getRole() != UserRole.AGENT && assignee.getRole() != UserRole.ADMIN) {
                throw new BusinessException("只能指派给客服或管理员", 400);
            }
            ticket.setAssignedTo(assignee);
            changed = true;
        }
        if (!changed && (request.getExtraMessage() == null || request.getExtraMessage().trim().isEmpty())) {
            throw new BusinessException("请提供要更新的 status 或 assignedTo", 400);
        }
        // 客服 PATCH 忽略 extraMessage（留言走独立接口）
    }

    private void applyCustomerUpdate(Ticket ticket, User customer, TicketUpdateRequest request) {
        String extraMessage = request.getExtraMessage();
        if (extraMessage == null || extraMessage.trim().isEmpty()) {
            throw new BusinessException("请提供 extraMessage 追加留言", 400);
        }
        if (request.getStatus() != null || request.getAssignedTo() != null) {
            throw new BusinessException("客户无权修改状态或指派人", 403);
        }

        Message message = Message.builder()
                .ticket(ticket)
                .sender(customer)
                .content(extraMessage.trim())
                .build();
        messageRepository.save(message);

        if (ticket.getStatus() == TicketStatus.RESOLVED || ticket.getStatus() == TicketStatus.CLOSED) {
            ticket.setStatus(TicketStatus.PENDING);
        }
    }

    private void assertOptimisticLock(LocalDateTime dbUpdatedAt, LocalDateTime requestUpdatedAt) {
        if (dbUpdatedAt == null
                || !dbUpdatedAt.truncatedTo(ChronoUnit.MILLIS)
                .equals(requestUpdatedAt.truncatedTo(ChronoUnit.MILLIS))) {
            throw new BusinessException("工单已被他人修改，请刷新后重试", 409);
        }
    }

    private void assertCanView(User currentUser, Ticket ticket) {
        if (isStaff(currentUser.getRole())) {
            return;
        }
        if (ticket.getCustomer() == null || !ticket.getCustomer().getId().equals(currentUser.getId())) {
            throw new BusinessException("无权访问该工单", 403);
        }
    }

    private boolean isStaff(UserRole role) {
        return role == UserRole.AGENT || role == UserRole.ADMIN;
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在", 404));
    }

    private Ticket requireTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new BusinessException("工单不存在", 404));
    }

    private TicketStatus parseStatusOptional(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return parseStatusRequired(status);
    }

    private TicketStatus parseStatusRequired(String status) {
        try {
            return TicketStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("无效的工单状态: " + status, 400);
        }
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private TicketResponse toTicketResponse(Ticket ticket) {
        TicketResponse response = new TicketResponse();
        fillTicketFields(response, ticket);
        return response;
    }

    private TicketDetailResponse toTicketDetailResponse(Ticket ticket) {
        TicketDetailResponse response = new TicketDetailResponse();
        fillTicketFields(response, ticket);
        return response;
    }

    private void fillTicketFields(TicketResponse response, Ticket ticket) {
        response.setId(ticket.getId());
        if (ticket.getCustomer() != null) {
            response.setCustomerId(ticket.getCustomer().getId());
            response.setCustomerUsername(ticket.getCustomer().getUsername());
        }
        response.setTitle(ticket.getTitle());
        response.setDescription(ticket.getDescription());
        response.setCategory(ticket.getCategory());
        response.setStatus(ticket.getStatus() == null ? null : ticket.getStatus().name());
        if (ticket.getAssignedTo() != null) {
            response.setAssignedTo(ticket.getAssignedTo().getId());
        }
        response.setAiSuggestedReply(ticket.getAiSuggestedReply());
        response.setCreatedAt(ticket.getCreatedAt());
        response.setUpdatedAt(ticket.getUpdatedAt());
    }

}
