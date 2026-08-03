/**
 * 将后端时间字段规范为可展示/可回传的字符串。
 * 兼容 ISO 字符串与 Jackson 默认数组格式 `[y,m,d,h,mi,s,nano]`。
 * 字符串原样返回，避免破坏乐观锁 updatedAt 精度。
 *
 * @param value 原始时间
 */
export function normalizeDateTime(value: unknown): string {
  if (value == null || value === '') return ''
  if (typeof value === 'string') return value
  if (Array.isArray(value) && value.length >= 3) {
    const [y, m, d, h = 0, mi = 0, s = 0, nano = 0] = value as number[]
    const pad = (n: number, len = 2) => String(n).padStart(len, '0')
    const millis = Math.floor(Number(nano) / 1_000_000)
    const base = `${y}-${pad(m)}-${pad(d)}T${pad(h)}:${pad(mi)}:${pad(s)}`
    return millis > 0 ? `${base}.${pad(millis, 3)}` : base
  }
  return String(value)
}

/**
 * 将可能为对象的 UUID 规范为字符串。
 *
 * @param value UUID 或字符串
 */
export function normalizeId(value: unknown): string {
  if (value == null) return ''
  if (typeof value === 'string') return value
  return String(value)
}
