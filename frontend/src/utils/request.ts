import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResult } from '@/types/api'
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

request.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult | undefined
    if (body && typeof body === 'object' && 'success' in body && body.success === false) {
      const msg = body.message || '请求失败'
      ElMessage.error(msg)
      return Promise.reject(new Error(msg))
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
      clearStoredAuth()
      ElMessage.error(msg || '登录已过期，请重新登录')
      const { default: router } = await import('@/router')
      if (router.currentRoute.value.path !== '/login') {
        await router.push({
          path: '/login',
          query: { redirect: router.currentRoute.value.fullPath },
        })
      }
      return Promise.reject(error)
    }

    ElMessage.error(msg)
    return Promise.reject(error)
  },
)

export default request
