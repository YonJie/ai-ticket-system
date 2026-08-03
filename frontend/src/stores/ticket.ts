import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  addMessageApi,
  createTicketApi,
  fetchTicketDetailApi,
  fetchTicketsApi,
  submitFeedbackApi,
  updateTicketApi,
} from '@/api/ticket'
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
import { useUserStore } from './user'

/**
 * 工单状态：列表、当前详情、加载状态；对接真实 API。
 */
export const useTicketStore = defineStore('ticket', () => {
  const tickets = ref<Ticket[]>([])
  const currentTicket = ref<Ticket | null>(null)
  const loading = ref(false)
  const total = ref(0)

  /**
   * 加载工单列表。
   *
   * @param status 可选状态筛选（客服端）
   */
  async function fetchTickets(status?: TicketStatus): Promise<void> {
    loading.value = true
    try {
      const page = await fetchTicketsApi({ status, page: 0, size: 50 })
      tickets.value = page.content
      total.value = page.total
    } finally {
      loading.value = false
    }
  }

  /**
   * 加载工单详情。
   *
   * @param id 工单 ID
   */
  async function fetchTicketDetail(id: string): Promise<Ticket> {
    loading.value = true
    try {
      const detail = await fetchTicketDetailApi(id)
      currentTicket.value = detail
      return detail
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建工单（服务端自动分类与 AI 建议回复）。
   *
   * @param payload 标题与描述
   */
  async function createTicket(payload: CreateTicketPayload): Promise<Ticket> {
    const userStore = useUserStore()
    if (!userStore.isCustomer) {
      throw new Error('仅客户可创建工单')
    }
    const ticket = await createTicketApi(payload)
    tickets.value = [ticket, ...tickets.value]
    total.value += 1
    return ticket
  }

  /**
   * 更新工单（状态 / 指派 / 兼容 extraMessage）。
   * 若未传 updatedAt，自动使用当前详情中的值。
   *
   * @param id 工单 ID
   * @param payload 更新字段
   */
  async function updateTicket(
    id: string,
    payload: Omit<UpdateTicketPayload, 'updatedAt'> & { updatedAt?: string },
  ): Promise<Ticket> {
    const updatedAt = payload.updatedAt || currentTicket.value?.updatedAt
    if (!updatedAt) {
      throw new Error('缺少 updatedAt，请刷新后重试')
    }
    await updateTicketApi(id, { ...payload, updatedAt })
    // PATCH 返回基础 Ticket，需重新拉详情以同步留言/评价
    return fetchTicketDetail(id)
  }

  /**
   * 追加留言，成功后刷新详情。
   *
   * @param id 工单 ID
   * @param payload 留言内容
   */
  async function addMessage(id: string, payload: CreateMessagePayload): Promise<TicketMessage> {
    const message = await addMessageApi(id, payload)
    await fetchTicketDetail(id)
    return message
  }

  /**
   * 提交评价（仅 resolved 且未评价）。
   *
   * @param id 工单 ID
   * @param payload 评分与评论
   */
  async function submitFeedback(
    id: string,
    payload: CreateFeedbackPayload,
  ): Promise<TicketFeedback> {
    const feedback = await submitFeedbackApi(id, payload)
    await fetchTicketDetail(id)
    return feedback
  }

  /**
   * 清空当前详情。
   */
  function clearCurrentTicket() {
    currentTicket.value = null
  }

  return {
    tickets,
    currentTicket,
    loading,
    total,
    fetchTickets,
    fetchTicketDetail,
    createTicket,
    updateTicket,
    addMessage,
    submitFeedback,
    clearCurrentTicket,
  }
})
