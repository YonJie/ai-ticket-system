# 多 Agent 协同开发提示词实录（PROMPTS）

> 本文档记录本项目在 Cursor 中通过**多 Agent / 子 Agent 并行与串行协作**完成开发时使用的提示词（Prompts）。  
> 考察重点：任务如何拆分、并行时如何避免冲突、共享约定如何下发、收尾如何整合。  
> 业务侧「工单自动分类关键词 / 默认建议回复」见 [PRD.md](./PRD.md) 与 [design.md](./design.md)，不在此重复。

## 1. 协作总览

```text
阶段 A 脚手架（串行）
  └─ Agent：初始化前后端 + Dockerfile + vercel.json

阶段 B 后端底座（串行，可并行拆给不同会话）
  ├─ Agent：JWT / Result / 拦截器 / 异常处理
  └─ Agent：JPA 实体 + Repository + 演示用户种子

阶段 C 后端业务（并行 4 子 Agent + 1 整合 Agent）★ 多 Agent 核心
  ├─ SubAgent Auth：注册 / 登录 / me
  ├─ SubAgent Ticket：工单 CRUD + 自动分类 + 乐观锁
  ├─ SubAgent Interaction：留言 / 评价
  ├─ SubAgent Stats：每日统计
  └─ SubAgent Integrate：编译核对 + 契约完整性

阶段 D 前端（串行，可与 C 并行启动）
  └─ Agent：路由 + Pinia + 页面（先 mock 后接真 API）

阶段 E 质量与交付（串行）
  ├─ Agent：前后端自动化测试 + TESTING.md
  ├─ Agent：Vercel 部署配置完善
  ├─ Agent：docs 空文档补全 / PROMPTS 实录
  └─ （可复用）Vercel 同域部署规范 Prompt → [VERCEL_DEPLOY_PROMPT.md](./VERCEL_DEPLOY_PROMPT.md)
```

**共享约定（写入各并行 Agent 提示词，降低冲突）**


| 约定     | 内容                                       |
| ------ | ---------------------------------------- |
| API 响应 | `{ success, data, message?, code? }`     |
| 命名     | DB `snake_case`；Java/前端 JSON `camelCase` |
| 鉴权     | JWT 拦截器；`userId` 写入 request 属性           |
| 契约     | 改接口必须同步 `docs/api-contract.md`           |
| 并行纪律   | 尽量只改本模块文件；共享文件谨慎合并；不提交 git（由编排方统一提交）     |
| 复用     | 先调研再写，禁止重复造轮子                            |


---

## 2. 阶段 A — 项目脚手架

**角色**：脚手架 Agent（会话 [初始化项目结构](533d5bb7-c78c-42b6-bd0d-4d4be35c700e)）  
**时间**：2026-08-03 约 11:38  

### 提示词

```text
我要开发“AI 智能客服工单系统”，技术栈 Vue3 + Vite（前端）、Spring Boot 2.x + Java 8（后端）、Neon Postgres（数据库），前后端均部署到 Vercel（后端通过 Docker 容器方式部署）。

请帮我在当前空仓库中初始化以下项目结构：
- /frontend：用 Vite 创建 Vue3 + TypeScript 项目，集成 Vue Router、Pinia、Axios，安装 Element Plus。
- /backend：用 Spring Initializr 创建 Spring Boot 2.x 项目，使用 Maven，Java 8，添加依赖：Spring Web、Spring Data JPA、PostgreSQL Driver、Spring Security（仅用于密码编码）、jjwt（JWT）、Lombok、Spring Boot DevTools。
- 创建 .gitignore（排除 node_modules、target、.env、.idea）。
- 创建 README.md 占位。
- 创建 backend/Dockerfile（用于 Vercel 部署），内容：基于 openjdk:8-jre-alpine，复制 target/*.jar 为 app.jar，ENTRYPOINT ["java","-jar","/app.jar"]。
- 创建根目录 vercel.json，配置 Docker 部署和路由规则（使 /api/* 转发到后端，其余请求转发到前端静态资源）。

完成后，分别运行 frontend 和 backend 的依赖安装，确保项目可启动。
```

---

## 3. 阶段 B — 后端基础设施

### 3.1 JWT / 统一响应 / 异常处理

**角色**：基础设施 Agent（会话 [后端基础设施](0b3a91ab-3571-4795-8c49-d776f99b3c67)）  
**时间**：约 14:16  

### 提示词

