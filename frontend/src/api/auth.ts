import request from '@/utils/request'
import type { ApiResult } from '@/types/api'
import type { LoginPayload, LoginResult, RegisterPayload, UserInfo } from '@/types/user'
import { normalizeDateTime, normalizeId } from '@/utils/normalize'

/**
 * 规范化用户信息字段。
 *
 * @param raw 后端用户对象
 */
export function mapUser(raw: Record<string, unknown>): UserInfo {
  return {
    id: normalizeId(raw.id),
    username: String(raw.username ?? ''),
    role: String(raw.role ?? 'CUSTOMER').toUpperCase() as UserInfo['role'],
    avatarUrl: (raw.avatarUrl as string | null | undefined) ?? null,
    createdAt: normalizeDateTime(raw.createdAt),
  }
}

/**
 * 用户注册（不返回 token，需再登录）。
 *
 * @param payload 注册参数
 */
export async function registerApi(payload: RegisterPayload): Promise<UserInfo> {
  const { data } = await request.post<ApiResult<Record<string, unknown>>>('/auth/register', {
    username: payload.username.trim(),
    password: payload.password,
    role: payload.role || 'CUSTOMER',
  })
  return mapUser(data.data)
}

/**
 * 用户登录。
 *
 * @param payload 登录参数
 */
export async function loginApi(payload: LoginPayload): Promise<LoginResult> {
  const { data } = await request.post<ApiResult<{ token: string; user: Record<string, unknown> }>>(
    '/auth/login',
    {
      username: payload.username.trim(),
      password: payload.password,
    },
  )
  return {
    token: data.data.token,
    user: mapUser(data.data.user),
  }
}

/**
 * 获取当前登录用户。
 */
export async function fetchMeApi(): Promise<UserInfo> {
  const { data } = await request.get<ApiResult<Record<string, unknown>>>('/auth/me')
  return mapUser(data.data)
}
