import request from './request'

// 会话信息
export interface ChatSession {
  id: number
  type: string // single / group
  name: string | null
  ownerId: number | null
  createdAt: string
}

// 消息响应
export interface MessageResponse {
  id: number
  sessionId: number
  senderId: number
  senderNickname?: string
  content: string
  contentType: string
  sentAt: string
}

// 获取或创建默认会话
export const getOrCreateDefaultSession = (): Promise<ChatSession> => {
  return request.post('/api/chat/sessions/default')
}

// 获取用户的会话列表
export const getSessions = (): Promise<ChatSession[]> => {
  return request.get('/api/chat/sessions')
}

// 获取会话消息历史
export const getMessages = (sessionId: number, page: number = 1, pageSize: number = 50): Promise<MessageResponse[]> => {
  return request.get(`/api/chat/sessions/${sessionId}/messages`, {
    params: { page, pageSize }
  })
}
