# 测试说明（TESTING）

本文档描述 AI 智能客服工单系统的自动化测试覆盖范围、运行方式与已知风险。

## 如何运行

### 后端

```bash
cd backend
mvn test
```

依赖：JDK 8+、Maven。测试使用 H2 内存库（见 `backend/src/test/resources/application.properties`），无需 Neon / `DATABASE_URL`。Surefire 设置 `ai.ticket.skip-dotenv=true`，避免本地 `.env` 覆盖测试数据源。

### 前端

```bash
cd frontend
npm install
npm test
```

监听模式：`npm run test:watch`。

## 覆盖场景

| 场景 | 位置 | 方式 |
|------|------|------|
| 用户注册成功 | `AuthControllerTest` | MockMvc + Mock `AuthService` |
| 用户登录成功（token + user） | `AuthControllerTest` | MockMvc |
| 注册参数校验失败（400） | `AuthControllerTest` | MockMvc + `GlobalExceptionHandler` |
| 创建工单并透传自动分类结果 | `TicketControllerTest` | MockMvc；响应含 `category` / `PENDING` |
| 自动分类关键词规则（回退） | `RuleBasedAiAssistServiceTest` | 规则分类 + 固定建议回复 |
| DeepSeek 响应解析 | `DeepSeekAiClientTest` | 不访问外网；合法/非法 category |
| AI 助手编排与回退 | `AiAssistServiceTest` | mock DeepSeek；无 Key / 失败 / 部分结果 |
| 修改工单状态 | `TicketControllerTest` | MockMvc `PATCH` → `PROCESSING` |
| 追加工单留言 | `TicketInteractionControllerTest` | MockMvc |
| 提交工单评价 | `TicketInteractionControllerTest` | MockMvc |
| 应用上下文可启动 | `AiTicketBackendApplicationTests` | `@SpringBootTest` + H2 |
| 登录表单渲染 | `LoginView.spec.ts` | Vitest + `@vue/test-utils` |
| 登录提交与客户跳转 | `LoginView.spec.ts` | mock `userStore.login` / router |
| 工单列表挂载拉取 | `CustomerTicketsView.spec.ts` | mock `fetchTickets` |
| 工单列表展示标题 | `CustomerTicketsView.spec.ts` | store 注入数据 |
| 工单列表空状态文案 | `CustomerTicketsView.spec.ts` | 空数组 |

## 测试策略摘要

- **后端 Controller**：`@WebMvcTest` 切片，Service 层 `@MockBean`；鉴权接口通过 `MockAuth.withUserId` + stub `JwtUtil.getUserIdFromToken` 模拟 JWT 拦截器写入的 `userId`。
- **自动分类**：规则测在 `RuleBasedAiAssistService`；DeepSeek 解析与编排用 mock，不打外网；创建接口测验证 Controller 透传 `category`。
- **前端**：组件级测试，mock Pinia store 与 vue-router，不发起真实 HTTP。

## 已知风险

1. **未覆盖真实 JWT 拦截器行为**：切片测试 stub 了 `JwtUtil`，不验证过期、伪造、缺失 token 等端到端鉴权路径。
2. **未覆盖真实数据库与业务边界**：Service 多为 mock；乐观锁冲突、角色权限（如非客户建单）、重复评价等需集成测试补充。
3. **自动分类非端到端**：规则 / DeepSeek mock / Controller 创建用例分离；未验证「真实 DeepSeek → DB 落库 category」全链路。
4. **前端非 E2E**：未用 Playwright/Cypress；Element Plus 表单校验、真实 API 错误提示、路由守卫未完整覆盖。
5. **H2 与 Neon Postgres 差异**：`contextLoads` 与测试配置使用 H2（`MODE=PostgreSQL`），生产方言/约束行为可能不同。
6. **演示数据初始化**：全量 `@SpringBootTest` 会跑 `DataInitializer` 写入演示用户，勿假设测试库为空。
7. **npm 镜像**：部分私有 registry 可能缺少 `@vue/test-utils` 等包；必要时使用 `https://registry.npmjs.org/` 安装。
8. **DeepSeek 依赖外网**：生产需配置 `DEEPSEEK_API_KEY`；未配置时行为与旧版规则回退一致。

## 相关文档

- 产品需求：[PRD.md](./PRD.md)
- 技术设计：[design.md](./design.md)
- API 契约：[api-contract.md](./api-contract.md)
- 多 Agent 提示词实录：[PROMPTS.md](./PROMPTS.md)
- 文档索引：[README.md](./README.md)
