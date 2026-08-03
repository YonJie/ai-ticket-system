package com.aiticket.controller;

import com.aiticket.common.Result;
import com.aiticket.dto.DailyStatsResponse;
import com.aiticket.interceptor.JwtInterceptor;
import com.aiticket.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

/**
 * 统计接口（管理员）。
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService statsService;

    /**
     * @param statsService 统计服务
     */
    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    /**
     * 获取今日工单统计：今日新增数与各状态数量。
     *
     * @param request HTTP 请求（含 JWT 拦截器写入的 userId）
     * @return 统一响应
     */
    @GetMapping("/daily")
    public Result<DailyStatsResponse> daily(HttpServletRequest request) {
        UUID userId = (UUID) request.getAttribute(JwtInterceptor.USER_ID_ATTR);
        return Result.success(statsService.getDailyStats(userId));
    }
}
