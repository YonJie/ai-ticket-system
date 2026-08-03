package com.aiticket.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 规则回退分类单元测试。
 */
class RuleBasedAiAssistServiceTest {

    private RuleBasedAiAssistService ruleBasedAiAssistService;

    /**
     * 构造规则服务。
     */
    @BeforeEach
    void setUp() {
        ruleBasedAiAssistService = new RuleBasedAiAssistService();
    }

    /**
     * 含「退货」「退款」应分类为退货。
     */
    @Test
    void classify_returnCategory() {
        assertThat(ruleBasedAiAssistService.classify("申请退货", "颜色不符")).isEqualTo("退货");
        assertThat(ruleBasedAiAssistService.classify("售后", "请求退款")).isEqualTo("退货");
    }

    /**
     * 含「物流」「快递」应分类为物流。
     */
    @Test
    void classify_logisticsCategory() {
        assertThat(ruleBasedAiAssistService.classify("物流延迟", "三天未更新")).isEqualTo("物流");
        assertThat(ruleBasedAiAssistService.classify("咨询", "快递丢件")).isEqualTo("物流");
    }

    /**
     * 含「账户」「登录」应分类为账户。
     */
    @Test
    void classify_accountCategory() {
        assertThat(ruleBasedAiAssistService.classify("账户被锁", "无法操作")).isEqualTo("账户");
        assertThat(ruleBasedAiAssistService.classify("无法登录", "提示密码错误")).isEqualTo("账户");
    }

    /**
     * 无关键词应分类为其他，并带固定建议回复。
     */
    @Test
    void analyze_otherCategoryWithDefaultReply() {
        assertThat(ruleBasedAiAssistService.analyze("产品咨询", "功能如何使用").getCategory())
                .isEqualTo("其他");
        assertThat(ruleBasedAiAssistService.analyze("产品咨询", "功能如何使用").getSuggestedReply())
                .isEqualTo(RuleBasedAiAssistService.DEFAULT_AI_REPLY);
    }
}
