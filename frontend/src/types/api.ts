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
 * 分页结果（与后端 PageResult 对齐）。
 */
export interface PageResult<T> {
  content: T[]
  /** 总条数 */
  total: number
  /** 当前页，从 0 开始 */
  page: number
  size: number
  totalPages: number
}

/**
 * 已由 request 拦截器提示过的 API 错误。
 */
export class ApiError extends Error {
  readonly handled = true
  readonly code?: number

  /**
   * @param message 错误文案
   * @param code 业务/HTTP 状态码
   */
  constructor(message: string, code?: number) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

/**
 * 判断错误是否已由拦截器提示，避免页面重复 ElMessage。
 *
 * @param error 捕获的错误
 */
export function isHandledApiError(error: unknown): boolean {
  return error instanceof ApiError || (error instanceof Error && (error as ApiError).handled === true)
}
