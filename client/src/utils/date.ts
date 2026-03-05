/**
 * 日期时间工具函数
 */

/**
 * 格式化时间显示
 * @param dateStr 日期字符串
 * @returns 格式化后的时间字符串
 */
export function formatTime(dateStr?: string): string {
  if (!dateStr) return '刚刚'
  
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (minutes < 1440) return `${Math.floor(minutes / 60)}小时前`
  return date.toLocaleDateString()
}

/**
 * 格式化完整日期时间
 * @param dateStr 日期字符串
 * @returns 格式化后的日期时间字符串
 */
export function formatDateTime(dateStr?: string): string {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
