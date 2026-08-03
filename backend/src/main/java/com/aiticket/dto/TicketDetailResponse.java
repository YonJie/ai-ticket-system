package com.aiticket.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * 工单详情响应：含留言与评价。
 */
public class TicketDetailResponse extends TicketResponse {

    private List<MessageResponse> messages = new ArrayList<MessageResponse>();
    private FeedbackResponse feedback;

    public List<MessageResponse> getMessages() {
        return messages;
    }

    public void setMessages(List<MessageResponse> messages) {
        this.messages = messages;
    }

    public FeedbackResponse getFeedback() {
        return feedback;
    }

    public void setFeedback(FeedbackResponse feedback) {
        this.feedback = feedback;
    }
}
