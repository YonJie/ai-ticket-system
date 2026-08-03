import request from '@/utils/request'
import type { ApiResult, PageResult } from '@/types/api'
import type {
  CreateFeedbackPayload,
  CreateMessagePayload,
  CreateTicketPayload,
  Ticket,
  TicketFeedback,
  TicketMessage,
  TicketStatus,
  UpdateTicketPayload,
} from '@/types/ticket'
import { normalizeDateTime, normalizeId } from '@/utils/normalize'

/**
 * 规范化留言对象。
 *
 * @param raw 后端留言
 */
export function mapMessage(raw: Record<string, unknown>): TicketMessage {
  return {
    id: normalizeId(raw.id),
    ticketId: normalizeId(raw.ticketId),
    userId: normalizeId(raw.userId ?? raw.senderId),
    username: String(raw.username ?? raw.senderUsername ?? ''),
    content: String(raw.content ?? ''),
    createdAt: normalizeDateTime(raw.createdAt ?? raw.createAt),
  }
}

/**
 * 规范化评价对象。
 *
 * @param raw 后端评价
 */
export function mapFeedback(raw: Record<string, unknown> | null | undefined): TicketFeedback | null {
  if (!raw) return null
  return {
    id: normalizeId(raw.id),
    ticketId: normalizeId(raw.ticketId),
    rating: Number(raw.rating),
    comment: (raw.comment as string | null | undefined) ?? null,
    createdAt: normalizeDateTime(raw.createdAt ?? raw.createAt),
  }
}

/**
 * 规范化工单对象（列表/详情）。
 *
 * @param raw 后端工单
 */
export function mapTicket(raw: Record<string, unknown>): Ticket {
  const messagesRaw = raw.messages
  const messages = Array.isArray(messagesRaw)
    ? (messagesRaw as Record<string, unknown>[]).map(mapMessage)
    : undefined

  return {
    id: normalizeId(raw.id),
    title: String(raw.title ?? ''),
    description: String(raw.description ?? ''),
    category: (raw.category as string | null | undefined) ?? null,
    status: String(raw.status ?? 'PENDING').toUpperCase() as TicketStatus,
    customerId: normalizeId(raw.customerId),
    customerUsername: String(raw.customerUsername ?? ''),
    assignedTo: raw.assignedTo != null ? normalizeId(raw.assignedTo ?? raw.assignedToId) : null,
    aiSuggestedReply: (raw.aiSuggestedReply as string | null | undefined) ?? null,
    messages,
    feedback: mapFeedback(raw.feedback as Record<string, unknown> | null | undefined),
    createdAt: normalizeDateTime(raw.createdAt ?? raw.createAt),
    updatedAt: normalizeDateTime(raw.updatedAt ?? raw.updateAt),
  }
}

/**
 * 获取工单列表。
 *
 * @param params 筛选与分页
 */
export async function fetchTicketsApi(params?: {
  status?: TicketStatus
  page?: number
  size?: number
}): Promise<PageResult<Ticket>> {
  const { data } = await request.get<ApiResult<PageResult<Record<string, unknown>>>>('/tickets', {
    params: {
      status: params?.status || undefined,
      page: params?.page ?? 0,
      size: params?.size ?? 50,
    },
  })
  const page = data.data
  return {
    content: (page.content || []).map(mapTicket),
    total: page.total ?? (page as unknown as { totalElements?: number }).totalElements ?? 0,
    page: page.page ?? 0,
    size: page.size ?? 50,
    totalPages: page.totalPages ?? 0,
  }
}

/**
 * 获取工单详情。
 *
 * @param id 工单 ID
 */
export async function fetchTicketDetailApi(id: string): Promise<Ticket> {
  const { data } = await request.get<ApiResult<Record<string, unknown>>>(`/tickets/${id}`)
  return mapTicket(data.data)
}

/**
 * 创建工单。
 *
 * @param payload 标题与描述
 */
export async function createTicketApi(payload: CreateTicketPayload): Promise<Ticket> {
  const { data } = await request.post<ApiResult<Record<string, unknown>>>('/tickets', {
    title: payload.title.trim(),
    description: payload.description.trim(),
  })
  return mapTicket(data.data)
}

/**
 * 更新工单（状态 / 指派 / 客户 extraMessage）。
 *
 * @param id 工单 ID
 * @param payload 更新字段（须含 updatedAt）
 */
export async function updateTicketApi(id: string, payload: UpdateTicketPayload): Promise<Ticket> {
  const { data } = await request.patch<ApiResult<Record<string, unknown>>>(`/tickets/${id}`, payload)
  return mapTicket(data.data)
}

/**
 * 追加留言。
 *
 * @param id 工单 ID
 * @param payload 留言内容
 */
export async function addMessageApi(
  id: string,
  payload: CreateMessagePayload,
): Promise<TicketMessage> {
  const { data } = await request.post<ApiResult<Record<string, unknown>>>(
    `/tickets/${id}/messages`,
    { content: payload.content.trim() },
  )
  return mapMessage(data.data)
}

/**
 * 提交评价。
 *
 * @param id 工单 ID
 * @param payload 评分与评论
 */
export async function submitFeedbackApi(
  id: string,
  payload: CreateFeedbackPayload,
): Promise<TicketFeedback> {
  const { data } = await request.post<ApiResult<Record<string, unknown>>>(
    `/tickets/${id}/feedback`,
    {
      rating: payload.rating,
      comment: payload.comment?.trim() || undefined,
    },
  )
  return mapFeedback(data.data)!
}
