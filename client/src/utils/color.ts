/**
 * 颜色工具类
 * 根据用户ID生成稳定的颜色
 */

/**
 * 预定义的颜色列表（用于不同用户的消息框）
 * 使用柔和的颜色，确保在深色背景下可读
 */
const USER_COLORS = [
  '#4a90e2', // 蓝色
  '#50c878', // 绿色
  '#ff6b6b', // 红色
  '#ffa500', // 橙色
  '#9b59b6', // 紫色
  '#1abc9c', // 青色
  '#e74c3c', // 深红色
  '#3498db', // 亮蓝色
  '#2ecc71', // 亮绿色
  '#f39c12', // 金色
  '#e67e22', // 深橙色
  '#16a085', // 深青色
  '#27ae60', // 深绿色
  '#8e44ad', // 深紫色
  '#c0392b', // 暗红色
  '#2980b9', // 暗蓝色
  '#d35400', // 暗橙色
  '#7f8c8d', // 灰色
  '#34495e', // 深灰色
  '#95a5a6', // 浅灰色
]

/**
 * 根据用户ID生成颜色
 * 使用哈希算法确保同一用户总是得到相同的颜色
 */
export function getUserColor(userId: number | undefined): string {
  if (!userId) {
    return USER_COLORS[0] // 默认颜色
  }
  
  // 使用简单的哈希算法
  const index = userId % USER_COLORS.length
  return USER_COLORS[Math.abs(index)]
}

/**
 * 根据颜色计算文字颜色（确保可读性）
 */
export function getTextColor(backgroundColor: string): string {
  // 对于深色背景，使用白色文字
  // 对于浅色背景，使用黑色文字
  // 这里我们主要使用深色背景，所以返回白色
  return '#ffffff'
}

/**
 * 生成颜色的渐变版本（用于消息框背景）
 * 确保在深色背景下有足够的对比度和可读性
 */
export function getMessageBackgroundColor(userId: number | undefined): string {
  const baseColor = getUserColor(userId)
  
  // 将颜色转换为RGB
  const hex = baseColor.replace('#', '')
  const r = parseInt(hex.substr(0, 2), 16)
  const g = parseInt(hex.substr(2, 2), 16)
  const b = parseInt(hex.substr(4, 2), 16)
  
  // 创建稍微暗一点但保持饱和度的版本（用于深色主题）
  // 使用乘法而不是减法，保持颜色饱和度
  const factor = 0.75 // 稍微暗一点
  const darkR = Math.floor(r * factor)
  const darkG = Math.floor(g * factor)
  const darkB = Math.floor(b * factor)
  
  // 确保颜色不会太暗（最小亮度）
  const minBrightness = 60
  const brightness = (darkR + darkG + darkB) / 3
  if (brightness < minBrightness) {
    const adjust = (minBrightness - brightness) / 3
    return `rgb(${Math.min(255, Math.floor(darkR + adjust))}, ${Math.min(255, Math.floor(darkG + adjust))}, ${Math.min(255, Math.floor(darkB + adjust))})`
  }
  
  return `rgb(${darkR}, ${darkG}, ${darkB})`
}
