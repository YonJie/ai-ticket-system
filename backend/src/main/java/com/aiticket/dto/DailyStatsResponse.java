package com.aiticket.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 每日工单统计响应。
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyStatsResponse {

    /** 统计日期（yyyy-MM-dd） */
    private String date;

    /** 今日新增工单数 */
    private long newTicketsToday;

    /** 各状态工单数量 */
    private StatusCounts statusCounts;

    /**
     * 各状态数量（键名为小写状态值）。
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StatusCounts {
        private long pending;
        private long processing;
        private long resolved;
        private long closed;
    }
}
