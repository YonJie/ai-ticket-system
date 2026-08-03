# API 契约文档

统一响应格式：

```json
{
  "success": true,
  "data": {},
  "message": "可选提示",
  "code": 200
}
```

失败时 `success` 为 `false`，`data` 通常为 `null`，`message` 说明原因，`code` 为业务/HTTP 状态码。

字段命名：JSON 使用 camelCase；数据库列为 snake_case。

鉴权：除白名单接口外，请求头需携带 `Authorization: Bearer <token>`。JWT 拦截器校验通过后将 `userId`（UUID）写入 request 属性。

---

## 认证 Auth

### POST /api/auth/register

注册新用户（无需鉴权）。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名，2-64 字符，唯一 |
| password | string | 是 | 明文密码，6-64 字符，服务端 BCrypt 加密存储 |
| role | string | 否 | 角色：`customer` / `agent` / `admin`（大小写不敏感），默认 `customer` |

**成功响应 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string (UUID) | 用户 ID |
| username | string | 用户名 |
| role | string | `CUSTOMER` / `AGENT` / `ADMIN` |
| avatarUrl | string \| null | 头像 URL |
| createdAt | string (ISO-8601) | 创建时间 |

**错误示例**

- 用户名已存在：`{ "success": false, "message": "用户名已存在", "code": 400 }`
- 无效角色：`{ "success": false, "message": "无效的角色: xxx", "code": 400 }`

---

### POST /api/auth/login

用户登录（无需鉴权）。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | string | 是 | 用户名 |
| password | string | 是 | 明文密码 |

**成功响应 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| token | string | JWT，有效期默认 24 小时 |
| user | object | 同注册返回的用户信息（不含 password） |

**错误示例**

- 用户名或密码错误：`{ "success": false, "message": "用户名或密码错误", "code": 401 }`

---

### GET /api/auth/me

获取当前登录用户（需鉴权）。

**请求头**

| 字段 | 说明 |
|------|------|
| Authorization | `Bearer <token>` |

**成功响应 `data`**

与注册接口返回的用户信息结构相同（不含 password）。

**错误示例**

- 未登录或令牌缺失：HTTP 401，`{ "success": false, "message": "未登录或令牌缺失", "code": 401 }`
- 令牌无效或已过期：HTTP 401，`{ "success": false, "message": "令牌无效或已过期", "code": 401 }`
- 用户不存在：`{ "success": false, "message": "用户不存在", "code": 404 }`

---

## 工单 Tickets

鉴权：以下接口均需 `Authorization: Bearer <token>`。角色以库中枚举为准：`CUSTOMER` / `AGENT` / `ADMIN`。

### 工单对象 Ticket

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string (UUID) | 工单 ID |
| customerId | string (UUID) | 提交客户 ID |
| customerUsername | string | 客户用户名 |
| title | string | 标题 |
| description | string | 描述 |
| category | string | 自动分类：`退货` / `物流` / `账户` / `其他` |
| status | string | `PENDING` / `PROCESSING` / `RESOLVED` / `CLOSED` |
| assignedTo | string (UUID) \| null | 指派客服用户 ID |
| aiSuggestedReply | string | AI 建议回复 |
| createdAt | string (ISO-8601) | 创建时间 |
| updatedAt | string (ISO-8601) | 更新时间（乐观锁依据） |

### 分页对象 PageResult

| 字段 | 类型 | 说明 |
|------|------|------|
| content | array | 当前页数据 |
| total | number | 总条数 |
| page | number | 当前页（从 **0** 开始） |
| size | number | 每页大小 |
| totalPages | number | 总页数 |

---

### POST /api/tickets

客户提交工单（需鉴权，**仅 CUSTOMER**）。

服务端根据 `title` + `description` 关键词自动分类：

1. 含「退货」或「退款」→ `退货`
2. 含「物流」或「快递」→ `物流`
3. 含「账户」或「登录」→ `账户`
4. 其他 → `其他`

并写入默认 `aiSuggestedReply`：`感谢您的反馈，我们已收到工单，预计 24 小时内处理。`；初始 `status` 为 `PENDING`。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 标题，非空 |
| description | string | 是 | 描述，非空 |

