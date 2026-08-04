# Vercel 前后端同域部署 Prompt（可复用）

> 下次开发同类项目（Vue3/Vite 前端 + Spring Boot 后端 + Neon Postgres）时，可将下方「Prompt 正文」整段复制给 Agent，要求按此规范完成部署配置。

---

## Prompt 正文（复制以下全部内容）

```text
请为当前 monorepo 完成「前后端同域部署到 Vercel」的配置与代码适配。技术边界：Vue3 + Vite 前端、Spring Boot 2.x + Java 8 后端、Neon Postgres。必须使用 Vercel Services（禁止已下线的 @vercel/docker）。完成后保证：同域访问、SPA 刷新不 404、后端容器能通过约 15s 端口探测、/api 走后端其余走前端。

════════════════════════════════════
一、目标架构（必须遵守）
════════════════════════════════════

浏览器 → https://<project>.vercel.app
  ├─ /api/*  → backend（container / Dockerfile.vercel）
  └─ 其余    → frontend（Vite 静态）
                 ↓
           Neon Postgres

- 仓库 Root Directory = 仓库根（由根目录 vercel.json 驱动）
- 账号套餐需支持 Vercel Services / Container
- 不要把后端改成 Serverless Function；Java 用 container runtime

════════════════════════════════════
二、必须产出的文件
════════════════════════════════════

1) 根目录 vercel.json（Services + 双层 rewrite）
2) backend/Dockerfile.vercel（Vercel 专用多阶段镜像）
3) backend/docker-entrypoint.sh（socat 抢端口）
4) backend/Dockerfile（本地 Docker 可选，可不含 socat）
5) 前端同域 API：Axios baseURL='/api'；Vite 本地 proxy /api → localhost:8080
6) 后端：server.port=${PORT:8080}；DATABASE_URL / JWT 等从环境变量读取
7) README 补充部署步骤、环境变量、常见报错排查

════════════════════════════════════
三、vercel.json 规范（照此结构）
════════════════════════════════════

{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "services": {
    "frontend": {
      "root": "frontend/",
      "framework": "vite",
      "rewrites": [
        { "source": "/(.*)", "destination": "/index.html" }
      ]
    },
    "backend": {
      "runtime": "container",
      "root": "backend/",
      "entrypoint": "Dockerfile.vercel",
      "functions": {
        "Dockerfile.vercel": {
          "memory": 1024,
          "maxDuration": 60
        }
      }
    }
  },
  "rewrites": [
    {
      "source": "/api/(.*)",
      "destination": { "service": "backend" }
    },
    {
      "source": "/(.*)",
      "destination": { "service": "frontend" }
    }
  ]
}

关键点（缺一不可）：
A. 顶层 rewrites：只负责「选 service」——/api → backend，其余 → frontend
B. frontend 服务内部 rewrites：/(.*) → /index.html（SPA History 刷新防 404）
C. Vercel Services 规则：请求进入某个 service 后，由该 service 自己处理；匹配不到就 404，不会回退到其他顶层 rewrite
D. 因此「只写顶层 /api 与 catch-all」不够；必须在 frontend service 内加 SPA fallback
E. entrypoint 用 Dockerfile.vercel，不要写已废弃的 @vercel/docker

════════════════════════════════════
四、后端容器（Dockerfile.vercel + entrypoint）
════════════════════════════════════

问题：Spring Boot + JPA 冷启动常超过 Vercel 容器约 15s「必须监听 PORT」探测 → FUNCTION_INVOCATION_FAILED / could not connect to $PORT。

解决：socat 立刻监听对外 $PORT，Spring 绑内部端口，由 socat 转发。

Dockerfile.vercel 要求：
- 多阶段：maven:3.9.9-eclipse-temurin-8 AS build → eclipse-temurin:8-jre-jammy
- 运行镜像安装 socat
- ENV PORT=80，INTERNAL_PORT=8080
- CMD 走 docker-entrypoint.sh
- 禁止 alpine 导致 java not in PATH；用 jammy + JAVA_HOME 绝对路径

docker-entrypoint.sh 要求：
#!/usr/bin/env bash
set -euo pipefail
PORT="${PORT:-80}"
INTERNAL_PORT="${INTERNAL_PORT:-8080}"
JAVA_BIN="${JAVA_HOME:-/opt/java/openjdk}/bin/java"
# 立刻监听对外 PORT，内部未就绪则 retry
socat TCP-LISTEN:"${PORT}",fork,reuseaddr,bind=0.0.0.0 \
  TCP:127.0.0.1:"${INTERNAL_PORT}",retry=120,interval=0.25 &
trap 'kill $! 2>/dev/null || true' EXIT
exec "${JAVA_BIN}" \
  -XX:TieredStopAtLevel=1 -Xms128m -Xmx512m \
  -jar /app/app.jar \
  --server.port="${INTERNAL_PORT}" \
  --server.address=127.0.0.1

冷启动优化（application.properties）：
- server.port=${PORT:8080}（本地默认；容器内由 entrypoint 覆盖为 INTERNAL_PORT）
- spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false
- spring.jmx.enabled=false

数据库 URL：
- 支持 DATABASE_URL / SPRING_DATASOURCE_URL
- 支持 Neon 的 postgresql://user:pass@host/db（启动时拆成 JDBC + username/password）
- 禁止错误写法：jdbc:postgresql://user:pass@host（PG JDBC 不兼容）
- 推荐：jdbc:postgresql://HOST/db?sslmode=require + SPRING_DATASOURCE_USERNAME/PASSWORD
  或直接 postgresql://user:pass@HOST/db?sslmode=require

════════════════════════════════════
五、前端适配（同域 + SPA）
════════════════════════════════════

1. Axios：baseURL 必须是相对路径 '/api'（不要写死后端域名）
2. vite.config.ts 开发代理：
   server.proxy['/api'] = { target: 'http://localhost:8080', changeOrigin: true }
3. Vue Router 使用 createWebHistory（生产靠 frontend service 的 index.html rewrite）
4. package.json build 脚本：vite build，输出 dist（Vite framework 会处理）
5. 本地与生产请求路径保持一致，避免 VITE_API_URL 分叉（除非刻意双域部署）

════════════════════════════════════
六、环境变量（Vercel Project Settings）
════════════════════════════════════

必填：
- DATABASE_URL
- JWT_SECRET

可选：
- SPRING_DATASOURCE_USERNAME / SPRING_DATASOURCE_PASSWORD（URL 不含账号时）
- DEEPSEEK_API_KEY / DEEPSEEK_BASE_URL / DEEPSEEK_MODEL / DEEPSEEK_TIMEOUT_MS

CLI：
vercel link
vercel env add DATABASE_URL
vercel env add JWT_SECRET
vercel env add DEEPSEEK_API_KEY

════════════════════════════════════
七、关键命令清单
════════════════════════════════════

# —— 本地前端 ——
cd frontend && npm install && npm run dev

# —— 本地后端 ——
cd backend && mvn spring-boot:run
# 或
cd backend && mvn clean package -DskipTests && java -jar target/*.jar

# —— 本地 Docker（可选，用 Dockerfile 而非 Dockerfile.vercel）——
cd backend
docker build -t <app>-backend .
docker run --rm -p 8080:8080 \
  -e DATABASE_URL="jdbc:postgresql://HOST/db?sslmode=require" \
  -e JWT_SECRET="your-secret" \
  <app>-backend

# —— 本地验证 Vercel 镜像逻辑（可选）——
cd backend
docker build -f Dockerfile.vercel -t <app>-backend-vercel .
docker run --rm -p 8080:80 \
  -e DATABASE_URL="..." \
  -e JWT_SECRET="..." \
  <app>-backend-vercel

# —— 部署 ——
# 仓库根目录；Root Directory 不要设成 frontend/
vercel          # Preview
vercel --prod   # Production
# 或 push 到已连接 Git 分支自动部署

# —— 部署后验证 ——
# 前端：https://<project>.vercel.app/
# 健康：https://<project>.vercel.app/api/health
# SPA：打开 /login 或业务子路径后「浏览器刷新」必须仍 200，不能 Vercel 404

════════════════════════════════════
八、验收清单（必须全部通过）
════════════════════════════════════

[ ] vercel.json 使用 services，不用 @vercel/docker
[ ] /api/* → backend；其余 → frontend
[ ] frontend 内有 /(.*) → /index.html（刷新子路由不 404）
[ ] backend entrypoint = Dockerfile.vercel，含 socat 抢端口
[ ] 前端 Axios baseURL='/api'；本地 Vite proxy 到 8080
[ ] 后端从环境变量读 DATABASE_URL、JWT_SECRET；PORT 可注入
[ ] 部署后 /api/health 返回 UP
[ ] 登录链路可用；刷新 /tickets、/agent 等不出现平台 404
[ ] README 写清环境变量、部署步骤、常见错误

════════════════════════════════════
九、常见故障与处理（文档必须收录）
════════════════════════════════════

| 现象 | 原因/处理 |
|------|-----------|
| 点击进页正常，刷新 404 | frontend service 缺 SPA rewrite → 加 /(.*)→/index.html |
| @vercel/docker not published | 改用 Services + runtime:container |
| sh: java: not found | 用 eclipse-temurin:8-jre-jammy + JAVA_HOME 绝对路径 |
| FUNCTION_INVOCATION_FAILED / POST /index | 冷启动超时或 DB/环境变量错误；查 [entrypoint]/Hikari 日志 |
| could not connect to $PORT (15s) | 必须用 socat 提前 listen；确认镜像含 socat |
| Services/container 不可用 | 套餐不支持；或后端改 Railway/Fly，前端仍 Vercel + /api rewrite |
| 后端连库失败 | 勿用 jdbc:postgresql://user:pass@host；改用规范 JDBC 或 postgresql:// 自动拆分 |

════════════════════════════════════
十、约束
════════════════════════════════════

- 不要提交 .env / 密钥
- 不要 force push
- 修改接口字段时同步更新 docs/api-contract.md
- 统一 API 响应：{ success, data, message?, code? }
- 完成回报：改了哪些文件、如何验证 SPA 刷新与 /api/health
```

---

## 使用方式

1. 新开 Agent 会话，粘贴上方「Prompt 正文」。
2. 若项目目录名不是 `frontend/` / `backend/`，在粘贴后追加一句目录映射说明。
3. 部署前在 Vercel 配好 `DATABASE_URL`、`JWT_SECRET`，再 `vercel --prod` 或 push。
4. 必测：子路由刷新 + `/api/health`。
