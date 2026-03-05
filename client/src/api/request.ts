import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { getToken, clearAuth } from '../utils/storage'

// 创建 axios 实例
// 开发环境：优先使用 VITE_API_BASE_URL，未配置时使用本地后端 http://localhost:8080
// 生产环境：使用 VITE_API_BASE_URL 或当前站点同源地址
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 
  (import.meta.env.DEV ? 'http://localhost:8080' : window.location.origin)

// 调试日志：输出 API 基础地址
console.log('API_BASE_URL:', API_BASE_URL)
console.log('VITE_API_BASE_URL:', import.meta.env.VITE_API_BASE_URL)
console.log('import.meta.env.DEV:', import.meta.env.DEV)

const request: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：自动添加JWT token
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = getToken()
    if (token && config.headers) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  (response: AxiosResponse) => {
    // 后端返回格式：{ code, message, data }
    const res = response.data
    if (res.code === 0) {
      return res.data
    } else {
      // 业务错误
      return Promise.reject(new Error(res.message || '请求失败'))
    }
  },
  (error: any) => {
    // HTTP错误（401未授权等）
    if (error.response?.status === 401) {
      // token过期或无效，清除本地认证信息，跳转到登录页
      clearAuth()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      console.error('请求超时，请检查后端服务是否启动')
    } else if (error.message?.includes('Network Error')) {
      console.error('网络错误，请检查后端服务是否启动')
    }
    return Promise.reject(error)
  }
)

export default request
