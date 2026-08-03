import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { ApiError, type ApiResult } from '@/types/api'
import { clearStoredAuth, getStoredToken } from '@/utils/auth'

/**
 * Axios 实例：自动带 Authorization，统一错误提示，401 跳转登录。
 */
const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getStoredToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 清理本地与 Pinia 登录态后跳转登录页。
 *
 * @param message 提示文案
 */
async function handleUnauthorized(message: string) {
  clearStoredAuth()
  try {
    const { useUserStore } = await import('@/stores/user')
    useUserStore().logout(false)
  } catch {
    // Pinia 未就绪时忽略
  }
  ElMessage.error(message || '登录已过期，请重新登录')
  const { default: router } = await import('@/router')
  if (router.currentRoute.value.path !== '/login') {
    await router.push({
      path: '/login',
      query: { redirect: router.currentRoute.value.fullPath },
    })
  }
}

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult | undefined
    // /health 等非统一包装响应直接放行
    if (body && typeof body === 'object' && 'success' in body && body.success === false) {
      const msg = body.message || '请求失败'
      const code = body.code
      if (code === 401 || response.status === 401) {
        void handleUnauthorized(msg)
      } else {
        ElMessage.error(msg)
      }
      return Promise.reject(new ApiError(msg, code))
    }
    return response
  },
  async (error: AxiosError<ApiResult>) => {
    const status = error.response?.status
    const msg =
      error.response?.data?.message ||
      error.message ||
      '网络异常，请稍后重试'

    if (status === 401) {
      await handleUnauthorized(msg)
      return Promise.reject(new ApiError(msg, 401))
    }

    ElMessage.error(msg)
    return Promise.reject(new ApiError(msg, status))
  },
)

export default request
