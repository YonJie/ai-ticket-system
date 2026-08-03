import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import { loginApi, registerApi } from '@/api/auth'
import type { LoginPayload, RegisterPayload, UserInfo, UserRole } from '@/types/user'
import { USER_KEY, getStoredToken, setStoredToken } from '@/utils/auth'

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
   * 登录并写入本地会话。
   *
   * @param payload 用户名密码
   */
  async function login(payload: LoginPayload): Promise<UserInfo> {
    const username = payload.username.trim()
    const password = payload.password
    if (!username || !password) {
      throw new Error('请输入用户名和密码')
    }
    const result = await loginApi({ username, password })
    persistSession(result.token, result.user)
    return result.user
  }

  /**
   * 注册成功后自动登录。
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
    if (username.length < 2) {
      throw new Error('用户名至少 2 个字符')
    }
    if (password.length < 6) {
      throw new Error('密码至少 6 位')
    }

    await registerApi({ username, password, role: roleValue })
    return login({ username, password })
  }

  /**
   * 退出登录。
   *
   * @param clearStorage 是否清理 localStorage（默认 true）
   */
  function logout(clearStorage = true) {
    token.value = null
    userInfo.value = null
    if (clearStorage) {
      setStoredToken(null)
      localStorage.removeItem(USER_KEY)
    }
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
