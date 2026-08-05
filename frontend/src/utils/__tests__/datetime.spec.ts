import { describe, expect, it } from 'vitest'
import { formatDateTime } from '@/utils/datetime'

describe('formatDateTime', () => {
  /**
   * 空值应返回占位符。
   */
  it('returns dash for empty value', () => {
    expect(formatDateTime('')).toBe('-')
    expect(formatDateTime(undefined)).toBe('-')
  })

  /**
   * 可解析时间应输出含日期与时分的本地化字符串。
   */
  it('formats ISO datetime with date and time', () => {
    const result = formatDateTime('2026-08-03T10:00:00')
    expect(result).toMatch(/2026/)
    expect(result).toMatch(/8/)
    expect(result).toMatch(/3/)
    expect(result).toMatch(/10/)
    expect(result).toMatch(/00/)
  })

  /**
   * 无法解析时应回退原始字符串。
   */
  it('returns original string when unparsable', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date')
  })
})