```text
请完成 Spring Boot 后端基础设施（Spring Boot 2.x, Java 8）：

1. 在 /backend/src/main/resources/application.properties 中配置：
   - spring.datasource.url=${DATABASE_URL}
   - spring.datasource.driver-class-name=org.postgresql.Driver
   - spring.jpa.hibernate.ddl-auto=update
   - spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

2. 创建 JWT 工具类 JwtUtil.java：
   - 使用 io.jsonwebtoken.Jwts，密钥从环境变量 JWT_SECRET 读取，设置过期时间 24h。
   - 方法：generateToken(Long userId), getUserIdFromToken(String token)。

3. 创建 JWT 拦截器 JwtInterceptor.java 实现 HandlerInterceptor：
   - 从请求头 Authorization: Bearer xxx 提取 token。
   - 验证 token 并将 userId 存入 request 属性（如 request.setAttribute("userId", userId)）。
   - 未登录返回 401。

4. 注册拦截器 WebConfig.java 实现 WebMvcConfigurer，添加拦截器，排除 /auth/** 路径。

5. 创建全局异常处理器 GlobalExceptionHandler.java（@RestControllerAdvice）：
   - 处理业务异常、参数校验异常等，统一返回 Result.error(message)。

6. 创建统一响应类 Result.java：包含 success, data, message, code。

7. 在 pom.xml 中添加 jjwt 依赖（版本 0.9.1）以及 spring-boot-starter-security 仅用于密码加密（BCryptPasswordEncoder）。
```

### 3.2 实体 / Repository / 演示数据

**角色**：数据模型 Agent（会话 [JPA 实体与 Repository](60c201f2-253b-4d14-a3a8-2db9422f391b)）  
**时间**：约 14:19  

### 提示词

```text
请创建 JPA 实体和 Repository（使用 Lombok）：

1. 实体类（使用 @Entity，@Table，@Id，@GeneratedValue，@Column 等）：
   - User：id(UUID), username, passwordHash, role (枚举: Customer, Agent, Admin), avatarUrl, createdAt
   - Ticket：id(UUID), customer (ManyToOne), title, description, category, status (枚举: Pending, Processing, Resolved, Closed), assignedTo (ManyToOne, 可为空), aiSuggestedReply, createdAt, updatedAt (带 @Version 用于乐观锁)
   - Message：id(UUID), ticket (ManyToOne), sender (ManyToOne), content, createdAt
   - Feedback：id(UUID), ticket (OneToOne), rating, comment, createdAt

2. 为每个实体创建对应的 JpaRepository 接口。

3. 在启动类中添加 CommandLineRunner，初始化三个角色各一个用户（用于演示，密码为 "123456"）。
```

---

## 4. 阶段 C — 后端业务并行（多 Agent 核心）

**编排会话**：[后端 API 并行实现](3edcd4e1-b824-4cfc-a620-5f5f6bf47f70)  
**模式**：主编排 Agent 启动 4 个并行 SubAgent，再启动 1 个整合 SubAgent。  
**时间**：约 14:26–14:31  

主会话用户侧入口提示（精简）：

```text
实现认证相关：
- POST /api/auth/register：…
- POST /api/auth/login：…
- GET /api/auth/me：…
```

随后主编排方将任务拆成下列完整子 Agent 提示词（含并行冲突约束）。

### 4.1 SubAgent — Auth

**职责**：认证三接口  

```text
在项目 ai-ticket-system 中实现后端认证相关 API。这是 Vue3 + Spring Boot 2.x + Neon Postgres 的 AI 智能客服工单系统。

## 需要实现的接口
1. POST /api/auth/register — username, password, role（默认 customer）；用户名唯一；BCrypt；返回用户信息
2. POST /api/auth/login — 校验密码；返回 { token, user }
3. GET /api/auth/me — 从 request 属性取 userId；返回当前用户

## 项目约定（必须遵守）
- API：{ success, data, message?, code? }
- DB snake_case；Java/前端 camelCase
- JWT 拦截器鉴权；DATABASE_URL；改字段同步 docs/api-contract.md

## 实现要求
1. 先调研 User / JwtUtil / 拦截器 / Result 等，复用已有基础设施
2. register/login 放行；/me 需鉴权；返回不含 password
3. 不要提交 git；不做无关重构

## 完成后请返回
文件列表、三接口摘要、userId 如何写入 request、配置注意点
```

### 4.2 SubAgent — Ticket

**职责**：工单 CRUD + 自动分类 + 乐观锁  
**并行约束**：明确「另一 agent 在做 Auth，勿破坏」

```text
在项目 ai-ticket-system 中实现后端工单相关 API（需鉴权）。

注意：可能有另一个 agent 正在并行实现认证接口。请复用 JWT 拦截器、User、统一响应；避免破坏认证相关改动。

## 接口
1. POST /api/tickets — 客户提单；关键词自动分类；默认 AI 建议回复
2. GET /api/tickets — 客户仅自己；客服/管理员全部 + status 筛选 + 分页
3. GET /api/tickets/{id} — 详情含留言、评价
4. PATCH /api/tickets/{id} — 客服改状态/指派；客户 extraMessage；updatedAt 乐观锁 → 409

## 项目约定
（同 Auth：统一响应、命名、JWT、api-contract）

## 实现要求
- 角色以项目枚举为准；分页结构写入契约
- 尽量只改工单相关文件；共享配置谨慎合并
- 不要提交 git

## 完成后请返回
文件列表、四接口摘要、分类与乐观锁要点、权限摘要、契约更新说明
```

