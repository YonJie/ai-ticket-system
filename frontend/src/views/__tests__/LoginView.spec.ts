import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import ElementPlus from 'element-plus'
import LoginView from '@/views/LoginView.vue'
import { useUserStore } from '@/stores/user'

/**
 * 构建带登录路由的测试用 router。
 */
function createTestRouter() {
  return createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', component: LoginView },
      { path: '/tickets', component: { template: '<div>tickets</div>' } },
      { path: '/agent', component: { template: '<div>agent</div>' } },
      { path: '/register', component: { template: '<div>register</div>' } },
    ],
  })
}

describe('LoginView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  /**
   * 应渲染登录标题与表单控件。
   */
  it('renders login form', async () => {
    const router = createTestRouter()
    await router.push('/login')
    await router.isReady()

    const wrapper = mount(LoginView, {
      global: {
        plugins: [createPinia(), router, ElementPlus],
        stubs: { AppHeader: true },
      },
    })

    expect(wrapper.find('h1').text()).toBe('登录')
    expect(wrapper.find('input').exists()).toBe(true)
    expect(wrapper.text()).toContain('登录')
  })

  /**
   * 点击登录应调用 userStore.login，并跳转客户首页。
   */
  it('submits login and redirects customer to tickets', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const userStore = useUserStore()
    const loginSpy = vi.spyOn(userStore, 'login').mockResolvedValue({
      id: 'u1',
      username: 'customer',
      role: 'CUSTOMER',
      createdAt: '2026-08-03T00:00:00',
    })

    const router = createTestRouter()
    const replaceSpy = vi.spyOn(router, 'replace')
    await router.push('/login')
    await router.isReady()

    const wrapper = mount(LoginView, {
      global: {
        plugins: [pinia, router, ElementPlus],
        stubs: { AppHeader: true },
      },
    })

    const button = wrapper.findAll('button').find((b) => b.text().includes('登录'))
    expect(button).toBeTruthy()
    await button!.trigger('click')
    await flushPromises()

    expect(loginSpy).toHaveBeenCalledWith({
      username: 'customer',
      password: '123456',
    })
    expect(replaceSpy).toHaveBeenCalledWith('/tickets')
  })
})
