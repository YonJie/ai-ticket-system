/**
 * 将业务需求文档 Markdown 转为 Word (.docx)
 */
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  HeadingLevel, BorderStyle, WidthType, ShadingType, LevelFormat,
  AlignmentType, VerticalAlign } = require('docx');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '业务需求文档.docx');

/** 页面内容宽度（A4，左右各约 2.54cm 边距） */
const PAGE_WIDTH = 9360;

const thinBorder = { style: BorderStyle.SINGLE, size: 4, color: '999999' };
const borders = { top: thinBorder, bottom: thinBorder, left: thinBorder, right: thinBorder };
const headerShading = { type: ShadingType.CLEAR, fill: '1F4E79' };
const altRowShading = { type: ShadingType.CLEAR, fill: 'F2F2F2' };

/**
 * 创建表格单元格
 * @param {string} text
 * @param {object} opts
 * @returns {TableCell}
 */
function cell(text, opts = {}) {
  const { bold = false, header = false, width, align = AlignmentType.LEFT, shade } = opts;
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
            color: header ? 'FFFFFF' : '333333',
            size: header ? 18 : 18,
            font: '微软雅黑',
          }),
        ],
      }),
    ],
  });
}

/**
 * 创建表格
 * @param {string[]} headers
 * @param {string[][]} rows
 * @param {number[]} colWidths
 * @returns {Table}
 */
function makeTable(headers, rows, colWidths) {
  const sum = colWidths.reduce((a, b) => a + b, 0);
  const headerRow = new TableRow({
    tableHeader: true,
    children: headers.map((h, i) => cell(h, { header: true, width: colWidths[i], align: AlignmentType.CENTER })),
  });
  const dataRows = rows.map((row, ri) =>
    new TableRow({
      children: row.map((c, i) =>
        cell(c, {
          width: colWidths[i],
          shade: ri % 2 === 1 ? altRowShading : undefined,
        })
      ),
    })
  );
  return new Table({
    width: { size: sum, type: WidthType.DXA },
    columnWidths: colWidths,
    rows: [headerRow, ...dataRows],
  });
}

/**
 * 标题段落
 * @param {string} text
 * @param {string} level
 * @returns {Paragraph}
 */
function heading(text, level) {
  return new Paragraph({
    heading: level,
    spacing: { before: level === HeadingLevel.TITLE ? 0 : 280, after: 120 },
    children: [new TextRun({ text, font: '微软雅黑', bold: true })],
  });
}

/**
 * 正文段落（支持简单 **bold** 与 `code`）
 * @param {string} text
 * @param {object} [opts]
 * @returns {Paragraph}
 */
function para(text, opts = {}) {
  return new Paragraph({
    spacing: { before: 60, after: 60, line: 360 },
    ...opts,
    children: parseInline(text),
  });
}

/**
 * 解析行内加粗与代码
 * @param {string} text
 * @returns {TextRun[]}
 */
function parseInline(text) {
  if (!text) return [new TextRun({ text: '', font: '微软雅黑', size: 21 })];
  const parts = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let last = 0;
  let m;
  while ((m = re.exec(text)) !== null) {
    if (m.index > last) {
      parts.push(new TextRun({ text: text.slice(last, m.index), font: '微软雅黑', size: 21, color: '333333' }));
    }
    const token = m[0];
    if (token.startsWith('**')) {
      parts.push(new TextRun({ text: token.slice(2, -2), font: '微软雅黑', size: 21, bold: true, color: '333333' }));
    } else {
      parts.push(new TextRun({ text: token.slice(1, -1), font: 'Consolas', size: 18, color: 'C7254E' }));
    }
    last = m.index + token.length;
  }
  if (last < text.length) {
    parts.push(new TextRun({ text: text.slice(last), font: '微软雅黑', size: 21, color: '333333' }));
  }
  return parts.length ? parts : [new TextRun({ text, font: '微软雅黑', size: 21 })];
}

/**
 * 子弹列表项
 * @param {string} text
 * @param {string} ref
 * @returns {Paragraph}
 */
function bullet(text, ref = 'bullets') {
  return new Paragraph({
    numbering: { reference: ref, level: 0 },
    spacing: { before: 40, after: 40, line: 340 },
    children: parseInline(text),
  });
}

/**
 * 编号列表项
 * @param {string} text
 * @returns {Paragraph}
 */
function numbered(text) {
  return new Paragraph({
    numbering: { reference: 'numbers', level: 0 },
    spacing: { before: 40, after: 40, line: 340 },
    children: parseInline(text),
  });
}

