/// <reference types="vite/client" />

import 'vue-router'
import type { UserRole } from './types/user'

declare module 'vue-router' {
  interface RouteMeta {
    /** 需登录 */
    requiresAuth?: boolean
    /** 仅游客（已登录则重定向） */
    guest?: boolean
    /** 允许访问的角色 */
    roles?: UserRole[]
  }
}
