import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { LoginPayload, RegisterPayload, UserInfo, UserRole } from '@/types/user'
import { USER_KEY, getStoredToken, setStoredToken } from '@/utils/auth'

/** 是否使用本地 mock（后端未就绪时保持 true） */
const USE_MOCK = true

/**
 * 从 localStorage 恢复用户信息。
 */
function loadStoredUser(): UserInfo | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as UserInfo
  } catch {
    return null
  }
}

/**
 * 演示用户（mock 登录/注册）。
 */
const mockUsers: Array<UserInfo & { password: string }> = [
  {
    id: 'u-customer-1',
    username: 'customer',
    password: '123456',
    role: 'CUSTOMER',
    avatarUrl: null,
    createdAt: '2026-08-01T09:00:00',
  },
  {
    id: 'u-agent-1',
    username: 'agent',
    password: '123456',
    role: 'AGENT',
    avatarUrl: null,
    createdAt: '2026-08-01T09:00:00',
  },
  {
    id: 'u-admin-1',
    username: 'admin',
    password: '123456',
    role: 'ADMIN',
    avatarUrl: null,
    createdAt: '2026-08-01T09:00:00',
  },
]

/**
 * 用户状态：token、userInfo、role，以及登录/注册/退出。
 */
export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getStoredToken())
  const userInfo = ref<UserInfo | null>(loadStoredUser())

  const role = computed<UserRole | null>(() => userInfo.value?.role ?? null)
  const isLoggedIn = computed(() => Boolean(token.value && userInfo.value))
  const isCustomer = computed(() => role.value === 'CUSTOMER')
  const isAgent = computed(() => role.value === 'AGENT' || role.value === 'ADMIN')

  /**
   * 持久化登录态。
   *
   * @param nextToken JWT
   * @param user 用户信息
   */
  function persistSession(nextToken: string, user: UserInfo) {
    token.value = nextToken
    userInfo.value = user
    setStoredToken(nextToken)
    localStorage.setItem(USER_KEY, JSON.stringify(user))
  }

  /**
   * 登录。mock 模式下校验本地演示账号；任意未注册用户名也可按 CUSTOMER 临时登录（密码任意非空）。
   *
   * @param payload 用户名密码
   */
  async function login(payload: LoginPayload): Promise<UserInfo> {
    const username = payload.username.trim()
    const password = payload.password

    if (!username || !password) {
      throw new Error('请输入用户名和密码')
    }

    if (USE_MOCK) {
      await delay(200)
      const found = mockUsers.find((u) => u.username === username)
      if (found && found.password !== password) {
        throw new Error('用户名或密码错误')
      }
      const user: UserInfo = found
        ? {
            id: found.id,
            username: found.username,
            role: found.role,
            avatarUrl: found.avatarUrl,
            createdAt: found.createdAt,
          }
        : {
            id: `u-temp-${Date.now()}`,
            username,
            role: 'CUSTOMER',
            avatarUrl: null,
            createdAt: new Date().toISOString(),
          }
      persistSession(`mock-token-${user.id}`, user)
      return user
    }

    // 后端就绪后切换：import request from '@/utils/request'
    throw new Error('真实 API 尚未启用，请保持 USE_MOCK = true')
  }

  /**
   * 注册。mock 模式下写入本地演示用户列表并自动登录。
   *
   * @param payload 注册信息
   */
  async function register(payload: RegisterPayload): Promise<UserInfo> {
    const username = payload.username.trim()
    const password = payload.password
    const roleValue: UserRole = payload.role || 'CUSTOMER'

    if (!username || !password) {
      throw new Error('请输入用户名和密码')
    }
    if (password.length < 6) {
      throw new Error('密码至少 6 位')
    }

    if (USE_MOCK) {
      await delay(200)
      if (mockUsers.some((u) => u.username === username)) {
        throw new Error('用户名已存在')
      }
      const user: UserInfo & { password: string } = {
        id: `u-${Date.now()}`,
        username,
        password,
        role: roleValue,
        avatarUrl: null,
        createdAt: new Date().toISOString(),
      }
      mockUsers.push(user)
      const publicUser: UserInfo = {
        id: user.id,
        username: user.username,
        role: user.role,
        avatarUrl: user.avatarUrl,
        createdAt: user.createdAt,
      }
      persistSession(`mock-token-${user.id}`, publicUser)
      return publicUser
    }

    throw new Error('真实 API 尚未启用，请保持 USE_MOCK = true')
  }

  /**
   * 退出登录。
   */
  function logout() {
    token.value = null
    userInfo.value = null
    setStoredToken(null)
    localStorage.removeItem(USER_KEY)
  }

  return {
    token,
    userInfo,
    role,
    isLoggedIn,
    isCustomer,
    isAgent,
    login,
    register,
    logout,
  }
})

/**
 * 模拟网络延迟。
 *
 * @param ms 毫秒
 */
function delay(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}
