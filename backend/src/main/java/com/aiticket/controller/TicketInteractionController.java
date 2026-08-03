package com.aiticket.controller;

import com.aiticket.common.Result;
import com.aiticket.dto.CreateFeedbackRequest;
import com.aiticket.dto.CreateMessageRequest;
import com.aiticket.dto.FeedbackResponse;
import com.aiticket.dto.MessageResponse;
import com.aiticket.service.TicketFeedbackService;
import com.aiticket.service.TicketMessageService;
import com.aiticket.util.AuthRequestUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.UUID;

/**
 * 工单留言与评价接口（与工单 CRUD Controller 分离，降低并行改动冲突）。
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketInteractionController {

    private final TicketMessageService ticketMessageService;
    private final TicketFeedbackService ticketFeedbackService;

    /**
     * @param ticketMessageService  留言服务
     * @param ticketFeedbackService 评价服务
     */
    public TicketInteractionController(
            TicketMessageService ticketMessageService,
            TicketFeedbackService ticketFeedbackService) {
        this.ticketMessageService = ticketMessageService;
        this.ticketFeedbackService = ticketFeedbackService;
    }

    /**
     * 追加工单留言（客户或客服/管理员）。
     *
     * @param id      工单 ID
     * @param request 留言请求
     * @param httpReq HTTP 请求（含 JWT 拦截器写入的 userId）
     * @return 留言信息
     */
    @PostMapping("/{id}/messages")
    public Result<MessageResponse> addMessage(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateMessageRequest request,
            HttpServletRequest httpReq) {
        UUID userId = AuthRequestUtils.requireUserId(httpReq);
        return Result.success(ticketMessageService.addMessage(id, userId, request));
    }

    /**
     * 提交工单评价（仅客户，且工单状态为 resolved）。
     *
     * @param id      工单 ID
     * @param request 评价请求
     * @param httpReq HTTP 请求（含 JWT 拦截器写入的 userId）
     * @return 评价信息
     */
    @PostMapping("/{id}/feedback")
    public Result<FeedbackResponse> submitFeedback(
            @PathVariable("id") UUID id,
            @Valid @RequestBody CreateFeedbackRequest request,
            HttpServletRequest httpReq) {
        UUID userId = AuthRequestUtils.requireUserId(httpReq);
        return Result.success(ticketFeedbackService.submitFeedback(id, userId, request));
    }
}
