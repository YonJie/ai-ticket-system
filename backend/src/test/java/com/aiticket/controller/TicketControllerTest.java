package com.aiticket.controller;

import com.aiticket.config.SecurityConfig;
import com.aiticket.dto.TicketCreateRequest;
import com.aiticket.dto.TicketResponse;
import com.aiticket.dto.TicketUpdateRequest;
import com.aiticket.exception.GlobalExceptionHandler;
import com.aiticket.service.TicketService;
import com.aiticket.support.MockAuth;
import com.aiticket.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TicketController MockMvc 切片测试：创建工单（含分类字段透传）、状态修改。
 */
@WebMvcTest(controllers = TicketController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketService ticketService;

    /** WebConfig / JwtInterceptor 切片加载所需 */
    @MockBean
    private JwtUtil jwtUtil;

    /**
     * 默认令牌解析：具体用例可覆盖返回值。
     */
    @BeforeEach
    void stubJwt() {
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(UUID.randomUUID());
    }

    /**
     * 创建工单成功，响应包含自动分类结果与 PENDING 状态。
     */
    @Test
    void createTicket_returnsClassifiedCategory() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(userId);
        UUID ticketId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.of(2026, 8, 3, 14, 0);

        TicketResponse response = new TicketResponse();
        response.setId(ticketId);
        response.setCustomerId(userId);
        response.setCustomerUsername("customer");
        response.setTitle("申请退货");
        response.setDescription("商品有问题需要退款");
        response.setCategory("退货");
        response.setStatus("PENDING");
        response.setAiSuggestedReply("感谢您的反馈，我们已收到工单，预计 24 小时内处理。");
        response.setCreatedAt(now);
        response.setUpdatedAt(now);

        when(ticketService.createTicket(eq(userId), any(TicketCreateRequest.class))).thenReturn(response);

        TicketCreateRequest body = new TicketCreateRequest();
        body.setTitle("申请退货");
        body.setDescription("商品有问题需要退款");

        mockMvc.perform(post("/api/tickets")
                        .with(MockAuth.withUserId(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.data.category").value("退货"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        verify(ticketService).createTicket(eq(userId), any(TicketCreateRequest.class));
    }

    /**
     * 客服修改工单状态为 PROCESSING。
     */
    @Test
    void updateTicket_changeStatus() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(userId);
        UUID ticketId = UUID.randomUUID();
        LocalDateTime updatedAt = LocalDateTime.of(2026, 8, 3, 14, 0);

        TicketResponse response = new TicketResponse();
        response.setId(ticketId);
        response.setCustomerId(UUID.randomUUID());
        response.setCustomerUsername("customer");
        response.setTitle("物流延迟");
        response.setDescription("快递三天未更新");
        response.setCategory("物流");
        response.setStatus("PROCESSING");
        response.setUpdatedAt(updatedAt.plusMinutes(1));
        response.setCreatedAt(updatedAt);

        when(ticketService.updateTicket(eq(userId), eq(ticketId), any(TicketUpdateRequest.class)))
                .thenReturn(response);

        TicketUpdateRequest body = new TicketUpdateRequest();
        body.setUpdatedAt(updatedAt);
        body.setStatus("PROCESSING");

        mockMvc.perform(patch("/api/tickets/{id}", ticketId)
                        .with(MockAuth.withUserId(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

        ArgumentCaptor<TicketUpdateRequest> captor = ArgumentCaptor.forClass(TicketUpdateRequest.class);
        verify(ticketService).updateTicket(eq(userId), eq(ticketId), captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("PROCESSING");
    }
}