**成功响应 `data`**

完整 Ticket 对象。

**错误示例**

- 非客户：`{ "success": false, "message": "仅客户可提交工单", "code": 403 }`
- 参数校验失败：HTTP 400，`code: 400`

---

### GET /api/tickets

获取工单列表（需鉴权）。

- **CUSTOMER**：仅返回自己的工单（创建时间倒序），支持分页
- **AGENT / ADMIN**：返回全部工单（创建时间倒序），支持 `status` 筛选与分页

**查询参数**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | 状态筛选（客服/管理员有效），如 `PENDING`（大小写不敏感） |
| page | number | 否 | 页码，默认 `0` |
| size | number | 否 | 每页大小，默认 `20`，最大 `100` |

**成功响应 `data`**

`PageResult<Ticket>`。

---

### GET /api/tickets/{id}

获取工单详情（需鉴权）。

- **CUSTOMER**：仅可查看自己的工单
- **AGENT / ADMIN**：可查看任意工单

**成功响应 `data`**

在 Ticket 基础上增加：

| 字段 | 类型 | 说明 |
|------|------|------|
| messages | array | 留言列表，按 `createdAt` 升序 |
| messages[].id | string (UUID) | 留言 ID |
| messages[].ticketId | string (UUID) | 工单 ID |
| messages[].userId | string (UUID) | 发送者用户 ID（对应实体 sender） |
| messages[].username | string | 发送者用户名 |
| messages[].content | string | 留言内容 |
| messages[].createdAt | string (ISO-8601) | 创建时间 |
| feedback | object \| null | 评价信息（无则 null） |
| feedback.id | string (UUID) | 评价 ID |
| feedback.ticketId | string (UUID) | 工单 ID |
| feedback.rating | number | 评分 1-5 |
| feedback.comment | string \| null | 评价留言 |
| feedback.createdAt | string (ISO-8601) | 创建时间 |

**错误示例**

- 不存在：`{ "success": false, "message": "工单不存在", "code": 404 }`
- 无权限：`{ "success": false, "message": "无权访问该工单", "code": 403 }`

---

### PATCH /api/tickets/{id}

更新工单（需鉴权）。使用请求体中的 `updatedAt` 与数据库比较做乐观锁；不一致时返回 **HTTP 409**。

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| updatedAt | string (ISO-8601) | 是 | 客户端持有的工单 `updatedAt`，须与库中一致 |
| status | string | 否 | **客服/管理员**：目标状态 `PENDING`/`PROCESSING`/`RESOLVED`/`CLOSED` |
| assignedTo | string (UUID) | 否 | **客服/管理员**：指派处理人（须为 AGENT/ADMIN） |
| extraMessage | string | 否 | **客户**：追加留言；若当前为 `RESOLVED`/`CLOSED` 则改回 `PENDING` |

**角色规则**

- **AGENT / ADMIN**：可改 `status`、`assignedTo`；忽略 `extraMessage`（留言请用独立留言接口）
- **CUSTOMER**：仅可通过 `extraMessage` 追加留言；不可改 `status` / `assignedTo`

**成功响应 `data`**

更新后的 Ticket 对象。

**错误示例**

- 乐观锁冲突：HTTP 409，`{ "success": false, "message": "工单已被他人修改，请刷新后重试", "code": 409 }`
- 缺少 updatedAt：`{ "success": false, "message": "updatedAt 不能为空", "code": 400 }`
- 客户未提供留言：`{ "success": false, "message": "请提供 extraMessage 追加留言", "code": 400 }`

---

## 统计 Stats

### GET /api/stats/daily

获取今日工单统计（需鉴权，**仅管理员** `ADMIN`）。

数据来源：真实查询 `tickets` 表——今日新增按 `createdAt` 落在当天 `[00:00, 次日 00:00)` 计数；各状态为全库按 `status` 分组计数。

**请求头**

| 字段 | 说明 |
|------|------|
| Authorization | `Bearer <token>` |

**成功响应 `data`**

