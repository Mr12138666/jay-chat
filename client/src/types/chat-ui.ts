export interface UiChatMessage {
  id: number
  sender: string
  content: string
  time: string
  // 原始时间戳（ISO）用于恢复后按时间稳定排序
  sentAtRaw?: string
  senderId?: number
  senderAvatar?: string | null
  contentType?: string
  replyToId?: number
  replyToNickname?: string
  replyToContent?: string
  recalled?: boolean
  // 机器人消息相关
  isBotMessage?: boolean
  botId?: number
  requestId?: string
  streaming?: boolean
}
