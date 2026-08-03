package com.aiticket.dto;

import com.aiticket.entity.Message;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工单留言响应对象。
 */
public class MessageResponse {

    private UUID id;
    private UUID ticketId;
    private UUID userId;
    private String username;
    private String content;
    private LocalDateTime createdAt;

    /**
     * 从实体转换为响应 DTO（与工单详情留言结构一致）。
     *
     * @param message 留言实体
     * @return 留言响应
     */
    public static MessageResponse from(Message message) {
        MessageResponse response = new MessageResponse();
        response.setId(message.getId());
        if (message.getTicket() != null) {
            response.setTicketId(message.getTicket().getId());
        }
        if (message.getSender() != null) {
            response.setUserId(message.getSender().getId());
            response.setUsername(message.getSender().getUsername());
        }
        response.setContent(message.getContent());
        response.setCreatedAt(message.getCreatedAt());
        return response;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTicketId() {
        return ticketId;
    }

    public void setTicketId(UUID ticketId) {
        this.ticketId = ticketId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
