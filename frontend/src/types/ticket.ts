/**
 * 工单状态。
 */
export type TicketStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'

/**
 * 工单留言（对齐后端 MessageResponse）。
 */
export interface TicketMessage {
  id: string
  ticketId: string
  /** 发送者用户 ID */
  userId: string
  /** 发送者用户名 */
  username: string
  content: string
  createdAt: string
}

/**
 * 工单评价（对齐后端 FeedbackResponse）。
 */
export interface TicketFeedback {
  id: string
  ticketId: string
  rating: number
  comment?: string | null
  createdAt: string
}

/**
 * 工单列表项 / 详情（对齐后端 TicketResponse / TicketDetailResponse）。
 */
export interface Ticket {
  id: string
  title: string
  description: string
  category?: string | null
  status: TicketStatus
  customerId: string
  customerUsername: string
  /** 指派客服用户 ID */
  assignedTo?: string | null
  aiSuggestedReply?: string | null
  messages?: TicketMessage[]
  feedback?: TicketFeedback | null
  createdAt: string
  updatedAt: string
}

/**
 * 新建工单请求。
 */
export interface CreateTicketPayload {
  title: string
  description: string
}

/**
 * 更新工单请求（客服改状态 / 客户 extraMessage）。
 */
export interface UpdateTicketPayload {
  /** 乐观锁：客户端持有的 updatedAt，必填 */
  updatedAt: string
  status?: TicketStatus
  assignedTo?: string
  extraMessage?: string
}

/**
 * 追加留言请求。
 */
export interface CreateMessagePayload {
  content: string
}

/**
 * 提交评价请求。
 */
export interface CreateFeedbackPayload {
  rating: number
  comment?: string
}
