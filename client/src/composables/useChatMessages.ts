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

  const showEmojiPicker = ref(false)
  const uploadingImage = ref(false)
  const showImagePreview = ref(false)
  const previewImageUrl = ref<string | null>(null)

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
        senderId: msg.senderId,
        senderAvatar: msg.senderId ? undefined : null,
        contentType: msg.contentType || 'text'
      })).reverse()

      await nextTick()
      scrollToBottom()
      setTimeout(() => scrollToBottom(), 100)

      // Fill avatar cache output lazily with already-fetched avatars.
      await Promise.all(messages.value.map(async (msg) => {
        if (msg.senderId) {
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

    const content = inputMessage.value.trim()
    const tempId = Date.now()
    const userId = currentUser.value?.id || 0

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
      senderId: userId,
      senderAvatar: avatar,
      contentType: 'text'
    })

    // 滚动到底部
    nextTick(() => {
      scrollToBottom(true)
    })

    const success = wsService.sendMessage(currentSessionId.value, content, 'text')

    if (success) {
      inputMessage.value = ''
      showEmojiPicker.value = false
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

    // 如果消息有 ID，检查是否已存在（处理服务器返回的消息）
    if (message.id) {
      // 查找是否有临时消息需要替换（通过 senderId 和 content 匹配）
      const tempIndex = messages.value.findIndex(msg =>
        msg.id && typeof msg.id === 'number' && msg.id > Date.now() - 10000 &&
        msg.senderId === message.senderId && msg.content === (message.contentType === 'image' ? message.content : convertEmojiCodesInText(message.content))
      )

      if (tempIndex !== -1) {
        // 保留原来的头像，只更新 ID 和时间
        const tempMsg = messages.value[tempIndex]!
        messages.value[tempIndex] = {
          ...tempMsg,
          id: message.id,
          time: formatTime(message.sentAt)
        }
        return
      }

      // 如果已有相同 ID 的消息，不重复添加
      if (messages.value.some(msg => msg.id === message.id)) {
        return
      }
    }

    const senderName = message.senderNickname || (message.senderId ? `用户${message.senderId}` : '未知用户')

    let senderAvatar: string | null = null
    if (message.senderId) {
      try {
        senderAvatar = await getUserAvatar(message.senderId)
      } catch {
        senderAvatar = null
      }
    }

    const displayContent = message.contentType === 'image'
      ? message.content
      : convertEmojiCodesInText(message.content)

    messages.value.push({
      id: message.id || Date.now(),
      sender: senderName,
      content: displayContent,
      time: formatTime(message.sentAt),
      senderId: message.senderId,
      senderAvatar,
      contentType: message.contentType || 'text'
    })

    nextTick(() => scrollToBottom(true))
  }

  return {
    messages,
    inputMessage,
    loading,
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
    handleMessage
  }
}

