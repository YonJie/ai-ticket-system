package com.aiticket.service;

import com.aiticket.dto.AiAssistResult;
import org.springframework.stereotype.Service;

/**
 * 关键词规则回退：无 DeepSeek 或调用失败时使用。
 */
@Service
public class RuleBasedAiAssistService {

    /** 默认 AI 建议回复文案。 */
    public static final String DEFAULT_AI_REPLY = "感谢您的反馈，我们已收到工单，预计 24 小时内处理。";

    /**
     * 按标题与描述做规则分析。
     *
     * @param title       标题
     * @param description 描述
     * @return 分类 + 固定建议回复
     */
    public AiAssistResult analyze(String title, String description) {
        return new AiAssistResult(classify(title, description), DEFAULT_AI_REPLY);
    }

    /**
     * 根据标题与描述关键词自动分类。
     *
     * @param title       标题
     * @param description 描述
     * @return 分类名称
     */
    public String classify(String title, String description) {
        String text = (title == null ? "" : title) + (description == null ? "" : description);
        if (containsAny(text, "退货", "退款")) {
            return "退货";
        }
        if (containsAny(text, "物流", "快递")) {
            return "物流";
        }
        if (containsAny(text, "账户", "登录")) {
            return "账户";
        }
        return "其他";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