/**
 * 分隔线
 * @returns {Paragraph}
 */
function hr() {
  return new Paragraph({
    border: { bottom: { style: BorderStyle.SINGLE, size: 6, color: '1F4E79', space: 8 } },
    spacing: { before: 120, after: 200 },
    children: [],
  });
}

/**
 * 小节小标题（非大纲）
 * @param {string} text
 * @returns {Paragraph}
 */
function subHead(text) {
  return new Paragraph({
    spacing: { before: 200, after: 80 },
    children: [new TextRun({ text, font: '微软雅黑', size: 22, bold: true, color: '1F4E79' })],
  });
}

const children = [
  heading('AI 智能客服工单系统 需求文档', HeadingLevel.TITLE),

  heading('版本记录', HeadingLevel.HEADING_1),
  makeTable(
    ['日期', '版本', '变更说明', '作者'],
    [['2026-08-06', '1.0', '按现状对齐重新生成业务需求文档（字段/界面/逻辑细化）', 'AI Coding Assistant']],
    [1600, 1000, 4760, 2000]
  ),

  heading('目录', HeadingLevel.HEADING_1),
  para('1. 概况'),
  para('2. 业务需求'),
  bullet('2.1 用户认证'),
  bullet('2.2 客户工单'),
  bullet('2.3 客服/管理工单台'),
  bullet('2.4 AI 辅助（分类与建议回复）'),
  bullet('2.5 管理员日统计'),
  hr(),

  // —— 1 概况 ——
  heading('1 概况', HeadingLevel.HEADING_1),
  heading('1.1 需求描述', HeadingLevel.HEADING_2),
  para('为客户提供在线提单、跟踪、留言与评价能力；为客服/管理员提供工单处理、状态变更、指派与留言能力。创建工单时由 DeepSeek（或规则回退）自动分类并生成 AI 建议回复，降低人工分流成本。'),
  para('**范围说明**：本文档描述**当前已实现**能力，不包含附件、通知推送、多轮 AI 对话/RAG、细粒度 RBAC 与审计日志等扩展项。'),
  para('**本期状态**：本期功能已实现并可演示/部署（Vue3 前端 + Spring Boot 后端 + Neon Postgres，部署于 Vercel）。'),

  heading('1.2 功能清单', HeadingLevel.HEADING_2),
  makeTable(
    ['序号', '功能模块', '功能说明'],
    [
      ['1', '用户认证', '注册、登录、当前用户信息；JWT 鉴权与按角色路由分流'],
      ['2', '客户工单', '新建工单、我的列表、详情、追加留言、已解决评价'],
      ['3', '客服/管理工单台', '全部工单列表与状态筛选、改状态、指派、留言、乐观锁冲突处理'],
      ['4', 'AI 辅助（分类与建议回复）', '创建工单时 DeepSeek 优先，失败/无 Key 时规则回退'],
      ['5', '管理员日统计', '今日新增与各状态计数（仅 ADMIN；接口已实现）'],
    ],
    [800, 2800, 5760]
  ),

  heading('1.3 应用集成', HeadingLevel.HEADING_2),
  makeTable(
    ['模块', '功能点', '外部应用', '交互说明'],
    [
      ['AI 辅助', '分类 + 建议回复', 'DeepSeek API', '创建工单时调用 deepseek-v4-flash（关 thinking）；无 Key/超时/非法输出则规则回退'],
      ['全部业务', '持久化', 'Neon Postgres', '用户、工单、留言、评价数据存储；连接串为 JDBC 形式 DATABASE_URL'],
      ['部署', '前后端同域', 'Vercel', '/api/* 路由到后端容器，其余为前端静态资源'],
    ],
    [1400, 1800, 1800, 4360]
  ),

  heading('1.4 需求计划', HeadingLevel.HEADING_2),
  para('本期功能已实现上线/可演示，本文档用于现状对齐与评审，不另列排期表。'),

  heading('1.5 用户角色（概况）', HeadingLevel.HEADING_2),
  makeTable(
    ['角色', '枚举值', '核心能力'],
    [
      ['客户', 'CUSTOMER', '注册/登录、提交工单、查看自己的工单、留言、对已解决工单评价'],
      ['客服', 'AGENT', '登录、查看全部工单、按状态筛选、改状态/指派、留言'],
      ['管理员', 'ADMIN', '客服全部能力 + 查看每日工单统计（接口）'],
    ],
    [1200, 1600, 6560]
  ),
  para('演示账号（启动时幂等初始化，密码均为 `123456`）：`customer` / `agent` / `admin`。'),
  hr(),

  // —— 2 业务需求 ——
  heading('2 业务需求', HeadingLevel.HEADING_1),

  // 2.1
  heading('2.1 用户认证', HeadingLevel.HEADING_2),
  heading('2.1.1 功能设计及说明', HeadingLevel.HEADING_3),
  makeTable(
    ['项目', '说明'],
    [
      ['角色', '游客（注册/登录）；登录后任意角色可获取当前用户'],
      ['功能简介', '提供注册、登录与会话维持（JWT），并按角色进入对应首页'],
      ['界面元素及控制说明', '见下方详细说明'],
    ],
    [2400, 6960]
  ),

  subHead('登录界面（/login）'),
  para('**表单区域：**'),
  makeTable(
    ['字段名', '控件类型', '必填', '长度限制', '联动规则', '默认值'],
    [
      ['用户名', '输入', '是', '—', '—', '演示预填 customer'],
      ['密码', '密码输入（可显示）', '是', '—', '—', '演示预填 123456'],
    ],
    [1200, 2000, 800, 1400, 1400, 2560]
  ),
  para('**按钮区域：**'),
  bullet('登录：校验通过后调用登录接口，成功写入 token/用户信息，按角色跳转（客服/管理员 → `/agent`，客户 → `/tickets`；若存在 `redirect` 查询参数则优先跳转）'),
  bullet('去注册：跳转 `/register`'),

  subHead('注册界面（/register）'),
  makeTable(
    ['字段名', '控件类型', '必填', '长度限制', '联动规则', '默认值'],
    [
      ['用户名', '输入', '是', '2–64 字符，唯一', '—', '空'],
      ['密码', '密码输入（可显示）', '是', '6–64 字符；服务端 BCrypt 存储', '—', '空'],
      ['角色', '下拉选择，选项「客户/客服」', '是', '对应 CUSTOMER/AGENT', '—', 'CUSTOMER'],
    ],
    [1200, 2400, 800, 2800, 1000, 1160]
  ),
  para('说明：接口侧角色可选且支持 `admin`（大小写不敏感），前端注册页当前仅开放客户/客服选项；管理员依赖演示账号或接口注册。'),
  para('**按钮区域：**'),
  bullet('注册：成功后自动登录并按角色跳转首页'),
  bullet('去登录：跳转 `/login`'),

  subHead('业务逻辑说明'),
  bullet('除白名单（`/api/health`、`/api/auth/register`、`/api/auth/login`）外，其余 `/api/**` 需 `Authorization: Bearer <token>`'),
  bullet('JWT 默认有效期 24 小时；无效/过期返回 401，前端清会话并引导重新登录'),
  bullet('未登录访问受保护路由 → `/login`；角色不匹配 → 跳转各自首页'),
  bullet('用户名已存在返回 400；用户名或密码错误返回 401'),
  hr(),

  // 2.2
  heading('2.2 客户工单', HeadingLevel.HEADING_2),
  heading('2.2.1 功能设计及说明', HeadingLevel.HEADING_3),
  makeTable(
    ['项目', '说明'],
    [
      ['角色', 'CUSTOMER'],
      ['功能简介', '客户创建、查看、跟踪本人工单，并支持留言与已解决评价'],
      ['界面元素及控制说明', '见下方详细说明'],
    ],
    [2400, 6960]
  ),

  subHead('列表界面（/tickets 我的工单）'),
  para('**查询区域：**'),
  bullet('本期前端列表无独立查询条件；后端分页参数 `page`（默认 0）、`size`（默认 20，最大 100）'),
  para('**列表区域：**'),
  bullet('展示：标题、分类、状态、创建时间、操作（查看）'),
  bullet('排序：创建时间倒序'),
  bullet('分页：接口支持分页；前端当前按默认页加载列表'),
  para('**按钮区域：**'),
  bullet('新建工单：进入 `/tickets/new`'),
  bullet('查看/标题链接：进入 `/tickets/:id`'),

  subHead('新建页面（/tickets/new）'),
  makeTable(
    ['字段名', '控件类型', '必填', '长度限制', '联动规则', '默认值'],
    [
      ['标题', '输入', '是', '前端限制 200 字，展示字数', '—', '空'],
      ['描述', '文本域', '是', '非空', '—', '空'],
    ],
    [1200, 1400, 800, 2800, 1400, 1760]
  ),
  para('**按钮区域：**'),
  bullet('返回：回到我的工单列表'),
  bullet('提交：创建成功后跳转详情页；创建过程触发 AI 分类与建议回复（见 2.4）'),
  bullet('未保存离开：标题或描述有内容时触发未保存离开守卫'),
  para('**系统自动生成字段（创建后）：**'),
  makeTable(
    ['字段名', '说明'],
    [
      ['分类 category', 'DeepSeek 或规则回退生成：退货 / 物流 / 账户 / 其他'],
      ['AI 建议回复 aiSuggestedReply', 'DeepSeek 或固定回退文案'],
      ['状态 status', '初始 PENDING'],
      ['客户信息', '取当前登录用户'],
    ],
    [3200, 6160]
  ),

  subHead('详情界面（/tickets/:id）'),
  para('**展示区域：**'),
  bullet('工单基本信息：标题、描述、分类、状态、指派情况、创建时间等'),
  bullet('AI 建议回复（只读展示，如有）'),
  bullet('留言时间线：发送者、角色标识（客户/客服）、时间、内容；按时间升序'),
  bullet('评价信息：若已评价则展示评分与备注'),
  para('**留言区域：**'),
  makeTable(
    ['字段名', '控件类型', '必填', '说明'],
    [['留言内容', '文本域', '是', '非空；客户仅可对本人工单留言']],
    [1600, 1600, 1000, 5160]
  ),
  para('**按钮区域：**'),
  bullet('发送留言：提交后刷新时间线；若工单为 `RESOLVED`/`CLOSED`，留言后状态改回 `PENDING`'),
  bullet('评价：仅当状态为 `RESOLVED` 且尚未评价时可见/可操作'),

  subHead('评价弹层/表单'),
  makeTable(
    ['字段名', '控件类型', '必填', '长度限制', '联动规则', '默认值'],
    [
      ['评分', '评分控件（1–5）', '是', '整数 1–5', '—', '5'],
      ['评论', '文本域', '否', '—', '—', '空'],
    ],
    [1200, 2000, 800, 1600, 1400, 2360]
  ),
  para('**按钮区域：**'),
  bullet('提交评价：每单仅一次；成功后展示评价结果，不可重复提交'),

  subHead('业务逻辑说明'),
  bullet('客户仅能访问本人创建的工单；越权返回 403'),
  bullet('仅客户可创建工单；客服/管理员创建返回 403'),
  bullet('评价：仅本人工单、仅 `RESOLVED`、每单一次（`ticket_id` 唯一）'),
  bullet('客户可通过留言接口或更新接口的 `extraMessage` 追加留言（前端详情页走留言接口）'),
  hr(),

  // 2.3
  heading('2.3 客服/管理工单台', HeadingLevel.HEADING_2),
  heading('2.3.1 功能设计及说明', HeadingLevel.HEADING_3),
  makeTable(
    ['项目', '说明'],
    [
      ['角色', 'AGENT、ADMIN'],
      ['功能简介', '查看全部工单，按状态筛选，变更状态/指派，并回复留言'],
      ['界面元素及控制说明', '见下方详细说明'],
    ],
    [2400, 6960]
  ),

  subHead('列表界面（/agent 客服后台）'),
  para('**查询区域：**'),
  bullet('状态：下拉选择，选项「待处理/处理中/已解决/已关闭」，可清空表示全部；精确匹配；非必填；同步到 URL `?status=`'),
  para('**列表区域：**'),
  bullet('展示：标题、客户、分类、状态、处理人（已指派/未指派）、创建时间、操作（处理）'),
  bullet('排序：创建时间倒序'),
  bullet('分页：接口默认 `page=0`、`size=20`（最大 100）'),
  para('**按钮区域：**'),
  bullet('处理/标题链接：进入 `/agent/tickets/:id`'),

  subHead('详情界面（/agent/tickets/:id）'),
  para('**展示区域：**'),
  bullet('工单基本信息、客户信息、分类、状态、指派情况、创建时间'),
  bullet('AI 建议回复（只读）+「填入回复框」快捷操作'),
  bullet('客户评价（如有，只读）'),
  bullet('留言时间线'),
  para('**操作区域：**'),
  makeTable(
    ['字段名', '控件类型', '必填', '说明'],
    [
      ['修改状态', '下拉选择，选项 PENDING/PROCESSING/RESOLVED/CLOSED', '—', '变更即提交；关闭（CLOSED）需二次确认'],
      ['回复内容', '文本域', '发送时必填', '可一键填入 AI 建议回复'],
    ],
    [1400, 3600, 1400, 2960]
  ),
  para('**按钮区域：**'),
  bullet('填入回复框：将 `aiSuggestedReply` 写入回复输入框（无建议时禁用）'),
  bullet('发送回复：调用留言接口；**不改变**工单状态'),
  bullet('修改状态：调用更新接口；提交时携带客户端持有的 `updatedAt` 做乐观锁；若未指派，默认将 `assignedTo` 设为当前登录用户'),

  subHead('业务逻辑说明'),
  bullet('客服/管理员可查看任意工单；可改 `status`、`assignedTo`'),
  bullet('指派对象须为 `AGENT` 或 `ADMIN`'),
  bullet('状态可选目标：`PENDING` → `PROCESSING` → `RESOLVED` → `CLOSED`（业务上允许按需选择目标状态，非强制逐步）'),
  bullet('乐观锁：请求体 `updatedAt` 须与库一致，冲突返回 **409**，提示刷新后重试'),
  bullet('客服留言不改变工单状态；客户在已解决/已关闭工单留言会打回 `PENDING`'),
  hr(),

  // 2.4
  heading('2.4 AI 辅助（分类与建议回复）', HeadingLevel.HEADING_2),
  heading('2.4.1 功能设计及说明', HeadingLevel.HEADING_3),
  makeTable(
    ['项目', '说明'],
    [
      ['角色', '系统自动（客户创建工单时触发）；客服可在详情页查看并采用建议回复'],
      ['功能简介', '根据工单标题与描述自动生成分类与建议回复'],
      ['界面元素及控制说明', '无独立配置页；结果展示在工单详情'],
    ],
    [2400, 6960]
  ),

  subHead('触发与结果展示'),
  bullet('触发时机：`POST /api/tickets` 创建成功路径内同步生成'),
  bullet('客户详情 / 客服详情：展示 `category`、`aiSuggestedReply`'),
  bullet('客服可将建议回复填入留言框后发送'),

  subHead('业务逻辑说明'),
  para('**优先 DeepSeek**（配置 `DEEPSEEK_API_KEY`）：'),
  bullet('模型：`deepseek-v4-flash`（关闭 thinking）'),
  bullet('一次输出：`category`（仅允许 `退货` / `物流` / `账户` / `其他`）+ `suggestedReply`'),
  para('**回退规则**（无 Key / 超时 / 非法 category / 空回复）：按标题+描述文本匹配，优先级从上到下：'),
  numbered('含「退货」或「退款」→ `退货`'),
  numbered('含「物流」或「快递」→ `物流`'),
  numbered('含「账户」或「登录」→ `账户`'),
  numbered('其他 → `其他`'),
  para('回退默认建议回复：`感谢您的反馈，我们已收到工单，预计 24 小时内处理。`'),
  para('提示词与参数细则见同目录 `AI_PROMPTS.md`。'),
  hr(),

  // 2.5
  heading('2.5 管理员日统计', HeadingLevel.HEADING_2),
  heading('2.5.1 功能设计及说明', HeadingLevel.HEADING_3),
  makeTable(
    ['项目', '说明'],
    [
      ['角色', 'ADMIN'],
      ['功能简介', '查询当日新增工单数及全库各状态计数'],
      ['界面元素及控制说明', '**后端接口已实现**；前端本期无独立统计页面'],
    ],
    [2400, 6960]
  ),

  subHead('接口能力（GET /api/stats/daily）'),
  para('**返回字段：**'),
  makeTable(
    ['字段名', '说明'],
    [
      ['date', '统计日期，yyyy-MM-dd'],
      ['newTicketsToday', '今日新增工单数（createdAt 落在当天 [00:00, 次日 00:00)）'],
      ['statusCounts.pending', '全库待处理数'],
      ['statusCounts.processing', '全库处理中数'],
      ['statusCounts.resolved', '全库已解决数'],
      ['statusCounts.closed', '全库已关闭数'],
    ],
    [2800, 6560]
  ),

  subHead('业务逻辑说明'),
  bullet('仅管理员可访问；非管理员返回 403'),
  bullet('未登录返回 401'),
  bullet('数据来自真实 `tickets` 表聚合，非缓存假数据'),
  hr(),

  // 附录
  heading('附录：权限一览', HeadingLevel.HEADING_1),
  makeTable(
    ['操作', 'CUSTOMER', 'AGENT', 'ADMIN'],
    [
      ['注册/登录', '✓', '✓', '✓'],
      ['创建工单', '✓', '✗', '✗'],
      ['看自己的工单', '✓', '—', '—'],
      ['看全部工单', '✗', '✓', '✓'],
      ['改状态 / 指派', '✗', '✓', '✓'],
      ['留言', '自己的', '任意', '任意'],
      ['评价', '自己的且已解决', '✗', '✗'],
      ['每日统计（接口）', '✗', '✗', '✓'],
    ],
    [2800, 2200, 2180, 2180]
  ),

  heading('附录：工单状态', HeadingLevel.HEADING_1),
  makeTable(
    ['状态值', '中文（前端标签）', '说明'],
    [
      ['PENDING', '待处理', '新建默认；客户在已解决/关闭后留言可打回'],
      ['PROCESSING', '处理中', '客服处理中'],
      ['RESOLVED', '已解决', '可评价'],
      ['CLOSED', '已关闭', '关闭操作建议二次确认'],
    ],
    [2000, 2200, 5160]
  ),

  heading('附录：前端路由', HeadingLevel.HEADING_1),
  makeTable(
    ['路径', '角色', '说明'],
    [
      ['/login  /register', '游客', '登录 / 注册'],
      ['/tickets  /tickets/new  /tickets/:id', '客户', '列表 / 新建 / 详情'],
      ['/agent  /agent/tickets/:id', '客服、管理员', '工作台 / 详情'],
    ],
    [4200, 1800, 3360]
  ),

  heading('附录：明确不做（当前版本）', HeadingLevel.HEADING_1),
  bullet('多轮 AI 客服对话 / RAG 知识库'),
  bullet('模型切换（固定 `deepseek-v4-flash`，不启用 Pro / thinking）'),
  bullet('文件附件上传'),
  bullet('邮件 / 站内通知推送'),
  bullet('细粒度 RBAC 与操作审计日志'),
  bullet('管理员日统计的独立前端页面（接口已具备）'),
];

