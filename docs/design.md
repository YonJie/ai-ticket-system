# 技术设计文档（design）

## 1. 总体架构

```text
浏览器 (Vue3 SPA)
    │  /api/* （同域）
    ▼
Vercel 路由 (vercel.json)
    ├─ /api/*  → 后端 Docker (Spring Boot)
    └─ 其余    → 前端静态资源 (frontend/dist)
                    │
                    ▼
              Neon Postgres
```

本地开发时，Vite 将 `/api` 代理到 `http://localhost:8080`。

## 2. 技术选型

| 层 | 选型 | 说明 |
|----|------|------|
| 前端 | Vue3 + Vite + TS + Pinia + Vue Router + Element Plus + Axios | SPA；构建产物 `dist` |
| 后端 | Spring Boot 2.7 / Java 8 / JPA / Validation / Security(PasswordEncoder) / JJWT | 无状态 JWT |
| 数据库 | Neon Postgres | 环境变量 `DATABASE_URL`（JDBC 形式） |
| AI | DeepSeek `deepseek-v4-flash`（关 thinking）+ 规则回退 | `DEEPSEEK_API_KEY` 可选；见 [AI_PROMPTS.md](./AI_PROMPTS.md) |
| 部署 | Vercel Services：前端 Vite + 后端 container/Dockerfile | 见根目录 `vercel.json` |

## 3. 后端分层

```text
controller  →  DTO 入参/出参，调用 service
service      →  业务规则、权限、事务
repository   →  Spring Data JPA
entity       →  表映射（snake_case 列 ↔ camelCase 字段）
interceptor  →  JwtInterceptor：校验 Bearer，写入 request.userId
```

统一响应：`com.aiticket.common.Result`  
业务异常：`BusinessException` + `GlobalExceptionHandler`（映射 400/401/403/404/409 等）。

### 鉴权白名单

- `/api/health`
- `/api/auth/register`
- `/api/auth/login`

其余 `/api/**` 需有效 JWT。

### 端口

- 本地默认 `8080`
- 容器 / Vercel：`server.port=${PORT:8080}`

## 4. 数据模型（概要）

### users

| 列 | 说明 |
|----|------|
| id | UUID |
| username | 唯一 |
| password | BCrypt |
| role | `CUSTOMER` / `AGENT` / `ADMIN` |
| avatar_url | 可选 |
| created_at | 创建时间 |

### tickets

| 列 | 说明 |
|----|------|
| id | UUID |
| customer_id | FK → users |
| title / description | 标题与描述 |
| category | DeepSeek 或规则自动分类结果 |
| status | `PENDING` / `PROCESSING` / `RESOLVED` / `CLOSED` |
| assigned_to | FK → users，可空 |
| ai_suggested_reply | AI / 回退建议回复 |
| created_at / updated_at | 时间戳；更新用乐观锁（`updatedAt` + `@Version`） |

### messages

工单留言：`ticket_id`、`sender`（user）、`content`、`created_at`。

### feedbacks

工单评价：`ticket_id`（唯一）、`rating`（1–5）、`comment`、`created_at`。

## 5. 前端设计

### 目录约定

- 页面：`frontend/src/views`（PascalCase）
- 通用组件：`frontend/src/components`
- API：`frontend/src/api`
- 状态：`frontend/src/stores`（Pinia）
- Axios 封装：`frontend/src/utils/request.ts`（自动 Bearer、401 清会话、ElMessage）

### 状态与路由

- `user` store：token / userInfo / login / register / logout，持久化 localStorage
- `ticket` store：列表、详情、创建、更新、留言、评价
- 路由守卫：按 `requiresAuth`、`roles`、`guest` 分流

### 字段命名

前后端 JSON 一律 **camelCase**；数据库 **snake_case**。

## 6. 部署设计

### 构建

- 前端：`npm run build` → `frontend/dist`
- 后端：`backend/Dockerfile` 多阶段（Maven 打包 + JRE 运行）

### 路由（vercel.json）

使用 Vercel **Services**（不再使用已下线的 `@vercel/docker` 构建器）：

1. `frontend`：`framework: vite`，`root: frontend/`
2. `backend`：`runtime: container`，`entrypoint: Dockerfile`
3. rewrite：`/api/*` → `backend`；其余 → `frontend`

### 环境变量

| 变量 | 用途 |
|------|------|
| `DATABASE_URL` | JDBC：`jdbc:postgresql://...` |
| `JWT_SECRET` | JWT 签名密钥 |
| `DEEPSEEK_API_KEY` | 可选；创建工单 AI 分类与建议回复 |
| `DEEPSEEK_BASE_URL` / `DEEPSEEK_MODEL` / `DEEPSEEK_TIMEOUT_MS` | 可选，默认见 README |
| `PORT` | 容器监听端口；Vercel Container 默认 **80**（`Dockerfile.vercel`），本地 Docker 可用 8080 |

详细步骤见仓库根 [README.md](../README.md)。

## 7. 安全注意

- Spring Security 当前 `permitAll`，鉴权依赖自定义 JWT 拦截器（非 Filter Chain 鉴权）
- 生产必须配置强 `JWT_SECRET`，勿使用默认值
- Neon 连接串须为 JDBC 前缀；`postgresql://` 裸协议 Spring 无法直接使用
- 演示账号仅用于演示环境，生产应禁用或改密

## 8. 扩展方向

- 多轮 AI 对话 / RAG 知识库
- 附件、通知、审计日志
- 将鉴权迁移到 Spring Security Filter，统一异常与 CORS
