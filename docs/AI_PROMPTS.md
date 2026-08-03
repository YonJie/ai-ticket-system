# AI 提示词与调用约定（AI_PROMPTS）

创建工单时由 `AiAssistService` 生成 `category` 与 `aiSuggestedReply`。

## 1. 调用策略

| 条件 | 行为 |
|------|------|
| 已配置 `DEEPSEEK_API_KEY` | 调用 DeepSeek Chat Completions |
| 未配置 / HTTP 失败 / 超时 / 解析失败 | `RuleBasedAiAssistService` 关键词分类 + 固定建议回复 |
| 模型返回非法 `category` | 分类回退规则；若 `suggestedReply` 非空则保留 |
| 模型返回空 `suggestedReply` | 使用固定建议回复 |

## 2. DeepSeek 参数

| 项 | 值 |
|----|-----|
| Base URL | `DEEPSEEK_BASE_URL`（默认 `https://api.deepseek.com`） |
| Path | `/chat/completions` |
| Model | `DEEPSEEK_MODEL`（默认 `deepseek-v4-flash`） |
| Thinking | `{"type":"disabled"}`（必须显式关闭） |
| Response format | `{"type":"json_object"}` |
| Timeout | `DEEPSEEK_TIMEOUT_MS`（默认 8000） |

不切换 `deepseek-v4-pro`，不启用 thinking。

## 3. System Prompt

```text
你是客服工单助手。根据用户提供的工单标题与描述，输出 JSON 对象，字段：
category（只能是：退货、物流、账户、其他 之一）、
suggestedReply（给客服的建议回复，中文，礼貌简洁，不超过 120 字，不要使用 Markdown）。
不要输出其它字段或解释。
```

## 4. User Prompt 模板

```text
标题：{title}
描述：{description}
```

## 5. 期望输出

```json
{
  "category": "物流",
  "suggestedReply": "您好，我们已收到您的物流咨询，正在为您查询进度，请稍候。"
}
```

## 6. 规则回退（摘要）

1. 含「退货」或「退款」→ `退货`
2. 含「物流」或「快递」→ `物流`
3. 含「账户」或「登录」→ `账户`
4. 其他 → `其他`

固定建议回复：`感谢您的反馈，我们已收到工单，预计 24 小时内处理。`
