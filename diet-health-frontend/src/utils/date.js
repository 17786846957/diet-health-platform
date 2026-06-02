/**
 * 格式化日期为 YYYY-MM-DD（避免时区问题）
 */
export function formatDate(date) {
  if (typeof date === 'string' && /^\d{4}-\d{2}-\d{2}/.test(date)) {
    return date.substring(0, 10)
  }
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * 获取本地日期字符串 YYYY-MM-DD
 */
export function getToday() {
  return formatDate(new Date())
}
