import request from './request'

// 登录请求参数
export interface LoginRequest {
  username: string
  password: string
}

// 注册请求参数
export interface RegisterRequest {
  username: string
  password: string
  nickname: string
}

// 用户信息
export interface UserInfo {
  id: number
  username: string
  nickname: string
  avatar: string | null
  createdAt: string
  updatedAt: string
}

// 登录响应
export interface LoginResponse {
  token: string
  user: UserInfo
}

// 登录接口
export const login = (data: LoginRequest): Promise<LoginResponse> => {
  return request.post('/api/auth/login', data)
}

// 注册接口
export const register = (data: RegisterRequest): Promise<void> => {
  return request.post('/api/auth/register', data)
}

// 获取当前用户信息（需要 token）
export const getCurrentUser = (): Promise<UserInfo> => {
  return request.get('/api/auth/me')
}
