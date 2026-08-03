import { defineStore } from 'pinia'
import { ref } from 'vue'
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

/** 是否使用本地 mock */
const USE_MOCK = true

/**
 * 初始 mock 工单数据。
 */
function createMockTickets(): Ticket[] {
  return [
    {
      id: 't-1001',
      title: '订单退款未到账',
      description: '上周申请退货退款，款项仍未到账，请协助查询。',
      category: '退货',
      status: 'PENDING',
      customerId: 'u-customer-1',
      customerUsername: 'customer',
      assignedToId: null,
      assignedToUsername: null,
      aiSuggestedReply: '感谢您的反馈，我们已收到工单，预计 24 小时内处理。',
      messages: [
        {
          id: 'm-1',
          ticketId: 't-1001',
          senderId: 'u-customer-1',
          senderUsername: 'customer',
          senderRole: 'CUSTOMER',
          content: '麻烦尽快处理，急需这笔钱。',
          createdAt: '2026-08-02T10:30:00',
        },
      ],
      feedback: null,
      createdAt: '2026-08-02T10:00:00',
      updatedAt: '2026-08-02T10:30:00',
    },
    {
      id: 't-1002',
      title: '快递一直显示运输中',
      description: '物流单号 SF123456，已超过预计送达时间。',
      category: '物流',
      status: 'PROCESSING',
      customerId: 'u-customer-1',
      customerUsername: 'customer',
      assignedToId: 'u-agent-1',
      assignedToUsername: 'agent',
      aiSuggestedReply: '感谢您的反馈，我们已收到工单，预计 24 小时内处理。',
      messages: [
        {
          id: 'm-2',
          ticketId: 't-1002',
          senderId: 'u-agent-1',
          senderUsername: 'agent',
          senderRole: 'AGENT',
          content: '已联系承运商催促，预计明日送达。',
          createdAt: '2026-08-02T15:00:00',
        },
      ],
      feedback: null,
      createdAt: '2026-08-01T14:00:00',
      updatedAt: '2026-08-02T15:00:00',
    },
    {
      id: 't-1003',
      title: '无法登录账户',
      description: '重置密码后仍提示账号或密码错误。',
      category: '账户',
      status: 'RESOLVED',
      customerId: 'u-customer-1',
      customerUsername: 'customer',
      assignedToId: 'u-agent-1',
      assignedToUsername: 'agent',
      aiSuggestedReply: '感谢您的反馈，我们已收到工单，预计 24 小时内处理。',
      messages: [
        {
          id: 'm-3',
          ticketId: 't-1003',
          senderId: 'u-agent-1',
          senderUsername: 'agent',
          senderRole: 'AGENT',
          content: '已为您解锁账户，请使用新密码登录。',
          createdAt: '2026-08-01T18:00:00',
        },
      ],
      feedback: null,
      createdAt: '2026-07-30T09:00:00',
      updatedAt: '2026-08-01T18:00:00',
    },
    {
      id: 't-1004',
      title: '咨询发票开具',
      description: '需要开具增值税专用发票，请问流程是什么？',
      category: '其他',
      status: 'CLOSED',
      customerId: 'u-customer-1',
      customerUsername: 'customer',
      assignedToId: 'u-agent-1',
      assignedToUsername: 'agent',
      aiSuggestedReply: '感谢您的反馈，我们已收到工单，预计 24 小时内处理。',
      messages: [],
      feedback: {
        id: 'f-1',
        ticketId: 't-1004',
        rating: 5,
        comment: '解答清楚，谢谢。',
        createdAt: '2026-07-28T12:00:00',
      },
      createdAt: '2026-07-27T11:00:00',
      updatedAt: '2026-07-28T12:00:00',
    },
  ]
}

let mockTickets = createMockTickets()

/**
 * 根据标题/描述自动分类（与后端约定一致）。
 *
 * @param title 标题
 * @param description 描述
 */
function classifyTicket(title: string, description: string): string {
  const text = `${title}${description}`
  if (/退货|退款/.test(text)) return '退货'
  if (/物流|快递/.test(text)) return '物流'
  if (/账户|登录/.test(text)) return '账户'
  return '其他'
}

