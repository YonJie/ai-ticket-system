package com.aiticket.service;

import com.aiticket.dto.AiAssistResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * AiAssistService 编排与回退逻辑单测。
 */
@ExtendWith(MockitoExtension.class)
class AiAssistServiceTest {

    @Mock
    private DeepSeekAiClient deepSeekAiClient;

    private RuleBasedAiAssistService ruleBasedAiAssistService;
    private AiAssistService aiAssistService;

    /**
     * 注入 mock DeepSeek 与真实规则服务。
     */
    @BeforeEach
    void setUp() {
        ruleBasedAiAssistService = new RuleBasedAiAssistService();
        aiAssistService = new AiAssistService(deepSeekAiClient, ruleBasedAiAssistService);
    }

    /**
     * 未配置 Key 时直接规则回退，不调用 DeepSeek。
     */
    @Test
    void analyze_withoutApiKey_usesRuleBased() {
        when(deepSeekAiClient.isConfigured()).thenReturn(false);

        AiAssistResult result = aiAssistService.analyze("申请退货", "商品破损要退款");

        assertThat(result.getCategory()).isEqualTo("退货");
        assertThat(result.getSuggestedReply()).isEqualTo(RuleBasedAiAssistService.DEFAULT_AI_REPLY);
        verify(deepSeekAiClient, never()).analyze(anyString(), anyString());
    }

    /**
     * DeepSeek 成功时使用模型结果。
     */
    @Test
    void analyze_withDeepSeekSuccess() {
        when(deepSeekAiClient.isConfigured()).thenReturn(true);
        when(deepSeekAiClient.analyze("物流延迟", "快递三天未更新"))
                .thenReturn(new AiAssistResult("物流", "您好，我们正在为您查询物流。"));

        AiAssistResult result = aiAssistService.analyze("物流延迟", "快递三天未更新");

        assertThat(result.getCategory()).isEqualTo("物流");
        assertThat(result.getSuggestedReply()).isEqualTo("您好，我们正在为您查询物流。");
    }

    /**
     * DeepSeek 抛错时回退规则。
     */
    @Test
    void analyze_whenDeepSeekFails_fallsBack() {
        when(deepSeekAiClient.isConfigured()).thenReturn(true);
        when(deepSeekAiClient.analyze(anyString(), anyString()))
                .thenThrow(new IllegalStateException("timeout"));

        AiAssistResult result = aiAssistService.analyze("无法登录", "提示密码错误");

        assertThat(result.getCategory()).isEqualTo("账户");
        assertThat(result.getSuggestedReply()).isEqualTo(RuleBasedAiAssistService.DEFAULT_AI_REPLY);
    }

    /**
     * DeepSeek 仅返回非法分类时，分类回退、建议回复保留。
     */
    @Test
    void analyze_partialRemoteResult_mergesWithFallback() {
        when(deepSeekAiClient.isConfigured()).thenReturn(true);
        when(deepSeekAiClient.analyze("申请退货", "要退款"))
                .thenReturn(new AiAssistResult(null, "已为您登记退货申请，请留意后续通知。"));

        AiAssistResult result = aiAssistService.analyze("申请退货", "要退款");

        assertThat(result.getCategory()).isEqualTo("退货");
        assertThat(result.getSuggestedReply()).isEqualTo("已为您登记退货申请，请留意后续通知。");
    }
}
