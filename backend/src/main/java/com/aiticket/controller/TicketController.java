package com.aiticket.controller;

import com.aiticket.common.Result;
import com.aiticket.dto.PageResult;
import com.aiticket.dto.TicketCreateRequest;
import com.aiticket.dto.TicketDetailResponse;
import com.aiticket.dto.TicketResponse;
import com.aiticket.dto.TicketUpdateRequest;
import com.aiticket.service.TicketService;
import com.aiticket.util.AuthRequestUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.UUID;

/**
 * 工单相关 API。
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    /**
     * @param ticketService 工单服务
     */
    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    /**
     * 客户提交工单。
     *
     * @param request     HTTP 请求（含 userId）
     * @param createRequest 创建体
     * @return 新建工单
     */
    @PostMapping
    public Result<TicketResponse> create(
            HttpServletRequest request,
            @Valid @RequestBody TicketCreateRequest createRequest) {
        UUID userId = AuthRequestUtils.requireUserId(request);
        return Result.success(ticketService.createTicket(userId, createRequest));
    }

    /**
     * 获取工单列表。
     *
     * @param request HTTP 请求
     * @param status  状态筛选（客服/管理员）
     * @param page    页码，从 0 开始
     * @param size    每页大小
     * @return 分页列表
     */
    @GetMapping
    public Result<PageResult<TicketResponse>> list(
            HttpServletRequest request,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        UUID userId = AuthRequestUtils.requireUserId(request);
        return Result.success(ticketService.listTickets(userId, status, page, size));
    }

    /**
     * 获取工单详情。
     *
     * @param request  HTTP 请求
     * @param id       工单 ID
     * @return 详情（含留言、评价）
     */
    @GetMapping("/{id}")
    public Result<TicketDetailResponse> detail(
            HttpServletRequest request,
            @PathVariable("id") UUID id) {
        UUID userId = AuthRequestUtils.requireUserId(request);
        return Result.success(ticketService.getTicketDetail(userId, id));
    }

    /**
     * 更新工单（客服改状态/指派，或客户追加留言）。
     *
     * @param request       HTTP 请求
     * @param id            工单 ID
     * @param updateRequest 更新体
     * @return 更新后的工单
     */
    @PatchMapping("/{id}")
    public Result<TicketResponse> update(
            HttpServletRequest request,
            @PathVariable("id") UUID id,
            @RequestBody TicketUpdateRequest updateRequest) {
        UUID userId = AuthRequestUtils.requireUserId(request);
        return Result.success(ticketService.updateTicket(userId, id, updateRequest));
    }
}