/**
 * 工单状态：列表、当前详情、加载状态及 mock CRUD。
 */
export const useTicketStore = defineStore('ticket', () => {
  const tickets = ref<Ticket[]>([])
  const currentTicket = ref<Ticket | null>(null)
  const loading = ref(false)

  /**
   * 加载工单列表。
   *
   * @param status 可选状态筛选（客服端）
   */
  async function fetchTickets(status?: TicketStatus): Promise<void> {
    loading.value = true
    try {
      if (USE_MOCK) {
        await delay(250)
        const userStore = useUserStore()
        let list = [...mockTickets]
        if (userStore.isCustomer && userStore.userInfo) {
          list = list.filter((t) => t.customerId === userStore.userInfo!.id)
        }
        if (status) {
          list = list.filter((t) => t.status === status)
        }
        list.sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))
        tickets.value = list.map((t) => ({ ...t, messages: undefined }))
        return
      }
      throw new Error('真实 API 尚未启用')
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
      if (USE_MOCK) {
        await delay(200)
        const found = mockTickets.find((t) => t.id === id)
        if (!found) {
          throw new Error('工单不存在')
        }
        const userStore = useUserStore()
        if (
          userStore.isCustomer &&
          userStore.userInfo &&
          found.customerId !== userStore.userInfo.id
        ) {
          throw new Error('无权查看该工单')
        }
        const detail: Ticket = {
          ...found,
          messages: [...(found.messages || [])].sort((a, b) =>
            a.createdAt > b.createdAt ? 1 : -1,
          ),
          feedback: found.feedback ? { ...found.feedback } : null,
        }
        currentTicket.value = detail
        return detail
      }
      throw new Error('真实 API 尚未启用')
    } finally {
      loading.value = false
    }
  }

  /**
   * 创建工单。
   *
   * @param payload 标题与描述
   */
  async function createTicket(payload: CreateTicketPayload): Promise<Ticket> {
    loading.value = true
    try {
      if (USE_MOCK) {
        await delay(200)
        const userStore = useUserStore()
        if (!userStore.userInfo || !userStore.isCustomer) {
          throw new Error('仅客户可创建工单')
        }
        const now = new Date().toISOString()
        const ticket: Ticket = {
          id: `t-${Date.now()}`,
          title: payload.title.trim(),
          description: payload.description.trim(),
          category: classifyTicket(payload.title, payload.description),
          status: 'PENDING',
          customerId: userStore.userInfo.id,
          customerUsername: userStore.userInfo.username,
          assignedToId: null,
          assignedToUsername: null,
          aiSuggestedReply: '感谢您的反馈，我们已收到工单，预计 24 小时内处理。',
          messages: [],
          feedback: null,
          createdAt: now,
          updatedAt: now,
        }
        mockTickets = [ticket, ...mockTickets]
        tickets.value = [ticket, ...tickets.value]
        return ticket
      }
      throw new Error('真实 API 尚未启用')
    } finally {
      loading.value = false
    }
  }

  /**
   * 更新工单（状态 / 指派 / 兼容 extraMessage）。
   *
   * @param id 工单 ID
   * @param payload 更新字段
   */
  async function updateTicket(id: string, payload: UpdateTicketPayload): Promise<Ticket> {
    loading.value = true
    try {
      if (USE_MOCK) {
        await delay(200)
        const idx = mockTickets.findIndex((t) => t.id === id)
        if (idx < 0) throw new Error('工单不存在')
        const ticket = mockTickets[idx]
        const userStore = useUserStore()
        const now = new Date().toISOString()

        if (payload.status && userStore.isAgent) {
          ticket.status = payload.status
          if (!ticket.assignedToId && userStore.userInfo) {
            ticket.assignedToId = userStore.userInfo.id
            ticket.assignedToUsername = userStore.userInfo.username
          }
        }
        if (payload.assignedTo && userStore.isAgent) {
          ticket.assignedToId = payload.assignedTo
          ticket.assignedToUsername = userStore.userInfo?.username || ticket.assignedToUsername
        }
        if (payload.extraMessage?.trim() && userStore.userInfo) {
          const msg = buildMessage(ticket, userStore.userInfo, payload.extraMessage.trim(), now)
          ticket.messages = [...(ticket.messages || []), msg]
          if (
            userStore.isCustomer &&
            (ticket.status === 'RESOLVED' || ticket.status === 'CLOSED')
          ) {
            ticket.status = 'PENDING'
          }
        }
        ticket.updatedAt = now
        mockTickets[idx] = ticket
        const detail = await fetchTicketDetail(id)
        return detail
      }
      throw new Error('真实 API 尚未启用')
    } finally {
      loading.value = false
    }
  }

  /**
   * 追加留言。
   *
   * @param id 工单 ID
   * @param payload 留言内容
   */
  async function addMessage(id: string, payload: CreateMessagePayload): Promise<TicketMessage> {
    loading.value = true
    try {
      if (USE_MOCK) {
        await delay(150)
        const userStore = useUserStore()
        if (!userStore.userInfo) throw new Error('请先登录')
        const content = payload.content.trim()
        if (!content) throw new Error('留言内容不能为空')

        const idx = mockTickets.findIndex((t) => t.id === id)
        if (idx < 0) throw new Error('工单不存在')
        const ticket = mockTickets[idx]

        if (userStore.isCustomer && ticket.customerId !== userStore.userInfo.id) {
          throw new Error('无权留言')
        }

        const now = new Date().toISOString()
        const message = buildMessage(ticket, userStore.userInfo, content, now)
        ticket.messages = [...(ticket.messages || []), message]

        if (
          userStore.isCustomer &&
          (ticket.status === 'RESOLVED' || ticket.status === 'CLOSED')
        ) {
          ticket.status = 'PENDING'
        }
        ticket.updatedAt = now
        mockTickets[idx] = ticket
        currentTicket.value = {
          ...ticket,
          messages: [...(ticket.messages || [])],
          feedback: ticket.feedback ? { ...ticket.feedback } : null,
        }
        return message
      }
      throw new Error('真实 API 尚未启用')
    } finally {
      loading.value = false
    }
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
    loading.value = true
    try {
      if (USE_MOCK) {
        await delay(150)
        const userStore = useUserStore()
        if (!userStore.isCustomer || !userStore.userInfo) {
          throw new Error('仅客户可评价')
        }
        if (payload.rating < 1 || payload.rating > 5) {
          throw new Error('评分须为 1-5')
        }

        const idx = mockTickets.findIndex((t) => t.id === id)
        if (idx < 0) throw new Error('工单不存在')
        const ticket = mockTickets[idx]
        if (ticket.customerId !== userStore.userInfo.id) {
          throw new Error('无权评价该工单')
        }
        if (ticket.status !== 'RESOLVED') {
          throw new Error('仅已解决的工单可评价')
        }
        if (ticket.feedback) {
          throw new Error('该工单已评价')
        }

        const feedback: TicketFeedback = {
          id: `f-${Date.now()}`,
          ticketId: id,
          rating: payload.rating,
          comment: payload.comment?.trim() || null,
          createdAt: new Date().toISOString(),
        }
        ticket.feedback = feedback
        ticket.updatedAt = feedback.createdAt
        mockTickets[idx] = ticket
        currentTicket.value = {
          ...ticket,
          messages: [...(ticket.messages || [])],
          feedback: { ...feedback },
        }
        return feedback
      }
      throw new Error('真实 API 尚未启用')
    } finally {
      loading.value = false
    }
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
    fetchTickets,
    fetchTicketDetail,
    createTicket,
    updateTicket,
    addMessage,
    submitFeedback,
    clearCurrentTicket,
  }
})

/**
 * 构造留言对象。
 */
function buildMessage(
  ticket: Ticket,
  user: { id: string; username: string; role: TicketMessage['senderRole'] },
  content: string,
  createdAt: string,
): TicketMessage {
  return {
    id: `m-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`,
    ticketId: ticket.id,
    senderId: user.id,
    senderUsername: user.username,
    senderRole: user.role,
    content,
    createdAt,
  }
}

/**
 * 模拟网络延迟。
 *
 * @param ms 毫秒
 */
function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
