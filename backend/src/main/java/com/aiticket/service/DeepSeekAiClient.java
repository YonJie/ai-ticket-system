package com.aiticket.service;

import com.aiticket.dto.AiAssistResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * DeepSeek Chat Completions 客户端（OpenAI 兼容）。
 * 固定使用 flash、关闭 thinking，一次返回分类与建议回复。
 */
@Component
public class DeepSeekAiClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekAiClient.class);

    private static final Set<String> ALLOWED_CATEGORIES =
            new HashSet<String>(Arrays.asList("退货", "物流", "账户", "其他"));

    private static final String SYSTEM_PROMPT =
            "你是客服工单助手。根据用户提供的工单标题与描述，输出 JSON 对象，字段："
                    + "category（只能是：退货、物流、账户、其他 之一）、"
                    + "suggestedReply（给客服的建议回复，中文，礼貌简洁，不超过 120 字，不要使用 Markdown）。"
                    + "不要输出其它字段或解释。";

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    /**
     * 从 Environment 读取配置。优先 {@code DEEPSEEK_*}（.env / 系统环境变量），
     * 避免 {@code ai.deepseek.api-key=${DEEPSEEK_API_KEY:}} 在 DotEnv 注入前被解析成空串。
     *
     * @param environment  Spring 环境
     * @param objectMapper Jackson
     */
    @Autowired
    public DeepSeekAiClient(Environment environment, ObjectMapper objectMapper) {
        this(
                firstNonBlank(
                        environment.getProperty("DEEPSEEK_API_KEY"),
                        environment.getProperty("ai.deepseek.api-key")),
                firstNonBlank(
                        environment.getProperty("DEEPSEEK_BASE_URL"),
                        environment.getProperty("ai.deepseek.base-url"),
                        "https://api.deepseek.com"),
                firstNonBlank(
                        environment.getProperty("DEEPSEEK_MODEL"),
                        environment.getProperty("ai.deepseek.model"),
                        "deepseek-v4-flash"),
                parseTimeoutMs(environment),
                objectMapper);
    }

    /**
     * @param apiKey       DeepSeek API Key，可为空
     * @param baseUrl      API Base URL
     * @param model        模型名，默认 deepseek-v4-flash
     * @param timeoutMs    超时毫秒
     * @param objectMapper Jackson
     */
    DeepSeekAiClient(
            String apiKey,
            String baseUrl,
            String model,
            int timeoutMs,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.baseUrl = trimTrailingSlash(baseUrl == null ? "https://api.deepseek.com" : baseUrl.trim());
        this.model = StringUtils.hasText(model) ? model.trim() : "deepseek-v4-flash";
        this.objectMapper = objectMapper;
        this.restTemplate = createRestTemplate(timeoutMs);
        log.info("DeepSeek 客户端就绪: configured={}, model={}, baseUrl={}",
                StringUtils.hasText(this.apiKey), this.model, this.baseUrl);
    }

    /**
     * 是否已配置可用的 API Key。
     *
     * @return true 表示可尝试调用 DeepSeek
     */
    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * 调用 DeepSeek，解析并校验结果。
     *
     * @param title       标题
     * @param description 描述
     * @return 分析结果；解析失败或非法字段时对应字段可为 null/空，由上层补全
     */
    public AiAssistResult analyze(String title, String description) {
        String url = baseUrl + "/chat/completions";
        log.info("调用 DeepSeek Chat Completions: model={}, url={}", model, url);

        Map<String, Object> thinking = new HashMap<String, Object>();
        thinking.put("type", "disabled");

        Map<String, Object> responseFormat = new HashMap<String, Object>();
        responseFormat.put("type", "json_object");

        List<Map<String, String>> messages = new ArrayList<Map<String, String>>();
        messages.add(message("system", SYSTEM_PROMPT));
        messages.add(message("user", buildUserPrompt(title, description)));

        Map<String, Object> body = new HashMap<String, Object>();
        body.put("model", model);
        body.put("thinking", thinking);
        body.put("response_format", responseFormat);
        body.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        ResponseEntity<String> response = restTemplate.postForEntity(
                url, new HttpEntity<Map<String, Object>>(body, headers), String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("DeepSeek HTTP 状态异常: " + response.getStatusCode());
        }

        try {
            return parseContent(response.getBody());
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("DeepSeek 响应解析失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 从 Chat Completions 响应体解析 category / suggestedReply。
     *
     * @param responseBody 原始 JSON 字符串
     * @return 可能部分字段为空的结果
     * @throws Exception JSON 解析失败时抛出
     */
    AiAssistResult parseContent(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || !contentNode.isTextual() || !StringUtils.hasText(contentNode.asText())) {
            throw new IllegalStateException("DeepSeek 响应缺少 message.content");
        }

        String content = contentNode.asText().trim();
        JsonNode payload = objectMapper.readTree(extractJsonObject(content));
        String category = textOrNull(payload, "category");
        String suggestedReply = textOrNull(payload, "suggestedReply");
        if (!StringUtils.hasText(suggestedReply)) {
            suggestedReply = textOrNull(payload, "suggested_reply");
        }

        if (StringUtils.hasText(category)) {
            category = category.trim();
            if (!ALLOWED_CATEGORIES.contains(category)) {
                log.warn("DeepSeek 返回非法 category={}，将回退规则分类", category);
                category = null;
            }
        } else {
            category = null;
        }

        if (StringUtils.hasText(suggestedReply)) {
            suggestedReply = suggestedReply.trim();
        } else {
            suggestedReply = null;
        }

        return new AiAssistResult(category, suggestedReply);
    }

    private static String extractJsonObject(String content) {
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return content.substring(start, end + 1);
        }
        return content;
    }

    private static String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || !value.isTextual()) {
            return null;
        }
        return value.asText();
    }

    private static Map<String, String> message(String role, String content) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("role", role);
        map.put("content", content);
        return map;
    }

    private static String buildUserPrompt(String title, String description) {
        return "标题：" + (title == null ? "" : title) + "\n描述：" + (description == null ? "" : description);
    }

    private static String trimTrailingSlash(String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static int parseTimeoutMs(Environment environment) {
        String raw = firstNonBlank(
                environment.getProperty("DEEPSEEK_TIMEOUT_MS"),
                environment.getProperty("ai.deepseek.timeout-ms"));
        if (!StringUtils.hasText(raw)) {
            return 8000;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return 8000;
        }
    }

    private static RestTemplate createRestTemplate(int timeoutMs) {
        int safeTimeout = timeoutMs <= 0 ? 8000 : timeoutMs;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(safeTimeout);
        factory.setReadTimeout(safeTimeout);
        return new RestTemplate(factory);
    }
}
