/**
 * 生成《系统详细设计说明书》（As-Built）docx。
 * 内容依据当前代码归纳，不引用 docs 内既有需求/设计文档。
 */
const {
  Document,
  Packer,
  Paragraph,
  TextRun,
  Table,
  TableRow,
  TableCell,
  HeadingLevel,
  BorderStyle,
  WidthType,
  ShadingType,
  LevelFormat,
  AlignmentType,
  VerticalAlign,
  PageNumber,
  Header,
  Footer,
  PageBreak,
} = require('docx');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '系统详细设计说明书.docx');
const PAGE_WIDTH = 9360;

const thinBorder = { style: BorderStyle.SINGLE, size: 4, color: '999999' };
const borders = { top: thinBorder, bottom: thinBorder, left: thinBorder, right: thinBorder };
const headerShading = { type: ShadingType.CLEAR, fill: '1F4E79' };
const altRowShading = { type: ShadingType.CLEAR, fill: 'F2F2F2' };
const codeShading = { type: ShadingType.CLEAR, fill: 'F5F5F5' };

/**
 * @param {string} text
 * @param {object} [opts]
 * @returns {TableCell}
 */
function cell(text, opts = {}) {
  const {
    bold = false,
    header = false,
    width,
    align = AlignmentType.LEFT,
    shade,
    fontSize = 18,
  } = opts;
  return new TableCell({
    borders,
    width: { size: width, type: WidthType.DXA },
    shading: shade || (header ? headerShading : undefined),
    verticalAlign: VerticalAlign.CENTER,
    children: [
      new Paragraph({
        alignment: align,
        spacing: { before: 40, after: 40 },
        children: [
          new TextRun({
            text: String(text ?? ''),
            bold: bold || header,
            color: header ? 'FFFFFF' : '222222',
            size: fontSize,
            font: 'Microsoft YaHei',
          }),
        ],
      }),
    ],
  });
}

/**
 * @param {string[]} headers
 * @param {string[][]} rows
 * @param {number[]} colWidths
 * @returns {Table}
 */
function makeTable(headers, rows, colWidths) {
  const sum = colWidths.reduce((a, b) => a + b, 0);
  const headerRow = new TableRow({
    children: headers.map((h, i) =>
      cell(h, { header: true, width: colWidths[i], align: AlignmentType.CENTER }),
    ),
  });
  const dataRows = rows.map(
    (row, ri) =>
      new TableRow({
        children: row.map((text, i) =>
          cell(text, {
            width: colWidths[i],
            shade: ri % 2 === 1 ? altRowShading : undefined,
            fontSize: 17,
          }),
        ),
      }),
  );
  return new Table({
    width: { size: sum, type: WidthType.DXA },
    columnWidths: colWidths,
    rows: [headerRow, ...dataRows],
  });
}

/**
 * @param {string} text
 * @param {object} [opts]
 * @returns {Paragraph}
 */
function p(text, opts = {}) {
  return new Paragraph({
    spacing: { before: opts.before ?? 80, after: opts.after ?? 80 },
    alignment: opts.align,
    children: [
      new TextRun({
        text,
        bold: opts.bold,
        italics: opts.italics,
        size: opts.size ?? 21,
        font: 'Microsoft YaHei',
        color: opts.color || '222222',
      }),
    ],
  });
}

/**
 * @param {string} text
 * @param {typeof HeadingLevel[keyof typeof HeadingLevel]} level
 * @returns {Paragraph}
 */
function h(text, level) {
  return new Paragraph({
    heading: level,
    spacing: { before: 280, after: 140 },
    children: [
      new TextRun({
        text,
        bold: true,
        font: 'Microsoft YaHei',
        size: level === HeadingLevel.HEADING_1 ? 28 : level === HeadingLevel.HEADING_2 ? 24 : 22,
      }),
    ],
  });
}

/**
 * @param {string} text
 * @returns {Paragraph}
 */
function bullet(text) {
  return new Paragraph({
    numbering: { reference: 'bullets', level: 0 },
    spacing: { before: 40, after: 40 },
    children: [
      new TextRun({ text, size: 21, font: 'Microsoft YaHei', color: '222222' }),
    ],
  });
}

/**
 * @param {string} text
 * @returns {Paragraph}
 */
function monoBlock(text) {
  return new Paragraph({
    shading: codeShading,
    spacing: { before: 60, after: 60 },
    children: [
      new TextRun({
        text,
        font: 'Consolas',
        size: 16,
        color: '333333',
      }),
    ],
  });
}

/**
 * 架构示意（等宽文本）。
 * @returns {Paragraph[]}
 */
