package com.aiticket.service;

import com.aiticket.dto.AiAssistResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 工单 AI 助手：优先 DeepSeek（flash、关闭 thinking），失败则规则回退。
 */
@Service
public class AiAssistService {

    private static final Logger log = LoggerFactory.getLogger(AiAssistService.class);

    private final DeepSeekAiClient deepSeekAiClient;
    private final RuleBasedAiAssistService ruleBasedAiAssistService;

    /**
     * @param deepSeekAiClient         DeepSeek 客户端
     * @param ruleBasedAiAssistService 规则回退
     */
    public AiAssistService(
            DeepSeekAiClient deepSeekAiClient,
            RuleBasedAiAssistService ruleBasedAiAssistService) {
        this.deepSeekAiClient = deepSeekAiClient;
        this.ruleBasedAiAssistService = ruleBasedAiAssistService;
    }

    /**
     * 分析工单标题与描述，生成分类与建议回复。
     *
     * @param title       标题
     * @param description 描述
     * @return 最终可用的分析结果（字段均非空）
     */
    public AiAssistResult analyze(String title, String description) {
        AiAssistResult fallback = ruleBasedAiAssistService.analyze(title, description);

        if (!deepSeekAiClient.isConfigured()) {
            log.info("未检测到 DEEPSEEK_API_KEY，使用规则回退（固定建议回复）");
            return fallback;
        }

        try {
            AiAssistResult remote = deepSeekAiClient.analyze(title, description);
            String category = StringUtils.hasText(remote.getCategory())
                    ? remote.getCategory()
                    : fallback.getCategory();
            String suggestedReply = StringUtils.hasText(remote.getSuggestedReply())
                    ? remote.getSuggestedReply()
                    : fallback.getSuggestedReply();
            log.info("DeepSeek 分析完成: category={}", category);
            return new AiAssistResult(category, suggestedReply);
        } catch (Exception ex) {
            log.warn("DeepSeek 调用失败，回退规则分类与固定建议回复: {}", ex.getMessage());
            return fallback;
        }
    }
}
