import request from './request'

// AI 机器人类型
export interface AIBot {
  id: number
  userId: number
  name: string
  avatar: string | null
  description: string | null
  systemPrompt: string | null
  model: string
  createdAt: string
  updatedAt: string
}

// 创建机器人请求
export interface CreateBotRequest {
  name: string
  avatar?: string
  description?: string
  systemPrompt?: string
  model?: string
}

// 更新机器人请求
export interface UpdateBotRequest {
  name?: string
  avatar?: string
  description?: string
  systemPrompt?: string
  model?: string
}

// 创建机器人
export const createBot = (data: CreateBotRequest): Promise<AIBot> => {
  return request.post('/api/ai/bots', data)
}

// 获取当前用户的机器人列表
export const getMyBots = (): Promise<AIBot[]> => {
  return request.get('/api/ai/bots')
}

// 获取会话中的机器人列表
export const getSessionBots = (sessionId: number): Promise<AIBot[]> => {
  return request.get(`/api/ai/bots/session/${sessionId}`)
}

// 获取机器人详情
export const getBot = (botId: number): Promise<AIBot> => {
  return request.get(`/api/ai/bots/${botId}`)
}

// 更新机器人
export const updateBot = (botId: number, data: UpdateBotRequest): Promise<AIBot> => {
  return request.put(`/api/ai/bots/${botId}`, data)
}

// 删除机器人
export const deleteBot = (botId: number): Promise<void> => {
  return request.delete(`/api/ai/bots/${botId}`)
}

// 添加机器人到会话
export const addBotToSession = (botId: number, sessionId: number): Promise<void> => {
  return request.post(`/api/ai/bots/${botId}/add-to-session`, null, {
    params: { sessionId }
  })
}

// 从会话移除机器人
export const removeBotFromSession = (botId: number, sessionId: number): Promise<void> => {
  return request.delete(`/api/ai/bots/${botId}/remove-from-session`, {
    params: { sessionId }
  })
}
