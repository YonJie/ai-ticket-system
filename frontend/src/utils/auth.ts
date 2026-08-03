const TOKEN_KEY = 'ai_ticket_token'
const USER_KEY = 'ai_ticket_user'

/**
 * 读取本地存储的 JWT。
 */
export function getStoredToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/**
 * 写入或清除本地 JWT。
 *
 * @param token JWT；传 null 时清除
 */
export function setStoredToken(token: string | null): void {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token)
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

/**
 * 清除本地登录态缓存键。
 */
export function clearStoredAuth(): void {
  setStoredToken(null)
  localStorage.removeItem(USER_KEY)
}

export { USER_KEY }
