import request from './request'

// 会话信息
export interface ChatSession {
  id: number
  type: string // private / group
  name: string | null
  ownerId: number | null
  notice?: string | null
  createdAt: string
}

// 消息响应
export interface MessageResponse {
  id: number
  sessionId: number
  senderId: number
  botId?: number
  senderNickname?: string
  content: string
  contentType: string
  replyToId?: number
  replyToNickname?: string
  replyToContent?: string
  sentAt: string
  recalled?: boolean
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

// 会话成员信息
export interface SessionMember {
  userId: number
  username: string
  nickname: string
  avatar: string | null
  joinedAt: string
}

// 会话成员统计
export interface SessionMemberStats {
  totalMembers: number
  onlineMembers: number
}

// 获取会话成员列表
export const getSessionMembers = (sessionId: number): Promise<SessionMember[]> => {
  return request.get(`/api/chat/sessions/${sessionId}/members`)
}

// 获取会话成员统计
export const getSessionMemberStats = (sessionId: number): Promise<SessionMemberStats> => {
  return request.get(`/api/chat/sessions/${sessionId}/members/stats`)
}

// 获取在线用户ID列表
export const getOnlineUserIds = (sessionId: number): Promise<number[]> => {
  return request.get(`/api/chat/sessions/${sessionId}/members/online`)
}

// 上传聊天图片
export const uploadChatImage = (file: File): Promise<string> => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/chat/images/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

// 用户信息（联系人列表用）
export interface ContactUser {
  userId: number
  username: string
  nickname: string
  avatar: string | null
  createdAt: string
  lastLoginAt: string | null
  lastMessageAt: string | null
}

// 获取用户列表（除当前用户外）
export const getUsers = (keyword?: string): Promise<ContactUser[]> => {
  return request.get('/api/chat/users', {
    params: { keyword }
  })
}

// 创建私人会话
export const createPrivateSession = (targetUserId: number): Promise<ChatSession> => {
  return request.post('/api/chat/sessions/private', null, {
    params: { targetUserId }
  })
}

// 获取私人会话的其他成员信息
export const getPrivateSessionOtherMember = (sessionId: number): Promise<ContactUser | null> => {
  return request.get(`/api/chat/sessions/${sessionId}/other-member`)
}

// 删除会话（退出会话）
export const deleteSession = (sessionId: number): Promise<void> => {
  return request.delete(`/api/chat/sessions/${sessionId}`)
}

// 撤回消息
export const recallMessage = (messageId: number): Promise<MessageResponse> => {
  return request.post(`/api/chat/messages/${messageId}/recall`)
}

// ==================== 群管理 API ====================

// 创建群聊
export interface CreateGroupRequest {
  groupName: string
  memberIds: number[]
}

export const createGroup = (data: CreateGroupRequest): Promise<ChatSession> => {
  return request.post('/api/chat/groups', data)
}

// 获取群信息
export const getGroupInfo = (sessionId: number): Promise<ChatSession> => {
  return request.get(`/api/chat/groups/${sessionId}`)
}

// 更新群信息
export interface UpdateGroupInfoRequest {
  groupName: string
  notice?: string
}

export const updateGroupInfo = (sessionId: number, data: UpdateGroupInfoRequest): Promise<void> => {
  return request.put(`/api/chat/groups/${sessionId}`, data)
}

// 邀请成员
export const inviteMembers = (sessionId: number, userIds: number[]): Promise<void> => {
  return request.post(`/api/chat/groups/${sessionId}/members`, { userIds })
}

// 移除成员
export const removeMember = (sessionId: number, userId: number): Promise<void> => {
  return request.delete(`/api/chat/groups/${sessionId}/members/${userId}`)
}

// 退群
export const leaveGroup = (sessionId: number): Promise<void> => {
  return request.post(`/api/chat/groups/${sessionId}/leave`)
}

// 解散群聊
export const dissolveGroup = (sessionId: number): Promise<void> => {
  return request.delete(`/api/chat/groups/${sessionId}`)
}

// 转让群主
export const transferOwner = (sessionId: number, newOwnerId: number): Promise<void> => {
  return request.post(`/api/chat/groups/${sessionId}/transfer`, null, {
    params: { newOwnerId }
  })
}

// 检查是否是群主
export const isGroupOwner = (sessionId: number): Promise<boolean> => {
  return request.get(`/api/chat/groups/${sessionId}/is-owner`)
}