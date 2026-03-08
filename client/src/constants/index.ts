/**
 * 前端常量定义
 * 统一管理会话类型、API路径等常量
 */

// ========== 会话类型常量 ==========
export const SessionType = {
  /** 群组会话 */
  GROUP: 'group',
  /** 私聊会话 */
  PRIVATE: 'private'
} as const

export type SessionTypeValue = typeof SessionType[keyof typeof SessionType]

// ========== API 路径常量 ==========
export const ApiPath = {
  // 认证相关
  AUTH_LOGIN: '/api/auth/login',
  AUTH_REGISTER: '/api/auth/register',

  // 聊天相关
  CHAT_SESSIONS_DEFAULT: '/api/chat/sessions/default',
  CHAT_SESSIONS: '/api/chat/sessions',
  CHAT_MESSAGES: (sessionId: number) => `/api/chat/sessions/${sessionId}/messages`,
  CHAT_MEMBERS: (sessionId: number) => `/api/chat/sessions/${sessionId}/members`,
  CHAT_MEMBERS_STATS: (sessionId: number) => `/api/chat/sessions/${sessionId}/members/stats`,
  CHAT_MEMBERS_ONLINE: (sessionId: number) => `/api/chat/sessions/${sessionId}/members/online`,
  CHAT_OTHER_MEMBER: (sessionId: number) => `/api/chat/sessions/${sessionId}/other-member`,
  CHAT_USERS: '/api/chat/users',
  CHAT_PRIVATE_SESSION: '/api/chat/sessions/private',
  CHAT_IMAGE_UPLOAD: '/api/chat/images/upload'
} as const
