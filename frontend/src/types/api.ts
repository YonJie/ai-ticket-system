/**
 * 统一 API 响应结构。
 */
export interface ApiResult<T = unknown> {
  success: boolean
  data: T
  message?: string
  code?: number
}

/**
 * 分页结果。
 */
export interface PageResult<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}