const doc = new Document({
  styles: {
    default: {
      document: {
        styles: [{ id: 'Normal', run: { font: '微软雅黑', size: 21 } }],
      },
    },
    paragraphStyles: [
      {
        id: 'Title',
        name: 'Title',
        basedOn: 'Normal',
        next: 'Normal',
        quickStyle: true,
        run: { font: '微软雅黑', size: 36, bold: true, color: '1F4E79' },
        paragraph: { spacing: { before: 0, after: 240 }, alignment: AlignmentType.CENTER },
      },
      {
        id: 'Heading1',
        name: 'Heading 1',
        basedOn: 'Normal',
        next: 'Normal',
        quickStyle: true,
        run: { font: '微软雅黑', size: 28, bold: true, color: '1F4E79' },
        paragraph: { spacing: { before: 320, after: 140 }, outlineLevel: 0 },
      },
      {
        id: 'Heading2',
        name: 'Heading 2',
        basedOn: 'Normal',
        next: 'Normal',
        quickStyle: true,
        run: { font: '微软雅黑', size: 24, bold: true, color: '2E75B6' },
        paragraph: { spacing: { before: 260, after: 100 }, outlineLevel: 1 },
      },
      {
        id: 'Heading3',
        name: 'Heading 3',
        basedOn: 'Normal',
        next: 'Normal',
        quickStyle: true,
        run: { font: '微软雅黑', size: 22, bold: true, color: '5B9BD5' },
        paragraph: { spacing: { before: 200, after: 80 }, outlineLevel: 2 },
      },
    ],
  },
  numbering: {
    config: [
      {
        reference: 'bullets',
        levels: [{
          level: 0,
          format: LevelFormat.BULLET,
          text: '•',
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 420, hanging: 210 } } },
        }],
      },
      {
        reference: 'numbers',
        levels: [{
          level: 0,
          format: LevelFormat.DECIMAL,
          text: '%1.',
          alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 420, hanging: 210 } } },
        }],
      },
    ],
  },
  sections: [{
    properties: {
      page: {
        margin: { top: 1134, right: 1134, bottom: 1134, left: 1134 },
      },
    },
    children,
  }],
});

Packer.toBuffer(doc).then((buffer) => {
  fs.writeFileSync(OUT, buffer);
  console.log('Wrote:', OUT);
}).catch((err) => {
  console.error(err);
  process.exit(1);
});