| 字段 | 类型 | 说明 |
|------|------|------|
| date | string | 统计日期，`yyyy-MM-dd` |
| newTicketsToday | number | 今日新增工单数 |
| statusCounts | object | 各状态数量 |
| statusCounts.pending | number | 待处理 |
| statusCounts.processing | number | 处理中 |
| statusCounts.resolved | number | 已解决 |
| statusCounts.closed | number | 已关闭 |

**成功响应示例**

```json
{
  "success": true,
  "data": {
    "date": "2026-08-03",
    "newTicketsToday": 0,
    "statusCounts": {
      "pending": 0,
      "processing": 0,
      "resolved": 0,
      "closed": 0
    }
  },
  "code": 200
}
```

**错误示例**

- 未登录：HTTP 401，`{ "success": false, "message": "未登录或令牌缺失", "code": 401 }`
- 非管理员：`{ "success": false, "message": "无权限：仅管理员可查看统计", "code": 403 }`

---

## 留言与评价 Message / Feedback

与工单详情中的 `messages[]` / `feedback` 字段结构一致。

### POST /api/tickets/{id}/messages

追加工单留言（需鉴权）。客户或客服/管理员均可。

**路径参数**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string (UUID) | 工单 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| content | string | 是 | 留言内容，不能为空 |

**成功响应 `data`（Message）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string (UUID) | 留言 ID |
| ticketId | string (UUID) | 工单 ID |
| userId | string (UUID) | 发送者用户 ID |
| username | string | 发送者用户名 |
| content | string | 留言内容 |
| createdAt | string (ISO-8601) | 创建时间 |

**业务规则**

- 客户仅可对自己的工单留言；客服 `AGENT` / 管理员 `ADMIN` 可对任意工单留言
- 客服/管理员留言：不改变工单状态
- 客户留言：若工单状态为 `RESOLVED` 或 `CLOSED`，先改为 `PENDING`，再保存留言

**错误示例**

- 留言内容为空：`{ "success": false, "message": "留言内容不能为空", "code": 400 }`
- 工单不存在：`{ "success": false, "message": "工单不存在", "code": 404 }`
- 无权操作：`{ "success": false, "message": "无权操作该工单", "code": 403 }`

---

### POST /api/tickets/{id}/feedback

提交工单评价（需鉴权，**仅客户**，且工单状态为 `RESOLVED`）。

**路径参数**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string (UUID) | 工单 ID |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| rating | number | 是 | 评分，整数 1-5 |
| comment | string | 否 | 评价备注 |

**成功响应 `data`（Feedback）**

| 字段 | 类型 | 说明 |
|------|------|------|
| id | string (UUID) | 评价 ID |
| ticketId | string (UUID) | 工单 ID |
| rating | number | 评分 1-5 |
| comment | string \| null | 评价备注 |
| createdAt | string (ISO-8601) | 创建时间 |

**业务规则**

- 仅工单所属客户可评价；客服/管理员不可评价
- 仅当工单状态为 `RESOLVED` 时可评价
- 每个工单仅可评价一次（DB `ticket_id` 唯一；业务层先查再插）

**错误示例**

- 非客户：`{ "success": false, "message": "仅客户可提交评价", "code": 403 }`
- 非本人工单：`{ "success": false, "message": "无权评价该工单", "code": 403 }`
- 状态非 resolved：`{ "success": false, "message": "仅已解决的工单可评价", "code": 400 }`
- 已评价：`{ "success": false, "message": "该工单已评价", "code": 400 }`
- 评分越界：`{ "success": false, "message": "rating: 评分最小为 1", "code": 400 }`（或最大值校验）

---

## 健康检查

### GET /api/health

无需鉴权。返回 `{ "status": "UP" }`（非统一 Result 包装）。

---

## 相关文档

- 产品需求：[PRD.md](./PRD.md)
- 技术设计：[design.md](./design.md)
- 多 Agent 提示词实录：[PROMPTS.md](./PROMPTS.md)
- 测试说明：[TESTING.md](./TESTING.md)
- 文档索引：[README.md](./README.md)