function archDiagram() {
  const lines = [
    '┌─────────────────────────────────────────────────────────────┐',
    '│                     浏览器 / SPA（Vue3）                      │',
    '│  Router 角色守卫 · Pinia · Axios(/api) · Element Plus         │',
    '└───────────────────────────┬─────────────────────────────────┘',
    '                            │ HTTPS',
    '                            ▼',
    '┌─────────────────────────────────────────────────────────────┐',
    '│              Vercel 路由（vercel.json rewrites）              │',
    '│   /api/* → backend(container)    其余 → frontend(Vite)       │',
    '└───────────────┬─────────────────────────────┬───────────────┘',
    '                │                             │',
    '                ▼                             ▼',
    '┌───────────────────────────┐   ┌─────────────────────────────┐',
    '│ Spring Boot 2.7 API       │   │ 静态资源 frontend/dist       │',
    '│ JwtInterceptor · Services │   └─────────────────────────────┘',
    '│ JPA · AI Assist           │',
    '└─────────────┬─────────────┘',
    '              │ JDBC',
    '              ▼',
    '┌───────────────────────────┐     ┌───────────────────────────┐',
    '│ Neon Postgres（生产）      │     │ DeepSeek API（可选）       │',
    '│ H2 mem（local profile）    │     │ 失败 → 规则回退            │',
    '└───────────────────────────┘     └───────────────────────────┘',
  ];
  return lines.map((line) => monoBlock(line));
}

/**
 * ER 示意。
 * @returns {Paragraph[]}
 */
function erDiagram() {
  const lines = [
    'users (1) ────────< tickets.customer_id',
    'users (1) ────────< tickets.assigned_to   （可空，客服/管理员）',
    'tickets (1) ───────< messages.ticket_id',
    'users (1) ─────────< messages.sender_id',
    'tickets (1) ───────1 feedbacks.ticket_id  （唯一，一对一）',
  ];
  return lines.map((line) => monoBlock(line));
}

/**
 * 时序：建单 + AI。
 * @returns {Paragraph[]}
 */
function seqCreateTicket() {
  const lines = [
    'Customer → Frontend: 填写 title/description 提交',
    'Frontend → API: POST /api/tickets + Bearer JWT',
    'JwtInterceptor → TicketController: 注入 userId',
    'TicketService → AiAssistService.analyze(title, desc)',
    'AiAssistService → DeepSeekAiClient: 若已配置 Key',
    '  │ 成功 → category + suggestedReply',
    '  │ 失败/未配置 → RuleBasedAiAssistService（关键词分类 + 固定回复）',
    'TicketService → DB: 保存 Ticket(PENDING, category, ai_suggested_reply)',
    'API → Frontend: Result{ success, data: TicketResponse }',
  ];
  return lines.map((line) => monoBlock(line));
}

/**
 * 时序：登录。
 * @returns {Paragraph[]}
 */
function seqLogin() {
  const lines = [
    'User → Frontend: 用户名/密码',
    'Frontend → API: POST /api/auth/login（白名单，无 JWT）',
    'AuthService → DB: 查用户 + BCrypt 校验',
    'AuthService → JwtUtil: generateToken(userId)',
    'API → Frontend: { token, user }',
    'Frontend: localStorage 持久化 token + user；按角色跳转',
  ];
  return lines.map((line) => monoBlock(line));
}

/**
 * @returns {Promise<void>}
 */
