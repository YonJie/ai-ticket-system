# AI 智能客服工单系统

Vue3 + Vite 前端、Spring Boot 2.x 后端、Neon Postgres 数据库，部署于 Vercel。

## 项目结构

```text
ai-ticket-system/
├── frontend/          # Vue3 + TypeScript + Vite（构建输出 dist）
├── backend/           # Spring Boot 2.7 + Java 8（Dockerfile 多阶段构建）
├── docs/              # PRD / 设计 / API 契约 / 测试 / AI 规则
├── vercel.json        # Vercel 前后端构建与路由
└── README.md
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue3、Vite、TypeScript、Vue Router、Pinia、Axios、Element Plus |
| 后端 | Spring Boot 2.7、Java 8、Spring Web、Spring Data JPA、Spring Security、JJWT |
| 数据库 | Neon Postgres（本地可用 `local` profile + H2 快速验证） |
| 部署 | Vercel Services（前端 Vite + 后端 container/Dockerfile） |

## 本地开发

### 前端

```bash
cd frontend
npm install
npm run dev
```

访问：http://localhost:5173（`/api` 代理到 `http://localhost:8080`）

生产构建（输出目录 `frontend/dist`）：

```bash
cd frontend
npm run build
```

### 后端

本地快速启动（内存 H2，无需 Neon）：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

连接 Neon Postgres（默认 profile）：

```bash
cd backend
# 设置 DATABASE_URL（JDBC 形式）与 JWT_SECRET 后：
mvn spring-boot:run
```

打包：

```bash
cd backend
mvn clean package
```

产物：`backend/target/ai-ticket-backend-0.0.1-SNAPSHOT.jar`

健康检查：http://localhost:8080/api/health

### 本地 Docker 测试（可选）

需已安装 Docker，且已能访问 Neon（或在容器中注入等价环境变量）：

```bash
cd backend
mvn clean package -DskipTests
docker build -t ai-ticket-backend .
docker run --rm -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://HOST/neondb?sslmode=require" \
  -e JWT_SECRET="your-secret" \
  ai-ticket-backend
```

> 说明：当前 `Dockerfile` 为多阶段构建，镜像内会执行 Maven 打包；本地也可先 `mvn clean package` 再构建以加快迭代（此时仍建议保留多阶段 Dockerfile 以便 Vercel 云端构建）。

### 环境变量（本地）

复制 `.env.example` 为 `.env`，至少配置：

| 变量 | 说明 |
|------|------|
| `DATABASE_URL` | **JDBC** 连接串，如 `jdbc:postgresql://HOST/neondb?sslmode=require`（不要用 `postgresql://` 裸协议） |
| `JWT_SECRET` | JWT 签名密钥，生产环境务必更换 |

## 部署到 Vercel

> **说明**：Vercel 已不再提供 `@vercel/docker` 构建器。本项目使用官方 **Services** 配置：前端为 Vite 静态服务，后端为容器（`runtime: "container"` + `Dockerfile`）。

### 1. 导入项目

1. 打开 [Vercel Dashboard](https://vercel.com/dashboard) → **Add New…** → **Project**
2. 导入本 Git 仓库，**Root Directory** 保持仓库根目录（使用根目录 `vercel.json`）
3. Framework / 项目类型选择支持 **Services** 的选项（若控制台有 Framework Preset，选 Other / Services，由 `vercel.json` 驱动）

### 2. 配置环境变量（必填）

在 **Project Settings → Environment Variables** 中添加（Production / Preview 均建议配置）：

| 变量名 | 示例 / 说明 |
|--------|-------------|
| `DATABASE_URL` | `jdbc:postgresql://ep-xxxx.neon.tech/neondb?sslmode=require` |
| `JWT_SECRET` | 足够长的随机字符串 |

也可在 CLI 中添加（需已 `vercel link`）：

```bash
vercel env add DATABASE_URL
vercel env add JWT_SECRET
```

### 3. 构建与路由（`vercel.json`）

根目录 `vercel.json` 要点：

- **frontend**：`root: frontend/`，`framework: vite`（构建输出 `dist`）
- **backend**：`runtime: container`，`entrypoint: Dockerfile`（多阶段 Maven + JRE）
- 路由：`/api/*` → `backend` 服务；其余 → `frontend` 服务

### 4. 部署

```bash
# 在仓库根目录
vercel        # Preview
vercel --prod # Production
```

或推送到已连接的 Git 分支，由 Vercel 自动部署。

### 5. 部署后验证

- 前端：`https://<your-project>.vercel.app/`
- 健康检查：`https://<your-project>.vercel.app/api/health`
- 演示账号（若 `DataInitializer` 已写入）：`customer` / `agent` / `admin`，密码 `123456`

### 6. 若仍报错

| 现象 | 处理 |
|------|------|
| `@vercel/docker` not published | 确认已拉取含新 `vercel.json`（Services）的提交后重新部署 |
| Services / container 不可用 | 账号套餐需支持 [Vercel Services / Container](https://vercel.com/docs/services)；或将后端单独部署到 Railway/Fly 等，前端仍用 Vercel，并用 rewrite 代理 `/api` |
| 后端启动失败 | 检查 `DATABASE_URL` 是否为 **JDBC** 形式（`jdbc:postgresql://...`），以及 `JWT_SECRET` 是否已配置 |

## 相关文档

- 文档索引：[`docs/README.md`](docs/README.md)
- 产品需求：[`docs/PRD.md`](docs/PRD.md)
- 技术设计：[`docs/design.md`](docs/design.md)
- API 契约：[`docs/api-contract.md`](docs/api-contract.md)
- 测试说明：[`docs/TESTING.md`](docs/TESTING.md)
- 多 Agent 提示词实录：[`docs/PROMPTS.md`](docs/PROMPTS.md)
