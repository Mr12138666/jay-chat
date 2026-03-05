import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'

// 创建 axios 实例
// 支持环境变量配置，开发环境使用 localhost，生产环境使用实际服务器地址
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 
                     (import.meta.env.DEV ? 'http://localhost:8080' : window.location.origin.replace(/:\d+$/, ':8080'))

const request: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器：自动添加 JWT token
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('token')
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
    // HTTP 错误（401 未授权等）
    if (error.response?.status === 401) {
      // token 过期或无效，清除本地 token，跳转到登录页
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    } else if (error.code === 'ECONNABORTED' || error.message?.includes('timeout')) {
      console.error('请求超时，请检查后端服务是否启动')
    } else if (error.message?.includes('Network Error')) {
      console.error('网络错误，请检查后端服务是否启动在 http://localhost:8080')
    }
    return Promise.reject(error)
  }
)

export default request
