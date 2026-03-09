export interface UiChatMessage {
  id: number
  sender: string
  content: string
  time: string
  senderId?: number
  senderAvatar?: string | null
  contentType?: string
  replyToId?: number
  replyToNickname?: string
  replyToContent?: string
  recalled?: boolean
}

