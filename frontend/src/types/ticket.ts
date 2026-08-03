/**
 * 工单状态。
 */
export type TicketStatus = 'PENDING' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'

/**
 * 留言发送方角色（便于展示）。
 */
export type MessageSenderRole = 'CUSTOMER' | 'AGENT' | 'ADMIN'

/**
 * 工单留言。
 */
export interface TicketMessage {
  id: string
  ticketId: string
  senderId: string
  senderUsername: string
  senderRole: MessageSenderRole
  content: string
  createdAt: string
}

/**
 * 工单评价。
 */
export interface TicketFeedback {
  id: string
  ticketId: string
  rating: number
  comment?: string | null
  createdAt: string
}

/**
 * 工单列表项 / 详情。
 */
export interface Ticket {
  id: string
  title: string
  description: string
  category?: string | null
  status: TicketStatus
  customerId: string
  customerUsername: string
  assignedToId?: string | null
  assignedToUsername?: string | null
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
 * 更新工单请求（客服改状态 / 客户追加留言兼容字段）。
 */
export interface UpdateTicketPayload {
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
