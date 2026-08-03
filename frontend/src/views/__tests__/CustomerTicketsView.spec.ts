import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import CustomerTicketsView from '@/views/CustomerTicketsView.vue'
import { useTicketStore } from '@/stores/ticket'
import type { Ticket } from '@/types/ticket'

/**
 * 构建工单列表页测试用 router。
 */
function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/tickets', component: CustomerTicketsView },
      { path: '/tickets/new', component: { template: '<div>new</div>' } },
      { path: '/tickets/:id', component: { template: '<div>detail</div>' } },
    ],
  })
}

/**
 * 构造测试用工单。
 *
 * @param overrides 覆盖字段
 */
function buildTicket(overrides: Partial<Ticket> = {}): Ticket {
  return {
    id: 't1',
    title: '申请退货',
    description: '商品有问题',
    category: '退货',
    status: 'PENDING',
    customerId: 'c1',
    customerUsername: 'customer',
    createdAt: '2026-08-03T10:00:00',
    updatedAt: '2026-08-03T10:00:00',
    ...overrides,
  }
}

describe('CustomerTicketsView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  /**
   * 挂载时应拉取工单列表。
   */
  it('fetches tickets on mount', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const ticketStore = useTicketStore()
    const fetchSpy = vi.spyOn(ticketStore, 'fetchTickets').mockResolvedValue()

    const router = createTestRouter()
    await router.push('/tickets')
    await router.isReady()

    mount(CustomerTicketsView, {
      global: {
        plugins: [pinia, router, ElementPlus],
        stubs: { AppHeader: true },
      },
    })
    await flushPromises()

    expect(fetchSpy).toHaveBeenCalled()
  })

  /**
   * 有数据时应展示工单标题。
   */
  it('renders ticket title when store has data', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const ticketStore = useTicketStore()
    vi.spyOn(ticketStore, 'fetchTickets').mockImplementation(async () => {
      ticketStore.tickets = [buildTicket({ title: '申请退货' })]
    })

    const router = createTestRouter()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(CustomerTicketsView, {
      global: {
        plugins: [pinia, router, ElementPlus],
        stubs: { AppHeader: true },
      },
    })
    await flushPromises()

    expect(wrapper.text()).toContain('我的工单')
    expect(wrapper.text()).toContain('申请退货')
  })

  /**
   * 空列表时应保留空状态提示文案配置。
   */
  it('shows empty state text configuration when no tickets', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const ticketStore = useTicketStore()
    vi.spyOn(ticketStore, 'fetchTickets').mockResolvedValue()
    ticketStore.tickets = []

    const router = createTestRouter()
    await router.push('/tickets')
    await router.isReady()

    const wrapper = mount(CustomerTicketsView, {
      global: {
        plugins: [pinia, router, ElementPlus],
        stubs: { AppHeader: true },
      },
    })
    await flushPromises()

    expect(wrapper.html()).toContain('暂无工单')
  })
})
