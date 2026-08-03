package com.aiticket.controller;

import com.aiticket.config.SecurityConfig;
import com.aiticket.dto.CreateFeedbackRequest;
import com.aiticket.dto.CreateMessageRequest;
import com.aiticket.dto.FeedbackResponse;
import com.aiticket.dto.MessageResponse;
import com.aiticket.exception.GlobalExceptionHandler;
import com.aiticket.service.TicketFeedbackService;
import com.aiticket.service.TicketMessageService;
import com.aiticket.support.MockAuth;
import com.aiticket.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TicketInteractionController MockMvc 切片测试：留言与评价。
 */
@WebMvcTest(controllers = TicketInteractionController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class TicketInteractionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TicketMessageService ticketMessageService;

    @MockBean
    private TicketFeedbackService ticketFeedbackService;

    /** WebConfig / JwtInterceptor 切片加载所需 */
    @MockBean
    private JwtUtil jwtUtil;

    /**
     * 默认令牌解析占位。
     */
    @BeforeEach
    void stubJwt() {
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(UUID.randomUUID());
    }

    /**
     * 追加工单留言成功。
     */
    @Test
    void addMessage_success() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(userId);
        UUID ticketId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();

        MessageResponse response = new MessageResponse();
        response.setId(messageId);
        response.setTicketId(ticketId);
        response.setUserId(userId);
        response.setUsername("customer");
        response.setContent("请问进度如何？");
        response.setCreatedAt(LocalDateTime.of(2026, 8, 3, 15, 0));

        when(ticketMessageService.addMessage(eq(ticketId), eq(userId), any(CreateMessageRequest.class)))
                .thenReturn(response);

        CreateMessageRequest body = CreateMessageRequest.builder()
                .content("请问进度如何？")
                .build();

        mockMvc.perform(post("/api/tickets/{id}/messages", ticketId)
                        .with(MockAuth.withUserId(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(messageId.toString()))
                .andExpect(jsonPath("$.data.content").value("请问进度如何？"));

        verify(ticketMessageService).addMessage(eq(ticketId), eq(userId), any(CreateMessageRequest.class));
    }

    /**
     * 提交工单评价成功。
     */
    @Test
    void submitFeedback_success() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtUtil.getUserIdFromToken(anyString())).thenReturn(userId);
        UUID ticketId = UUID.randomUUID();
        UUID feedbackId = UUID.randomUUID();

        FeedbackResponse response = new FeedbackResponse();
        response.setId(feedbackId);
        response.setTicketId(ticketId);
        response.setRating(5);
        response.setComment("处理很快");
        response.setCreatedAt(LocalDateTime.of(2026, 8, 3, 16, 0));

        when(ticketFeedbackService.submitFeedback(eq(ticketId), eq(userId), any(CreateFeedbackRequest.class)))
                .thenReturn(response);

        CreateFeedbackRequest body = CreateFeedbackRequest.builder()
                .rating(5)
                .comment("处理很快")
                .build();

        mockMvc.perform(post("/api/tickets/{id}/feedback", ticketId)
                        .with(MockAuth.withUserId(userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("处理很快"));

        verify(ticketFeedbackService).submitFeedback(eq(ticketId), eq(userId), any(CreateFeedbackRequest.class));
    }
}