### 4.3 SubAgent — Message / Feedback

**职责**：留言与评价独立接口  
**并行约束**：与 Auth、Ticket CRUD 并行，注意 `extraMessage` 重叠语义

```text
在项目 ai-ticket-system 中实现留言和评价接口。

注意：可能有其他 agent 正在并行实现认证和工单 CRUD（含 PATCH extraMessage）。尽量只新增/完善留言与评价相关代码。

## 接口
1. POST /api/tickets/{id}/messages — 客户/客服留言；客户在 resolved/closed 时先改 pending
2. POST /api/tickets/{id}/feedback — 仅客户且 resolved；rating 1-5；每单仅一次

## 项目约定 + 实现要求
（统一响应、JWT、api-contract；实体命名以仓库为准；不要提交 git）

## 完成后请返回
文件列表、两接口字段、状态变更与唯一性规则、契约更新说明
```

### 4.4 SubAgent — Stats

**职责**：管理员每日统计  

```text
在项目 ai-ticket-system 中实现统计接口。

注意：可能有其他 agent 正在并行实现认证、工单、留言/评价。尽量只改统计相关文件。

## 需要实现
GET /api/stats/daily（仅管理员）
- 今日新增 + 各状态计数
- 优先真实查询；否则 mock 并注释标明
- 非管理员 403

## 完成后返回
文件列表、接口字段、真实/mock、管理员鉴权方式
```

### 4.5 SubAgent — Integrate（整合核对）

**职责**：编译与契约收口，解决并行残留  

```text
刚才有 4 个并行 agent 分别实现了：
1. 认证  2. 工单  3. 留言评价  4. 统计

请做整合核对（不要提交 git，不要做大范围重构）：
1. backend 下 mvn compile，确认通过
2. 检查共享文件无冲突残留（Repository、DTO、GlobalExceptionHandler、api-contract、WebConfig）
3. 确认全部接口路径正确；JWT 白名单仅 health + register + login
4. 明显破损则最小修复；正常则不改代码

完成后返回：编译结果、是否修复、接口齐全简表、api-contract 覆盖情况
```

**协同设计要点（考试可答）**

1. **按限界上下文拆分**：Auth / Ticket / Interaction / Stats，减少文件交叉。
2. **提示词显式声明并行方**：告诉 Agent「别人在改什么」。
3. **共享契约文件**：`api-contract.md` 作为对齐面，但并行写入易冲突 → 需要 Integrate 步骤核对。
4. **禁止子 Agent 自行 git commit**：避免历史交错。
5. **先调研再改**：降低重复实现与覆盖他人代码概率。

---

## 5. 阶段 D — 前端（可与阶段 C 并行会话）

**角色**：前端 Agent（会话 [前端 mock 页面](c9b1fae8-591b-4a95-9753-d77edd8d7969)）  
**时间**：约 14:27  

### 提示词

```text
基于 api-contract.md（后端未完成时可用 mock），在 /frontend/src 中实现：

1. 路由：登录、注册、客户首页、新建工单、工单详情、客服后台、客服工单处理页。
2. Pinia store：userStore（token/userInfo/role/登录注册退出）；ticketStore（列表/详情/加载状态）。
3. request.ts：封装 axios，自动加 token，401 跳转登录，ElMessage 统一错误。
4. 页面：LoginView / RegisterView / CustomerTicketsView / NewTicketView / TicketDetailView / AgentDashboard / AgentTicketDetail。
5. 先使用 mock 数据让页面可交互。
```

> 后续另有会话将 mock 替换为真实 API 调用（字段映射与 store 对接），与后端并行结果汇合。

---

## 6. 阶段 E — 测试、部署与文档

### 6.1 自动化测试

**角色**：测试 Agent（会话 [添加自动化测试](fd026578-db6d-4bac-8af5-7b11b0396c43)）  
**时间**：约 15:14  

```text
为项目添加测试：
1. 后端：使用 JUnit 5 + MockMvc 测试核心 Controller（注册登录、创建工单、自动分类、状态修改、留言、评价）。
2. 前端：使用 Vitest + @vue/test-utils 测试登录表单、工单列表组件。
3. 在根目录 TESTING.md 记录覆盖场景和已知风险。
```

（落地后测试说明归入 `docs/TESTING.md`。）

### 6.2 部署配置（初版）

**同一会话后续提示**（约 15:24）：

