package com.aiticket.service;

import com.aiticket.dto.DailyStatsResponse;
import com.aiticket.entity.User;
import com.aiticket.enums.TicketStatus;
import com.aiticket.enums.UserRole;
import com.aiticket.exception.BusinessException;
import com.aiticket.repository.TicketRepository;
import com.aiticket.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 统计业务服务（真实查询 tickets 表）。
 */
@Service
public class StatsService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;

    /**
     * @param ticketRepository 工单仓库
     * @param userRepository   用户仓库
     */
    public StatsService(TicketRepository ticketRepository, UserRepository userRepository) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
    }

    /**
     * 获取今日工单统计；仅管理员可访问。
     *
     * @param userId 当前登录用户 ID（来自 JWT 拦截器）
     * @return 每日统计数据
     */
    @Transactional(readOnly = true)
    public DailyStatsResponse getDailyStats(UUID userId) {
        requireAdmin(userId);

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime startOfNextDay = today.plusDays(1).atStartOfDay();

        long newTicketsToday = ticketRepository
                .countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfDay, startOfNextDay);

        DailyStatsResponse.StatusCounts statusCounts = buildStatusCounts();

        return DailyStatsResponse.builder()
                .date(today.toString())
                .newTicketsToday(newTicketsToday)
                .statusCounts(statusCounts)
                .build();
    }

    /**
     * 校验当前用户为管理员。
     *
     * @param userId 用户 ID
     */
    private void requireAdmin(UUID userId) {
        if (userId == null) {
            throw new BusinessException("未登录或令牌缺失", 401);
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在", 401));
        if (user.getRole() != UserRole.ADMIN) {
            throw new BusinessException("无权限：仅管理员可查看统计", 403);
        }
    }

    /**
     * 汇总各状态工单数量；缺失状态记为 0。
     *
     * @return 状态计数
     */
    private DailyStatsResponse.StatusCounts buildStatusCounts() {
        long pending = 0L;
        long processing = 0L;
        long resolved = 0L;
        long closed = 0L;

        List<Object[]> rows = ticketRepository.countGroupByStatus();
        for (Object[] row : rows) {
            if (row == null || row.length < 2 || row[0] == null || row[1] == null) {
                continue;
            }
            TicketStatus status = (TicketStatus) row[0];
            long count = ((Number) row[1]).longValue();
            switch (status) {
                case PENDING:
                    pending = count;
                    break;
                case PROCESSING:
                    processing = count;
                    break;
                case RESOLVED:
                    resolved = count;
                    break;
                case CLOSED:
                    closed = count;
                    break;
                default:
                    break;
            }
        }

        return DailyStatsResponse.StatusCounts.builder()
                .pending(pending)
                .processing(processing)
                .resolved(resolved)
                .closed(closed)
                .build();
    }
}
