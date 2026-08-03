package com.aiticket.dto;

/**
 * AI 助手对工单标题与描述的分析结果：分类 + 建议回复。
 */
public class AiAssistResult {

    private final String category;
    private final String suggestedReply;

    /**
     * @param category       分类：退货 / 物流 / 账户 / 其他
     * @param suggestedReply 客服建议回复文案
     */
    public AiAssistResult(String category, String suggestedReply) {
        this.category = category;
        this.suggestedReply = suggestedReply;
    }

    /**
     * @return 分类名称
     */
    public String getCategory() {
        return category;
    }

    /**
     * @return 建议回复
     */
    public String getSuggestedReply() {
        return suggestedReply;
    }
}