```text
完善部署配置，使前后端均部署到 Vercel：
1. 前端 build: "vite build"，输出 dist
2. 确认 backend/Dockerfile
3. 根目录 vercel.json（Docker + 路由示例）
4. Vercel 环境变量 DATABASE_URL、JWT_SECRET
5. README 补充部署步骤
本地：mvn clean package，可选 Docker 测试
```

### 6.3 文档补全

```text
补充一下根目录下的 docs 目录下的对应 md 文件内容
```

### 6.4 Vercel 同域部署规范 Prompt（可复用，2026-08-04）

**角色**：部署规范沉淀（会话：SPA 刷新 404 排查 + 部署经验收口）  
**完整正文（下次直接复制给 Agent）**：[VERCEL_DEPLOY_PROMPT.md](./VERCEL_DEPLOY_PROMPT.md)

相对 6.2 初版的关键增量：

1. 改用 Vercel **Services**（`runtime: "container"`），禁止 `@vercel/docker`
2. 双层 rewrite：顶层选 service；**frontend 内** `/(.*) → /index.html`（防 History 刷新 404）
3. `Dockerfile.vercel` + `socat` 抢 `$PORT`，规避 Java 冷启动超 15s
4. 前端 Axios `baseURL: '/api'` + Vite 本地 proxy；同域无硬编码后端域名
5. Neon `DATABASE_URL` 写法约束 + 环境变量 / CLI / 验收清单 / 故障表

**开场摘要（粘贴完整版前可先说明意图）**：

```text
请按 docs/VERCEL_DEPLOY_PROMPT.md 完成前后端同域部署到 Vercel：
Services（Vite + container）、双层 rewrite（含 SPA fallback）、
Dockerfile.vercel + socat、同域 /api、环境变量与验收清单。
禁止 @vercel/docker。完成后验证 /api/health 与子路由刷新不 404。
```

---

## 7. Agent 与产物对照表


| 阶段  | Agent / SubAgent | 主要产物                                                                     |
| --- | ---------------- | ------------------------------------------------------------------------ |
| A   | 脚手架              | `frontend/`、`backend/`、`Dockerfile`、`vercel.json`、`.gitignore`           |
| B1  | 基础设施             | `JwtUtil`、`JwtInterceptor`、`Result`、`GlobalExceptionHandler`、`WebConfig` |
| B2  | 数据模型             | User/Ticket/Message/Feedback 实体与 Repository、`DataInitializer`            |
| C1  | Auth             | `AuthController` / `AuthService`、契约 Auth 节                               |
| C2  | Ticket           | `TicketController` / `TicketService`、分类与乐观锁                              |
| C3  | Interaction      | `TicketInteractionController`、留言/评价服务                                    |
| C4  | Stats            | `StatsController` / `StatsService`                                       |
| C5  | Integrate        | 编译通过、共享文件与契约收口                                                           |
| D   | 前端               | 路由、stores、Views、`request.ts`                                             |
| E1  | 测试               | MockMvc / Vitest、`docs/TESTING.md`                                       |
| E2  | 部署               | 多阶段 Dockerfile、`vercel.json`、README 部署节                                  |
| E3  | 文档               | PRD / design / docs 索引 / 本文                                              |
| E4  | 部署规范可复用 Prompt | [VERCEL_DEPLOY_PROMPT.md](./VERCEL_DEPLOY_PROMPT.md)（Services / SPA / socat） |


---

## 8. 多 Agent 协同经验摘要

1. **垂直切片优于按层切片**：按 API 域拆 Agent，比「一人写所有 Controller、另一人写所有 Service」更少冲突。
2. **提示词要带「别人在做什么」**：并行时缺此句，易互相覆盖 `api-contract.md` / `WebConfig`。
3. **必须有 Integrate Agent**：并行结束后做编译与契约核对，比事后人工翻 diff 更稳。
4. **契约文档是双刃剑**：促进对齐，也是最高冲突热点；可约定「各 Agent 只追加自己章节」。
5. **前端可先 mock**：与后端并行，契约先行（`api-contract.md`）是对接关键。
6. **统一「不要 commit」**：提交权收口到编排方，避免多 Agent 交错历史。

---

## 9. 维护说明

- 新增一次有意义的 Agent/SubAgent 任务时，在对应阶段追加「提示词」与「产物」两行。  
- 提示词可做不影响语义的精简，但应保留：**目标接口、项目约定、并行约束、禁止事项、完成回报格式**。  
- 会话 UUID 便于回溯 Cursor agent-transcripts，可不对外公开路径细节。  
- **Vercel 部署规范**以 [VERCEL_DEPLOY_PROMPT.md](./VERCEL_DEPLOY_PROMPT.md) 为唯一完整正文；本文件 6.4 节只保留索引与增量说明，避免双份漂移。