async function main() {
  const children = [];

  // Cover
  children.push(
    new Paragraph({ spacing: { before: 1200 }, children: [] }),
    p('AI 智能客服工单系统', { size: 40, bold: true, align: AlignmentType.CENTER }),
    p('系统详细设计说明书（As-Built）', { size: 32, bold: true, align: AlignmentType.CENTER, before: 200 }),
    p('版本：1.0　|　状态：现状说明书　|　读者：研发交接', {
      size: 20,
      align: AlignmentType.CENTER,
      before: 400,
      color: '555555',
    }),
    p('依据：仓库当前实现（Vue3 + Spring Boot 2.7 + Neon Postgres）', {
      size: 18,
      align: AlignmentType.CENTER,
      color: '666666',
    }),
    p('生成说明：本文档独立归纳自代码与运行配置，不依赖 docs 内既有需求/设计类文件。', {
      size: 18,
      align: AlignmentType.CENTER,
      color: '666666',
    }),
    new Paragraph({ children: [new PageBreak()] }),
  );

  // 1
  children.push(
    h('1. 文档说明', HeadingLevel.HEADING_1),
    h('1.1 定位', HeadingLevel.HEADING_2),
    p('本文档为 As-Built（现状）详细设计说明书：描述系统「当前已经如何工作」，而非下一阶段目标态方案。字段、接口、状态机与部署约定均以代码为准。'),
    h('1.2 读者与用途', HeadingLevel.HEADING_2),
    bullet('主要读者：前后端 / 全栈研发交接与维护。'),
    bullet('用途：理解模块边界、数据模型、关键流程、鉴权与 AI 回退、本地/生产启动依赖。'),
    h('1.3 约定与不覆盖范围', HeadingLevel.HEADING_2),
    bullet('API 统一响应：{ success, data, message?, code? }；库表 snake_case，Java/前端 camelCase。'),
    bullet('核心域（用户/工单/消息/反馈/AI）写契约级；health、stats 等写摘要。'),
    bullet('不包含产品路线图、未实现功能的详细方案、UI 视觉规范详解。'),
    bullet('已知缺口与技术债见第 12 章，正文流程只描述已实现行为。'),
  );

  // 2
  children.push(
    h('2. 系统概览', HeadingLevel.HEADING_1),
    h('2.1 系统目标（已实现）', HeadingLevel.HEADING_2),
    p('面向客户与客服的轻量工单系统：客户提交工单并留言/评价；客服/管理员查看与处理工单；创建工单时由 AI（或规则回退）自动分类并生成建议回复。'),
    h('2.2 角色', HeadingLevel.HEADING_2),
    makeTable(
      ['角色', '枚举值', '主要能力（现状）'],
      [
        ['客户', 'CUSTOMER', '注册登录、提交工单、查看自己的工单、留言、对 RESOLVED 工单评价'],
        ['客服', 'AGENT', '查看全部工单、筛选状态、改状态/指派、留言；可见 AI 建议回复'],
        ['管理员', 'ADMIN', '与客服共享后台路由；另可访问每日统计接口'],
      ],
      [1400, 1800, 6160],
    ),
    p('演示账号（启动时 DataInitializer 幂等创建，密码均为 123456）：customer / agent / admin。', {
      before: 120,
      italics: true,
      size: 19,
      color: '555555',
    }),
    h('2.3 技术栈', HeadingLevel.HEADING_2),
    makeTable(
      ['层级', '技术'],
      [
        ['前端', 'Vue3、Vite、TypeScript、Vue Router、Pinia、Axios、Element Plus'],
        ['后端', 'Spring Boot 2.7、Java 8、Spring Web、Spring Data JPA、Spring Security（密码编码）、JJWT'],
        ['数据库', 'Neon Postgres（默认）；local profile 使用 H2 内存库'],
        ['AI', 'DeepSeek Chat Completions（可选）；RuleBasedAiAssistService 回退'],
        ['部署', 'Vercel Services：frontend(Vite) + backend(container/Dockerfile.vercel)'],
      ],
      [1600, 7760],
    ),
  );

  // 3
  children.push(
    h('3. 总体架构', HeadingLevel.HEADING_1),
    h('3.1 逻辑与部署拓扑', HeadingLevel.HEADING_2),
    p('浏览器加载 SPA；同源 /api 由 Vercel rewrite 到后端容器；后端经 JDBC 访问 Neon，可选调用 DeepSeek。'),
    ...archDiagram(),
    h('3.2 后端包结构（com.aiticket）', HeadingLevel.HEADING_2),
    makeTable(
      ['包/目录', '职责'],
      [
        ['controller', 'HTTP 入口：Auth / Ticket / TicketInteraction / Stats / Health'],
        ['service', '业务编排：Auth、Ticket、Message、Feedback、AiAssist、Stats'],
        ['entity / repository', 'JPA 实体与数据访问'],
        ['dto / common', '请求响应对象与统一 Result'],
        ['interceptor / util', 'JWT 拦截器、JwtUtil、AuthRequestUtils'],
        ['config', 'Web MVC、Security、DataInitializer、DotEnv 注入'],
        ['exception', 'BusinessException + GlobalExceptionHandler'],
        ['enums', 'UserRole、TicketStatus'],
      ],
      [2800, 6560],
    ),
    h('3.3 前端结构要点', HeadingLevel.HEADING_2),
    bullet('views/：登录注册、客户工单列表/新建/详情、客服看板与详情。'),
    bullet('stores/：user（会话）、ticket、app。'),
    bullet('utils/request.ts：Axios 封装，自动 Authorization，统一 ElMessage，401 清会话并跳转登录。'),
    bullet('router：requiresAuth + roles 元信息守卫；AGENT 与 ADMIN 共享 /agent 路由。'),
  );

  // 4
  children.push(
    h('4. 领域模型与 ER', HeadingLevel.HEADING_1),
    h('4.1 实体关系', HeadingLevel.HEADING_2),
    ...erDiagram(),
    h('4.2 表：users', HeadingLevel.HEADING_2),
    makeTable(
      ['字段', '类型/约束', '说明'],
      [
        ['id', 'UUID PK', 'uuid2 生成'],
        ['username', 'varchar(64) UNIQUE NOT NULL', '登录名'],
        ['password_hash', 'varchar NOT NULL', 'BCrypt'],
        ['role', 'varchar(32) NOT NULL', 'CUSTOMER / AGENT / ADMIN'],
        ['avatar_url', 'varchar 可空', '预留'],
        ['created_at', 'timestamp NOT NULL', 'PrePersist 填充'],
      ],
      [2200, 3200, 3960],
    ),
    h('4.3 表：tickets', HeadingLevel.HEADING_2),
    makeTable(
      ['字段', '类型/约束', '说明'],
      [
        ['id', 'UUID PK', 'uuid2'],
        ['customer_id', 'FK → users NOT NULL', '提交客户'],
        ['title', 'varchar(200) NOT NULL', '标题'],
        ['description', 'TEXT NOT NULL', '描述'],
        ['category', 'varchar(64)', 'AI/规则分类结果'],
        ['status', 'varchar(32) NOT NULL', '见状态枚举'],
        ['assigned_to', 'FK → users 可空', '指派客服/管理员'],
        ['ai_suggested_reply', 'TEXT', 'AI/规则建议回复'],
        ['created_at / updated_at', 'timestamp NOT NULL', '创建与更新时间'],
        ['version', 'bigint @Version', 'JPA 版本字段；业务乐观锁另用 updatedAt 比较'],
      ],
      [2600, 3000, 3760],
    ),
    h('4.4 表：messages', HeadingLevel.HEADING_2),
    makeTable(
      ['字段', '类型/约束', '说明'],
      [
        ['id', 'UUID PK', 'uuid2'],
        ['ticket_id', 'FK → tickets NOT NULL', '所属工单'],
        ['sender_id', 'FK → users NOT NULL', '发送者'],
        ['content', 'TEXT NOT NULL', '留言内容'],
        ['created_at', 'timestamp NOT NULL', '创建时间'],
      ],
      [2200, 3200, 3960],
    ),
    h('4.5 表：feedbacks', HeadingLevel.HEADING_2),
    makeTable(
      ['字段', '类型/约束', '说明'],
      [
        ['id', 'UUID PK', 'uuid2'],
        ['ticket_id', 'FK UNIQUE NOT NULL', '一单一评'],
        ['rating', 'int NOT NULL', '1–5'],
        ['comment', 'TEXT 可空', '评价文字'],
        ['created_at', 'timestamp NOT NULL', '创建时间'],
      ],
      [2200, 3200, 3960],
    ),
    h('4.6 工单状态机', HeadingLevel.HEADING_2),
    makeTable(
      ['状态', '含义', '关键流转（已实现）'],
      [
        ['PENDING', '待处理', '创建默认；客户在 RESOLVED/CLOSED 留言后回退到此状态'],
        ['PROCESSING', '处理中', '客服 PATCH status 设置'],
        ['RESOLVED', '已解决', '客服设置；客户可提交评价'],
        ['CLOSED', '已关闭', '客服设置；客户留言会回退 PENDING'],
      ],
      [1800, 1600, 5960],
    ),
    p('状态字符串在 API 中大小写不敏感解析后转为枚举名（UPPERCASE）。无强制「仅允许正向流转」的状态机校验——客服可直接设为任一合法枚举值。', {
      before: 100,
      size: 19,
      color: '555555',
    }),
  );

  // 5
  children.push(
    h('5. 核心业务流程与时序', HeadingLevel.HEADING_1),
    h('5.1 注册与登录', HeadingLevel.HEADING_2),
    bullet('注册：POST /api/auth/register；role 可选，默认 CUSTOMER；支持注册为 AGENT/ADMIN（开放注册，见局限章）。'),
    bullet('登录：校验用户名密码后签发 JWT（默认 24h，JWT_EXPIRATION_MS）。'),
    bullet('当前用户：GET /api/auth/me 需 JWT。'),
    ...seqLogin(),
    h('5.2 客户创建工单 + AI', HeadingLevel.HEADING_2),
    bullet('仅 CUSTOMER 可创建；title/description trim 后非空。'),
    bullet('同步调用 AiAssistService，将 category 与 aiSuggestedReply 落库，状态 PENDING。'),
    ...seqCreateTicket(),
    h('5.3 列表与详情', HeadingLevel.HEADING_2),
    bullet('客户：仅本人工单；客服/管理员：全部，可按 status 筛选，page/size 分页（size 上限 100）。'),
    bullet('详情含 messages（按时间升序）与可选 feedback。'),
    bullet('访问控制：客户只能看自己的；员工可看全部。'),
    h('5.4 更新工单（PATCH）', HeadingLevel.HEADING_2),
    bullet('请求必须带 updatedAt；与库中值毫秒对齐比较，不一致返回 409。'),
    bullet('员工：可改 status、assignedTo（指派对象须为 AGENT/ADMIN）；忽略 extraMessage。'),
    bullet('客户：通过 extraMessage 追加留言；若当前为 RESOLVED/CLOSED 则状态改回 PENDING；不可改 status/assignedTo。'),
    p('说明：客户追加留言亦存在独立接口 POST /api/tickets/{id}/messages；PATCH 路径保留兼容。', {
      size: 19,
      color: '555555',
    }),
    h('5.5 留言与评价', HeadingLevel.HEADING_2),
    bullet('留言：客户或员工；客户在终态留言会回退 PENDING；员工留言不改状态。'),
    bullet('评价：仅客户、仅本人工单、仅 RESOLVED、每单一次（唯一约束 + 业务校验）。'),
  );

  // 6
  children.push(
    h('6. 后端设计', HeadingLevel.HEADING_1),
    h('6.1 鉴权模型', HeadingLevel.HEADING_2),
    bullet('Spring Security：csrf 关闭、无状态会话、authorizeRequests.anyRequest().permitAll()——HTTP 层不拦请求。'),
    bullet('实际鉴权：JwtInterceptor 拦截 /api/**，白名单：/api/health、/api/auth/register、/api/auth/login。'),
    bullet('校验 Authorization: Bearer <token>，解析 userId 写入 request attribute；失败写 401 JSON。'),
    bullet('角色授权在 Service 层按 UserRole 判断（非方法级注解）。'),
    h('6.2 统一响应与异常', HeadingLevel.HEADING_2),
    monoBlock('{ "success": true|false, "data": ..., "message": "...", "code": 200|4xx|5xx }'),
    bullet('BusinessException(code) → GlobalExceptionHandler 映射 HTTP 状态（400/401/403/404/409/5xx）。'),
    bullet('Bean Validation 失败 → 400 + 字段错误拼接消息。'),
    h('6.3 关键服务职责', HeadingLevel.HEADING_2),
    makeTable(
      ['服务', '职责'],
      [
        ['AuthService', '注册、登录签发 JWT、查询当前用户'],
        ['TicketService', '创建（含 AI）、列表、详情、PATCH 更新与乐观锁'],
        ['TicketMessageService', '独立留言接口与状态回退'],
        ['TicketFeedbackService', '评价约束与落库'],
        ['AiAssistService', 'DeepSeek 优先 + 规则回退编排'],
        ['StatsService', '管理员每日工单统计'],
      ],
      [2800, 6560],
    ),
    h('6.4 并发与一致性', HeadingLevel.HEADING_2),
    bullet('工单更新使用请求体 updatedAt 与 DB 比较（毫秒截断），冲突 409。'),
    bullet('实体含 @Version，但业务路径以 updatedAt 乐观锁为主。'),
    bullet('评价依赖 ticket_id 唯一约束兜底并发双提交。'),
  );

  // 7
  children.push(
    h('7. 前端设计', HeadingLevel.HEADING_1),
    h('7.1 路由与守卫', HeadingLevel.HEADING_2),
    makeTable(
      ['路径', '角色', '页面职责'],
      [
        ['/login, /register', 'guest', '登录注册；已登录按角色重定向'],
        ['/tickets', 'CUSTOMER', '我的工单列表'],
        ['/tickets/new', 'CUSTOMER', '新建工单'],
        ['/tickets/:id', 'CUSTOMER', '工单详情、留言、评价'],
        ['/agent', 'AGENT/ADMIN', '客服看板（列表/筛选）'],
        ['/agent/tickets/:id', 'AGENT/ADMIN', '处理详情、改状态、指派、留言、看 AI 建议'],
      ],
      [2400, 1800, 5160],
    ),
    h('7.2 状态与请求', HeadingLevel.HEADING_2),
    bullet('useUserStore：token + userInfo 持久化 localStorage；isAgent = AGENT || ADMIN。'),
    bullet('request.ts：baseURL=/api，超时 15s；成功体 success===false 时 ElMessage 并 reject。'),
    bullet('开发态 Vite 代理 /api → localhost:8080；生产由 Vercel rewrite 到 backend service。'),
    h('7.3 页面交互要点（现状）', HeadingLevel.HEADING_2),
    bullet('新建工单成功后进入详情，展示系统生成的分类与建议回复（只读字段）。'),
    bullet('客服详情可基于 aiSuggestedReply 辅助回复（人工发送留言）。'),
    bullet('更新工单时携带详情中的 updatedAt，冲突提示刷新。'),
  );

  // 8
  children.push(
    h('8. API 契约（折中）', HeadingLevel.HEADING_1),
    h('8.1 通用约定', HeadingLevel.HEADING_2),
    bullet('Base path：/api'),
    bullet('鉴权头：Authorization: Bearer <jwt>（白名单除外）'),
    bullet('时间：LocalDateTime JSON（通常 ISO-8601）'),
    bullet('分页：page 从 0；响应 PageResult{ content, totalElements, page, size, totalPages }'),
    h('8.2 认证（契约级）', HeadingLevel.HEADING_2),
    makeTable(
      ['方法/路径', '鉴权', '请求要点', '响应 data'],
      [
        ['POST /auth/register', '否', 'username(2-64), password(6-64), role?', 'UserResponse'],
        ['POST /auth/login', '否', 'username, password', '{ token, user }'],
        ['GET /auth/me', '是', '—', 'UserResponse'],
      ],
      [2400, 800, 3000, 3160],
    ),
    p('UserResponse：id, username, role, avatarUrl?, createdAt。密码永不返回。', { size: 19 }),
    h('8.3 工单（契约级）', HeadingLevel.HEADING_2),
    makeTable(
      ['方法/路径', '鉴权/角色', '请求要点', '响应 data'],
      [
        ['POST /tickets', 'JWT / CUSTOMER', 'title, description', 'TicketResponse'],
        ['GET /tickets', 'JWT', 'status?, page, size', 'PageResult<TicketResponse>'],
        ['GET /tickets/{id}', 'JWT / 可见性校验', '—', 'TicketDetailResponse'],
        ['PATCH /tickets/{id}', 'JWT', 'updatedAt 必填；员工 status/assignedTo；客户 extraMessage', 'TicketResponse'],
        ['POST /tickets/{id}/messages', 'JWT', 'content', 'MessageResponse'],
        ['POST /tickets/{id}/feedback', 'JWT / CUSTOMER', 'rating(1-5), comment?', 'FeedbackResponse'],
      ],
      [2800, 2000, 2800, 1760],
    ),
    p('TicketResponse 字段：id, customerId, customerUsername, title, description, category, status, assignedTo, aiSuggestedReply, createdAt, updatedAt。'),
    p('TicketDetailResponse：继承上表 + messages[] + feedback?。'),
    p('MessageResponse：id, ticketId, userId, username, content, createdAt。'),
    p('FeedbackResponse：id, ticketId, rating, comment, createdAt。'),
    h('8.4 边缘接口（摘要）', HeadingLevel.HEADING_2),
    makeTable(
      ['方法/路径', '说明'],
      [
        ['GET /health', '健康检查；拦截器白名单；响应形态可能非统一 Result 包装'],
        ['GET /stats/daily', '需 JWT；StatsService 校验管理员；返回今日新增与各状态数量'],
      ],
      [2800, 6560],
    ),
    h('8.5 常见业务错误码', HeadingLevel.HEADING_2),
    makeTable(
      ['code', '典型场景'],
      [
        ['400', '校验失败、空字段、非法状态/角色、重复评价、非 RESOLVED 评价'],
        ['401', '未登录、密码错误、令牌无效'],
        ['403', '非客户建单/评价、客户改状态、越权访问工单'],
        ['404', '用户/工单/指派对象不存在'],
        ['409', '工单 updatedAt 冲突'],
      ],
      [1200, 8160],
    ),
  );

  // 9
  children.push(
    h('9. AI 辅助设计', HeadingLevel.HEADING_1),
    h('9.1 触发时机', HeadingLevel.HEADING_2),
    p('仅在客户创建工单（TicketService.createTicket）时同步调用 AiAssistService.analyze(title, description)。留言、改状态等路径不再调用 AI。'),
    h('9.2 编排与回退', HeadingLevel.HEADING_2),
    bullet('未配置 DEEPSEEK_API_KEY → 直接规则回退。'),
    bullet('已配置 → DeepSeekAiClient 调用；异常或空字段 → 用规则结果补齐。'),
    bullet('最终保证 category 与 suggestedReply 均非空后写入 Ticket。'),
    h('9.3 DeepSeek 客户端要点', HeadingLevel.HEADING_2),
    bullet('OpenAI 兼容 Chat Completions；默认模型 deepseek-v4-flash；默认超时 8s。'),
    bullet('System Prompt 要求只输出 JSON：category ∈ {退货,物流,账户,其他}，suggestedReply 中文简洁 ≤120 字。'),
    bullet('允许类别集合在客户端校验；非法类别回落到规则分类。'),
    h('9.4 规则回退', HeadingLevel.HEADING_2),
    makeTable(
      ['关键词（标题+描述）', 'category'],
      [
        ['退货 / 退款', '退货'],
        ['物流 / 快递', '物流'],
        ['账户 / 登录', '账户'],
        ['其他', '其他'],
      ],
      [4680, 4680],
    ),
    p('固定建议回复：感谢您的反馈，我们已收到工单，预计 24 小时内处理。'),
    h('9.5 落库与展示', HeadingLevel.HEADING_2),
    bullet('tickets.category、tickets.ai_suggested_reply。'),
    bullet('客服端详情展示建议回复，供人工参考后通过留言发送；非自动回复客户。'),
  );

  // 10
  children.push(
    h('10. 部署与配置清单', HeadingLevel.HEADING_1),
    h('10.1 环境变量', HeadingLevel.HEADING_2),
    makeTable(
      ['变量', '必需性', '说明'],
      [
        ['DATABASE_URL 或 SPRING_DATASOURCE_URL', '生产必需', 'Neon；支持 postgresql:// 启动时拆 JDBC；勿用 jdbc:postgresql://user:pass@ 形式'],
        ['JWT_SECRET', '强烈建议', '默认有开发占位，生产必须替换'],
        ['JWT_EXPIRATION_MS', '可选', '默认 86400000（24h）'],
        ['DEEPSEEK_API_KEY', '可选', '缺失则规则回退'],
        ['DEEPSEEK_BASE_URL / MODEL / TIMEOUT_MS', '可选', '默认官方 base、deepseek-v4-flash、8000ms'],
        ['PORT', '平台注入', '容器端口；本地默认 8080'],
        ['JPA_DDL_AUTO', '可选', '默认 update'],
      ],
      [3600, 1400, 4360],
    ),
    h('10.2 本地启动', HeadingLevel.HEADING_2),
    bullet('前端：cd frontend && npm install && npm run dev → http://localhost:5173'),
    bullet('后端快速：cd backend && mvn spring-boot:run -Dspring-boot.run.profiles=local（H2，无需 Neon）'),
    bullet('后端连 Neon：设置 DATABASE_URL、JWT_SECRET 后 mvn spring-boot:run'),
    bullet('健康检查：GET http://localhost:8080/api/health'),
    h('10.3 生产（Vercel）', HeadingLevel.HEADING_2),
    bullet('services.frontend：root=frontend/，SPA rewrite → index.html'),
    bullet('services.backend：container，entrypoint=Dockerfile.vercel，memory 1024，maxDuration 60'),
    bullet('顶层 rewrites：/api/* → backend；其余 → frontend'),
    h('10.4 故障速查', HeadingLevel.HEADING_2),
    makeTable(
      ['现象', '排查'],
      [
        ['启动后无法连库', '检查 DATABASE_URL 形态与 Neon 网络；确认非错误 JDBC 嵌入账号格式'],
        ['接口全 401', '前端是否带 Bearer；token 是否过期；JWT_SECRET 是否与签发时一致'],
        ['AI 建议总是固定文案', 'DEEPSEEK_API_KEY 未配置或调用失败，属预期回退'],
        ['409 冲突', '多人同时改同一工单，刷新详情后重试'],
      ],
      [2800, 6560],
    ),
  );

  // 11
  children.push(
    h('11. 测试与质量简表', HeadingLevel.HEADING_1),
    h('11.1 如何运行', HeadingLevel.HEADING_2),
    bullet('后端：cd backend && mvn test'),
    bullet('前端：cd frontend && npm test（或项目配置的 vitest 脚本）'),
    h('11.2 已有覆盖（现状）', HeadingLevel.HEADING_2),
    makeTable(
      ['层级', '代表用例', '意图'],
      [
        ['后端 Controller', 'AuthControllerTest, TicketControllerTest, TicketInteractionControllerTest', '注册登录、工单 CRUD/留言评价主路径'],
        ['后端 Service', 'AiAssistServiceTest, DeepSeekAiClientTest, RuleBasedAiAssistServiceTest', 'AI 编排与规则分类'],
        ['后端 Config', 'DotEnvEnvironmentPostProcessorTest', '环境变量注入'],
        ['前端', 'LoginView.spec.ts, CustomerTicketsView.spec.ts, datetime.spec.ts', '关键页面与工具函数'],
      ],
      [1800, 4200, 3360],
    ),
    p('改动鉴权、工单状态、评价约束或 AI 回退时，优先跑上述相关测试作为回归保护。', { size: 19 }),
  );

  // 12
  children.push(
    h('12. 已知局限 / 非目标 / 技术债', HeadingLevel.HEADING_1),
    h('12.1 已知局限（代码可证实）', HeadingLevel.HEADING_2),
    bullet('注册接口可自选 AGENT/ADMIN，无邀请码或管理员审批。'),
    bullet('Spring Security 未做 URL 级鉴权，完全依赖拦截器 + Service 内角色判断。'),
    bullet('工单状态无严格迁移图校验，员工可直接设置为任一枚举值。'),
    bullet('AI 仅创建时调用一次，无会话式自动回复、无流式输出、无知识库检索。'),
    bullet('无附件上传、无实时推送（WebSocket）；列表需刷新拉取。'),
    bullet('ADMIN 与 AGENT 前端能力基本重合，统计接口为少数管理员专属能力。'),
    bullet('默认 JWT_SECRET 与演示密码 123456 仅适合开发演示。'),
    bullet('JPA ddl-auto 默认 update，生产环境需自行评估迁移策略。'),
    h('12.2 非目标（当前未作为系统能力交付）', HeadingLevel.HEADING_2),
    bullet('多租户 / 组织隔离、SLA、工单优先级队列、知识库、质检报表中台。'),
    bullet('OAuth/SSO、细粒度 RBAC 资源权限、操作审计完整链路。'),
    h('12.3 技术债与接手提示', HeadingLevel.HEADING_2),
    bullet('客户留言存在 PATCH.extraMessage 与 POST .../messages 双路径，修改行为需两边对齐。'),
    bullet('@Version 与 updatedAt 乐观锁并存，后续统一策略可降低心智负担。'),
    bullet('AI 超时会拉长创建工单接口耗时（默认可达数秒级）；需关注产品体验与超时配置。'),
    bullet('DotEnv / 环境变量解析顺序影响 DeepSeek Key 是否生效，改配置加载时需回归 Ai 相关测试。'),
  );

  // Appendix
  children.push(
    h('附录 A. 仓库目录速查', HeadingLevel.HEADING_1),
    monoBlock('ai-ticket-system/'),
    monoBlock('├── frontend/          Vue3 SPA'),
    monoBlock('├── backend/           Spring Boot API + Dockerfile(s)'),
    monoBlock('├── docs/              本文档等'),
    monoBlock('├── vercel.json        前后端 Services 与 /api rewrite'),
    monoBlock('└── README.md          本地启动入口'),
    h('附录 B. 修订记录', HeadingLevel.HEADING_1),
    makeTable(
      ['版本', '日期', '说明'],
      [['1.0', '2026-08-07', '首版 As-Built 详细设计说明书，自代码归纳']],
      [1600, 2000, 5760],
    ),
  );

  const doc = new Document({
    styles: {
      default: {
        document: {
          styles: [
            {
              id: 'Normal',
              run: { font: 'Microsoft YaHei', size: 21 },
            },
          ],
        },
      },
    },
    numbering: {
      config: [
        {
          reference: 'bullets',
          levels: [
            {
              level: 0,
              format: LevelFormat.BULLET,
              text: '•',
              alignment: AlignmentType.LEFT,
              style: { paragraph: { indent: { left: 720, hanging: 360 } } },
            },
          ],
        },
      ],
    },
    sections: [
      {
        properties: {
          page: {
            margin: { top: 1134, bottom: 1134, left: 1134, right: 1134 },
          },
        },
        headers: {
          default: new Header({
            children: [
              new Paragraph({
                alignment: AlignmentType.RIGHT,
                children: [
                  new TextRun({
                    text: 'AI 智能客服工单系统 · 系统详细设计说明书（As-Built）',
                    size: 16,
                    color: '888888',
                    font: 'Microsoft YaHei',
                  }),
                ],
              }),
            ],
          }),
        },
        footers: {
          default: new Footer({
            children: [
              new Paragraph({
                alignment: AlignmentType.CENTER,
                children: [
                  new TextRun({ text: '第 ', size: 16, font: 'Microsoft YaHei', color: '666666' }),
                  new TextRun({ children: [PageNumber.CURRENT], size: 16, color: '666666' }),
                  new TextRun({ text: ' 页', size: 16, font: 'Microsoft YaHei', color: '666666' }),
                ],
              }),
            ],
          }),
        },
        children,
      },
    ],
  });

  const buffer = await Packer.toBuffer(doc);
  fs.writeFileSync(OUT, buffer);
  console.log('Wrote', OUT);
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
