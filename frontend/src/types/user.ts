/**
 * 用户角色。
 */
export type UserRole = 'CUSTOMER' | 'AGENT' | 'ADMIN'

/**
 * 用户信息（前端 camelCase）。
 */
export interface UserInfo {
  id: string
  username: string
  role: UserRole
  avatarUrl?: string | null
  createdAt?: string
}

/**
 * 登录响应。
 */
export interface LoginResult {
  token: string
  user: UserInfo
}

/**
 * 登录请求体。
 */
export interface LoginPayload {
  username: string
  password: string
}

/**
 * 注册请求体。
 */
export interface RegisterPayload {
  username: string
  password: string
  role?: UserRole
}
