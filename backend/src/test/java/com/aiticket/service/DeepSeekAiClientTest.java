package com.aiticket.service;

import com.aiticket.dto.AiAssistResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DeepSeek 响应解析单测（不访问外网）。
 */
class DeepSeekAiClientTest {

    private DeepSeekAiClient client;

    /**
     * 使用空 Key 的客户端仅测解析。
     */
    @BeforeEach
    void setUp() {
        client = new DeepSeekAiClient(
                "",
                "https://api.deepseek.com",
                "deepseek-v4-flash",
                8000,
                new ObjectMapper());
    }

    /**
     * 合法 JSON content 应解析出分类与建议回复。
     */
    @Test
    void parseContent_validPayload() throws Exception {
        String body = "{"
                + "\"choices\":[{"
                + "\"message\":{\"content\":\"{\\\"category\\\":\\\"物流\\\",\\\"suggestedReply\\\":\\\"您好，我们已协助查询物流进度。\\\"}\"}"
                + "}]"
                + "}";

        AiAssistResult result = client.parseContent(body);
        assertThat(result.getCategory()).isEqualTo("物流");
        assertThat(result.getSuggestedReply()).isEqualTo("您好，我们已协助查询物流进度。");
    }

    /**
     * 非法分类应置空，交由上层回退。
     */
    @Test
    void parseContent_invalidCategoryBecomesNull() throws Exception {
        String body = "{"
                + "\"choices\":[{"
                + "\"message\":{\"content\":\"{\\\"category\\\":\\\"售后\\\",\\\"suggestedReply\\\":\\\"请稍候\\\"}\"}"
                + "}]"
                + "}";

        AiAssistResult result = client.parseContent(body);
        assertThat(result.getCategory()).isNull();
        assertThat(result.getSuggestedReply()).isEqualTo("请稍候");
    }

    /**
     * 缺少 content 应抛错。
     */
    @Test
    void parseContent_missingContent() {
        assertThatThrownBy(() -> client.parseContent("{\"choices\":[{}]}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("message.content");
    }

    /**
     * 未配置 Key 时 isConfigured 为 false。
     */
    @Test
    void isConfigured_falseWhenEmptyKey() {
        assertThat(client.isConfigured()).isFalse();
    }
}
