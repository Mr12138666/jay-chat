/**
 * 错误处理工具函数
 */

/**
 * 处理API错误
 */
export function handleApiError(error: any): string {
  if (error.response) {
    // 服务器返回了错误响应
    const status = error.response.status
    const message = error.response.data?.message || error.message || '请求失败'
    
    if (status === 401) {
      return '登录已过期，请重新登录'
    } else if (status === 403) {
      return '没有权限访问'
    } else if (status === 404) {
      return '资源不存在'
    } else if (status >= 500) {
      return '服务器错误，请稍后重试'
    }
    
    return message
  } else if (error.request) {
    // 请求已发出但没有收到响应
    return '网络错误，请检查网络连接'
  } else {
    // 其他错误
    return error.message || '未知错误'
  }
}

/**
 * 显示错误提示
 */
export function showError(message: string): void {
  // 可以集成消息提示组件，这里先用alert
  console.error(message)
  alert(message)
}
