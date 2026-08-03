package com.aiticket.dto;

import com.aiticket.entity.Feedback;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 工单评价响应对象。
 */
public class FeedbackResponse {

    private UUID id;
    private UUID ticketId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    /**
     * 从实体转换为响应 DTO（与工单详情评价结构一致）。
     *
     * @param feedback 评价实体
     * @return 评价响应
     */
    public static FeedbackResponse from(Feedback feedback) {
        FeedbackResponse response = new FeedbackResponse();
        response.setId(feedback.getId());
        if (feedback.getTicket() != null) {
            response.setTicketId(feedback.getTicket().getId());
        }
        response.setRating(feedback.getRating());
        response.setComment(feedback.getComment());
        response.setCreatedAt(feedback.getCreatedAt());
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

    public Integer getRating() {
        return rating;
    }

    public void setRating(Integer rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
