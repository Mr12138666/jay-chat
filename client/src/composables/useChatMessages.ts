import { nextTick, ref, type Ref } from 'vue'
import { getMessages, uploadChatImage } from '../api/chat'
import { wsService, type ChatMessage } from '../utils/websocket'
import { convertEmojiCodesInText } from '../utils/emoji'
import { formatTime } from '../utils/date'
import type { UiChatMessage } from '../types/chat-ui'

interface UseChatMessagesOptions {
  currentSessionId: Ref<number | null>
  currentUser: Ref<{ id: number; nickname: string; avatar: string | null } | null>
  getUserAvatar: (userId: number) => Promise<string | null>
  scrollToBottom: (smooth?: boolean) => void
  showError: (message: string) => void
  handleApiError: (error: unknown) => string
  onMessageReceived?: (message: ChatMessage) => void
}

export const useChatMessages = ({
  currentSessionId,
  currentUser,
  getUserAvatar,
  scrollToBottom,
  showError,
  handleApiError,
  onMessageReceived
}: UseChatMessagesOptions) => {
  const messages = ref<UiChatMessage[]>([])
  const inputMessage = ref('')
  const loading = ref(false)

  // 引用的消息
  const replyingTo = ref<{ id: number; sender: string; content: string; isBotMessage?: boolean; botId?: number } | null>(null)

  const showEmojiPicker = ref(false)
  const uploadingImage = ref(false)
  const showImagePreview = ref(false)
  const previewImageUrl = ref<string | null>(null)

  // 设置引用消息
  const setReplyingTo = (message: { id: number; sender: string; content: string; isBotMessage?: boolean; botId?: number } | null) => {
    replyingTo.value = message
  }

  // 取消引用
  const cancelReply = () => {
    replyingTo.value = null
  }

  const loadMessages = async (sessionId: number) => {
    try {
      loading.value = true
      const history = await getMessages(sessionId, 1, 50)

      const senderIds = history.map(msg => msg.senderId).filter(id => id !== undefined) as number[]
      await Promise.all([...new Set(senderIds)].map(id => getUserAvatar(id)))

      messages.value = history.map(msg => ({
        id: msg.id,
        sender: msg.senderNickname || `用户${msg.senderId}`,
        content: msg.contentType === 'image' ? msg.content : convertEmojiCodesInText(msg.content),
        time: formatTime(msg.sentAt),
        sentAtRaw: msg.sentAt,
        senderId: msg.senderId,
        senderAvatar: msg.botId ? null : (msg.senderId ? undefined : null),
        contentType: msg.contentType || 'text',
        replyToId: msg.replyToId,
        replyToNickname: msg.replyToNickname,
        replyToContent: msg.replyToContent,
        recalled: msg.recalled,
        isBotMessage: !!msg.botId,
        botId: msg.botId
      })).reverse()

      await nextTick()
      scrollToBottom()

      // Fill avatar cache output lazily with already-fetched avatars.
      await Promise.all(messages.value.map(async (msg) => {
        if (msg.senderId && !msg.isBotMessage) {
          msg.senderAvatar = await getUserAvatar(msg.senderId)
        }
      }))
    } catch (error) {
      console.error('加载消息失败:', error)
    } finally {
      loading.value = false
    }
  }

  const sendMessage = async () => {
    if (!inputMessage.value.trim() || !currentSessionId.value) return

    const rawContent = inputMessage.value.trim()
    let content = rawContent
    const tempId = Date.now()
    const userId = currentUser.value?.id || 0

    // 引用机器人消息时，若未显式@机器人，则自动补齐，保证能触发 AI 回复
    if (replyingTo.value?.isBotMessage) {
      const mention = `@${replyingTo.value.sender}`
      if (!rawContent.startsWith(mention) && !rawContent.includes(`${mention} `)) {
        content = `${mention} ${rawContent}`
      }
    }

    // 保存引用消息信息
    const replyInfo = replyingTo.value ? {
      replyToId: replyingTo.value.id,
      replyToNickname: replyingTo.value.sender,
      replyToContent: replyingTo.value.content
    } : {}

    // 获取当前用户头像
    let avatar: string | null = null
    if (userId) {
      try {
        avatar = await getUserAvatar(userId)
      } catch {
        avatar = null
      }
    }

    // 立即显示消息到消息列表（使用临时ID）
    messages.value.push({
      id: tempId,
      sender: currentUser.value?.nickname || '我',
      content: convertEmojiCodesInText(content),
      time: formatTime(new Date().toISOString()),
      sentAtRaw: new Date().toISOString(),
      senderId: userId,
      senderAvatar: avatar,
      contentType: 'text',
      ...replyInfo
    })

    // 滚动到底部（强制滚动，确保看到自己发送的消息）
    nextTick(() => {
      scrollToBottom(true)
    })

    const success = wsService.sendMessage(currentSessionId.value, content, 'text', replyingTo.value?.id)

    if (success) {
      inputMessage.value = ''
      showEmojiPicker.value = false
      // 清除引用
      cancelReply()
    } else {
      console.error('发送消息失败，WebSocket 未连接')
    }
  }

  const toggleEmojiPicker = () => {
    showEmojiPicker.value = !showEmojiPicker.value
  }

  const closeEmojiPicker = () => {
    showEmojiPicker.value = false
  }

  const selectEmoji = (code: string) => {
    const input = inputMessage.value
    const cursorPos = (document.querySelector('.message-input') as HTMLInputElement)?.selectionStart || input.length

    inputMessage.value = input.slice(0, cursorPos) + code + input.slice(cursorPos)
    showEmojiPicker.value = false

    nextTick(() => {
      const inputEl = document.querySelector('.message-input') as HTMLInputElement
      if (inputEl) {
        inputEl.focus()
        const newPos = cursorPos + code.length
        inputEl.setSelectionRange(newPos, newPos)
      }
    })
  }

  const handleImageFileChange = async (file: File) => {
    if (!file.type.startsWith('image/')) {
      showError('请选择图片文件')
      return
    }

    if (file.size > 10 * 1024 * 1024) {
      showError('图片大小不能超过10MB')
      return
    }

    if (!currentSessionId.value) {
      showError('请先选择会话')
      return
    }

    try {
      uploadingImage.value = true
      const imageUrl = await uploadChatImage(file)
      const success = wsService.sendMessage(currentSessionId.value, imageUrl, 'image')
      if (!success) {
        showError('发送图片失败，WebSocket 未连接')
      }
    } catch (error) {
      handleApiError(error)
      showError('上传图片失败')
    } finally {
      uploadingImage.value = false
    }
  }

  const openImagePreview = (imageUrl: string) => {
    previewImageUrl.value = imageUrl
    showImagePreview.value = true
  }

  const closeImagePreview = () => {
    showImagePreview.value = false
    previewImageUrl.value = null
  }

  const handleMessage = async (message: ChatMessage) => {
    // 如果不是当前会话的消息，调用未读回调
    if (message.sessionId !== currentSessionId.value) {
      if (onMessageReceived) {
        onMessageReceived(message)
      }
      return
    }

    const isBotMessage = !!message.botId
    const senderName = message.senderNickname ||
      (isBotMessage ? `AI 助手${message.botId}` : (message.senderId ? `用户${message.senderId}` : '未知用户'))
    const displayContent = message.contentType === 'image'
      ? message.content
      : convertEmojiCodesInText(message.content)

    // 先按最终ID去重
    if (message.id && messages.value.some(msg => msg.id === message.id)) {
      return
    }

    // 流式 bot 最终消息：优先替换最近的 bot 气泡，避免出现两条回复
    if (isBotMessage && message.id) {
      const finalTs = message.sentAt ? Date.parse(message.sentAt) : NaN

      for (let i = messages.value.length - 1; i >= 0; i--) {
        const msg = messages.value[i]
        if (!msg?.isBotMessage || msg.botId !== message.botId) {
          continue
        }

        const msgTs = msg.sentAtRaw ? Date.parse(msg.sentAtRaw) : NaN
        const closeTime = !Number.isNaN(finalTs) && !Number.isNaN(msgTs)
          ? Math.abs(finalTs - msgTs) <= 120000
          : false
        const sameContent = (msg.content || '').trim() === (displayContent || '').trim()

        if (msg.streaming || (sameContent && closeTime)) {
          messages.value[i] = {
            ...msg,
            id: message.id,
            sender: senderName,
            content: displayContent,
            time: formatTime(message.sentAt),
            sentAtRaw: message.sentAt,
            senderId: message.senderId,
            senderAvatar: null,
            contentType: message.contentType || 'text',
            replyToId: message.replyToId,
            replyToNickname: message.replyToNickname,
            replyToContent: message.replyToContent,
            recalled: message.recalled,
            isBotMessage: true,
            botId: message.botId,
            streaming: false
          }

          nextTick(() => scrollToBottom(true))
          return
        }
      }
    }

    // 如果消息有 ID，检查是否有本地临时消息需要替换（仅用户消息）
    if (message.id && !isBotMessage) {
      const tempIndex = messages.value.findIndex(msg =>
        msg.id && typeof msg.id === 'number' && msg.id > Date.now() - 10000 &&
        msg.senderId === message.senderId &&
        msg.content === (message.contentType === 'image' ? message.content : convertEmojiCodesInText(message.content))
      )

      if (tempIndex !== -1) {
        const tempMsg = messages.value[tempIndex]!
        messages.value[tempIndex] = {
          ...tempMsg,
          id: message.id,
          time: formatTime(message.sentAt),
          sentAtRaw: message.sentAt
        }
        return
      }
    }

    let senderAvatar: string | null = null
    if (message.senderId && !isBotMessage) {
      try {
        senderAvatar = await getUserAvatar(message.senderId)
      } catch {
        senderAvatar = null
      }
    }


    messages.value.push({
      id: message.id || Date.now(),
      sender: senderName,
      content: displayContent,
      time: formatTime(message.sentAt),
      sentAtRaw: message.sentAt,
      senderId: message.senderId,
      senderAvatar,
      contentType: message.contentType || 'text',
      replyToId: message.replyToId,
      replyToNickname: message.replyToNickname,
      replyToContent: message.replyToContent,
      recalled: message.recalled,
      isBotMessage,
      botId: message.botId
    })

    nextTick(() => scrollToBottom(true))
  }

  // 处理撤回消息
  const handleRecallMessage = (message: ChatMessage) => {
    const msgIndex = messages.value.findIndex(msg => msg.id === message.id)
    if (msgIndex !== -1 && messages.value[msgIndex]) {
      messages.value[msgIndex]!.recalled = true
    }
  }

  return {
    messages,
    inputMessage,
    loading,
    replyingTo,
    setReplyingTo,
    cancelReply,
    showEmojiPicker,
    uploadingImage,
    showImagePreview,
    previewImageUrl,
    loadMessages,
    sendMessage,
    toggleEmojiPicker,
    closeEmojiPicker,
    selectEmoji,
    handleImageFileChange,
    openImagePreview,
    closeImagePreview,
    handleMessage,
    handleRecallMessage
  }
}

