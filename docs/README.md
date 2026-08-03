# 文档目录（docs）

本目录存放 AI 智能客服工单系统的产品、设计、契约、测试与**多 Agent 协同提示词**文档。

| 文档 | 说明 |
|------|------|
| [PRD.md](./PRD.md) | 产品需求：角色、功能范围、业务规则与验收要点 |
| [design.md](./design.md) | 技术设计：架构、数据模型、鉴权、部署与目录约定 |
| [api-contract.md](./api-contract.md) | API 契约：请求/响应字段与错误码（前后端对齐唯一来源） |
| [TESTING.md](./TESTING.md) | 测试说明：如何运行、覆盖场景与已知风险 |
| [PROMPTS.md](./PROMPTS.md) | **多 Agent 协同开发提示词实录**（任务拆分、并行约束、整合方式） |

仓库根目录还提供：

- [../README.md](../README.md)：本地开发、环境变量与 Vercel 部署步骤
- [../.env.example](../.env.example)：环境变量示例

## 维护约定

1. **改接口字段** → 同步更新 `api-contract.md`（见项目 Cursor 约定）。
2. **改业务规则 / 角色权限** → 同步更新 `PRD.md` 与 `design.md`。
3. **新增/调整 Agent 开发任务** → 在 `PROMPTS.md` 追加提示词与产物对照。
4. **增补自动化测试** → 同步更新 `TESTING.md` 覆盖表。
