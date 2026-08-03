import axios from 'axios'

/**
 * Axios 实例，默认走 /api 前缀以便本地代理与 Vercel 路由。
 */
const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error),
)

export default http
