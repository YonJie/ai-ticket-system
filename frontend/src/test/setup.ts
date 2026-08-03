/**
 * Vitest 全局 setup：提供 localStorage 等浏览器 API 兜底。
 */
import { afterEach } from 'vitest'

afterEach(() => {
  localStorage.clear()
})
