package com.aiticket.service;

import com.aiticket.repository.FeedbackRepository;
import com.aiticket.repository.MessageRepository;
import com.aiticket.repository.TicketRepository;
import com.aiticket.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 工单自动分类规则单元测试（包可见 classify 方法）。
 */
@ExtendWith(MockitoExtension.class)
class TicketServiceClassifyTest {

    @Mock
    private TicketRepository ticketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private FeedbackRepository feedbackRepository;

    private TicketService ticketService;

    /**
     * 构造仅依赖 mock 仓库的 TicketService。
     */
    @BeforeEach
    void setUp() {
        ticketService = new TicketService(
                ticketRepository, userRepository, messageRepository, feedbackRepository);
    }

    /**
     * 含「退货」「退款」应分类为退货。
     */
    @Test
    void classify_returnCategory() {
        assertThat(ticketService.classify("申请退货", "颜色不符")).isEqualTo("退货");
        assertThat(ticketService.classify("售后", "请求退款")).isEqualTo("退货");
    }

    /**
     * 含「物流」「快递」应分类为物流。
     */
    @Test
    void classify_logisticsCategory() {
        assertThat(ticketService.classify("物流延迟", "三天未更新")).isEqualTo("物流");
        assertThat(ticketService.classify("咨询", "快递丢件")).isEqualTo("物流");
    }

    /**
     * 含「账户」「登录」应分类为账户。
     */
    @Test
    void classify_accountCategory() {
        assertThat(ticketService.classify("账户被锁", "无法操作")).isEqualTo("账户");
        assertThat(ticketService.classify("无法登录", "提示密码错误")).isEqualTo("账户");
    }

    /**
     * 无关键词应分类为其他。
     */
    @Test
    void classify_otherCategory() {
        assertThat(ticketService.classify("产品咨询", "功能如何使用")).isEqualTo("其他");
    }
}
