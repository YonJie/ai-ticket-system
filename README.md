# AI 智能客服工单系统

Vue3 + Vite 前端、Spring Boot 2.x 后端、Neon Postgres 数据库，部署于 Vercel。

## 项目结构

```text
ai-ticket-system/
├── frontend/          # Vue3 + TypeScript + Vite
├── backend/           # Spring Boot 2.7 + Java 8
├── vercel.json        # Vercel Docker / 路由配置
└── README.md
```

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue3、Vite、TypeScript、Vue Router、Pinia、Axios、Element Plus |
| 后端 | Spring Boot 2.7、Java 8、Spring Web、Spring Data JPA、Spring Security、JJWT |
| 数据库 | Neon Postgres（本地可用 `local` profile + H2 快速验证） |
| 部署 | Vercel（前端静态 + 后端 Docker） |

## 本地开发

### 前端

```bash
cd frontend
npm install
npm run dev
```

访问：http://localhost:5173

### 后端

本地快速启动（内存 H2，无需 Neon）：

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

连接 Neon Postgres（默认 profile）：

```bash
cd backend
# 设置 SPRING_DATASOURCE_URL / USERNAME / PASSWORD 后：
mvn spring-boot:run
```

健康检查：http://localhost:8080/api/health

### 环境变量

复制 `.env.example` 为 `.env`，按 JDBC 形式配置 Neon 连接。

## 部署说明

`vercel.json` 已配置：

- `/api/*` → 后端 Docker 服务
- 其余请求 → 前端静态资源
