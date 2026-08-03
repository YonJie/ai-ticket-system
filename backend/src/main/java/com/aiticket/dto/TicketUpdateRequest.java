package com.aiticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 更新工单请求体（状态修改或客户追加留言）。
 */
public class TicketUpdateRequest {

    /** 乐观锁时间戳，必须与库中 updatedAt 一致 */
    private LocalDateTime updatedAt;

    /** 客服可修改的目标状态 */
    private String status;

    /** 客服可指定的处理人用户 ID */
    private UUID assignedTo;

    /** 客户追加留言内容 */
    private String extraMessage;

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(UUID assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getExtraMessage() {
        return extraMessage;
    }

    public void setExtraMessage(String extraMessage) {
        this.extraMessage = extraMessage;
    }
}
