import type { TicketStatus } from '@/types/ticket'
import type { UserRole } from '@/types/user'

/** 工单状态中文标签 */
export const ticketStatusLabels: Record<TicketStatus, string> = {
  PENDING: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭',
}

/** 角色中文标签 */
export const roleLabels: Record<UserRole, string> = {
  CUSTOMER: '客户',
  AGENT: '客服',
  ADMIN: '管理员',
}

/** Element Plus Tag 类型映射 */
export const ticketStatusTagType: Record<
  TicketStatus,
  'info' | 'warning' | 'success' | 'danger'
> = {
  PENDING: 'info',
  PROCESSING: 'warning',
  RESOLVED: 'success',
  CLOSED: 'danger',
}
