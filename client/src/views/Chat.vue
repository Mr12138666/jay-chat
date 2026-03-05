<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import type { UserInfo } from '../api/auth'
import { getSessions, getMessages, getOrCreateDefaultSession, type ChatSession } from '../api/chat'
import { wsService, type ChatMessage } from '../utils/websocket'

const router = useRouter()

// 当前用户信息
const currentUser = ref<UserInfo | null>(null)

// 会话列表
const sessions = ref<ChatSession[]>([])
const currentSessionId = ref<number | null>(null)

// 消息列表
const messages = ref<Array<{
  id: number
  sender: string
  content: string
  time: string
  senderId?: number
}>>([])

// 当前输入的消息
const inputMessage = ref('')
const loading = ref(false)

// WebSocket 连接状态（响应式）
const wsConnected = ref(false)

// 格式化时间
const formatTime = (dateStr?: string) => {
  if (!dateStr) return '刚刚'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const minutes = Math.floor(diff / 60000)
  
  if (minutes < 1) return '刚刚'
  if (minutes < 60) return `${minutes}分钟前`
  if (minutes < 1440) return `${Math.floor(minutes / 60)}小时前`
  return date.toLocaleDateString()
}

// 加载会话列表
const loadSessions = async () => {
  try {
    console.log('开始加载会话列表...')
    
    // 先确保用户加入全局的"公共聊天室"
    console.log('确保加入全局公共聊天室...')
    const defaultSession = await getOrCreateDefaultSession()
    console.log('默认会话（公共聊天室）:', defaultSession)
    
    // 然后获取用户的所有会话列表
    sessions.value = await getSessions()
    console.log('获取到的会话列表:', sessions.value)
    
    // 确保"公共聊天室"在列表中（如果不在，添加到列表开头）
    const hasDefaultSession = sessions.value.some(s => s.id === defaultSession.id)
    if (!hasDefaultSession) {
      sessions.value.unshift(defaultSession)
    }
    
    // 优先选择"公共聊天室"作为当前会话
    if (!currentSessionId.value) {
      currentSessionId.value = defaultSession.id
      // 更新 WebSocket 服务的当前会话ID
      wsService.setCurrentSessionId(defaultSession.id)
      await loadMessages(defaultSession.id)
      // 订阅公共聊天室的消息（如果 WebSocket 已连接）
      if (wsConnected.value || wsService.isConnected()) {
        wsService.subscribeToSession(defaultSession.id, handleMessage)
        console.log('已选择公共聊天室，会话ID:', defaultSession.id, '已订阅')
      } else {
        console.log('已选择公共聊天室，会话ID:', defaultSession.id, '等待 WebSocket 连接后自动订阅')
      }
    }
  } catch (error: any) {
    console.error('加载会话列表失败:', error)
    // 如果获取会话列表失败，尝试直接创建默认会话
    if (error.message?.includes('401') || error.message?.includes('未登录')) {
      alert('登录已过期，请重新登录')
      router.push('/login')
    } else {
      console.log('尝试直接创建默认会话...')
      try {
        const defaultSession = await getOrCreateDefaultSession()
        sessions.value = [defaultSession]
        currentSessionId.value = defaultSession.id
        wsService.setCurrentSessionId(defaultSession.id)
        await loadMessages(defaultSession.id)
        if (wsConnected.value || wsService.isConnected()) {
          wsService.subscribeToSession(defaultSession.id, handleMessage)
        }
      } catch (createError) {
        console.error('创建默认会话也失败:', createError)
        alert('无法加载会话，请检查网络连接或刷新页面')
      }
    }
  }
}

// 加载消息历史
const loadMessages = async (sessionId: number) => {
  try {
    loading.value = true
    const history = await getMessages(sessionId, 1, 50)
    messages.value = history.map(msg => ({
      id: msg.id,
      sender: msg.senderNickname || `用户${msg.senderId}`,
      content: msg.content,
      time: formatTime(msg.sentAt),
      senderId: msg.senderId
    })).reverse() // 反转，最新的在底部
    
    // 滚动到底部
    await nextTick()
    scrollToBottom()
  } catch (error) {
    console.error('加载消息失败:', error)
  } finally {
    loading.value = false
  }
}

  // 切换会话
const switchSession = async (sessionId: number) => {
  if (currentSessionId.value === sessionId) return
  currentSessionId.value = sessionId
  wsService.setCurrentSessionId(sessionId)
  await loadMessages(sessionId)
  // 订阅新会话的消息
  if (wsConnected.value || wsService.isConnected()) {
    wsService.subscribeToSession(sessionId, handleMessage)
  }
}

// 发送消息
const sendMessage = () => {
  if (!inputMessage.value.trim() || !currentSessionId.value) return
  
  const success = wsService.sendMessage(
    currentSessionId.value,
    inputMessage.value.trim(),
    'text'
  )
  
  if (success) {
    inputMessage.value = ''
  } else {
    console.error('发送消息失败，WebSocket 未连接')
  }
}

// 处理接收到的消息
const handleMessage = (message: ChatMessage) => {
  console.log('handleMessage 被调用:', {
    messageSessionId: message.sessionId,
    currentSessionId: currentSessionId.value,
    message: message
  })
  
  // 只处理当前会话的消息
  if (message.sessionId === currentSessionId.value) {
    // 检查消息是否已存在（避免重复显示）
    // 使用消息ID来判断，如果ID相同则认为是重复消息
    if (message.id) {
      const exists = messages.value.some(msg => msg.id === message.id)
      if (exists) {
        console.log('消息已存在，跳过:', message.id, message.content)
        return
      }
    }
    
    // 添加新消息
    // 确保使用正确的发送者昵称，如果为空则使用用户名或用户ID
    const senderName = message.senderNickname || 
                      (message.senderId ? `用户${message.senderId}` : '未知用户')
    
    messages.value.push({
      id: message.id || Date.now(),
      sender: senderName,
      content: message.content,
      time: formatTime(message.sentAt),
      senderId: message.senderId
    })
    
    console.log('添加消息到列表:', {
      id: message.id,
      senderId: message.senderId,
      senderNickname: message.senderNickname,
      sender: senderName,
      content: message.content
    })
    
    nextTick(() => {
      scrollToBottom()
    })
  } else {
    console.warn('消息会话ID不匹配，跳过:', {
      messageSessionId: message.sessionId,
      currentSessionId: currentSessionId.value
    })
  }
}

// 滚动到底部
const scrollToBottom = () => {
  const messagesEl = document.querySelector('.chat-messages')
  if (messagesEl) {
    messagesEl.scrollTop = messagesEl.scrollHeight
  }
}

// 退出登录
const handleLogout = () => {
  wsService.disconnect()
  localStorage.removeItem('token')
  localStorage.removeItem('user')
  router.push('/login')
}

onMounted(async () => {
  // 从 localStorage 读取用户信息
  const userStr = localStorage.getItem('user')
  const token = localStorage.getItem('token')
  
  if (!userStr || !token) {
    router.push('/login')
    return
  }
  
  // 解析并更新当前用户信息
  const user = JSON.parse(userStr)
  currentUser.value = user
  console.log('当前登录用户:', user)
  
  // 确保 WebSocket 断开旧连接（如果有）
  wsService.disconnect()
  
  // 先加载会话列表（这样可以在 WebSocket 连接成功后立即订阅）
  await loadSessions()
  
  // 设置连接状态变更回调
  wsService.onConnectionStateChange((connected) => {
    wsConnected.value = connected
    console.log('WebSocket 连接状态变更:', connected)
  })
  
  // 使用新的 token 连接 WebSocket
  // 注意：connect 的回调中会自动订阅 currentSessionId（如果已设置）
  wsService.connect(token, handleMessage, (error) => {
    console.error('WebSocket 连接错误:', error)
    wsConnected.value = false
    // 连接失败时，可以显示错误提示
    alert('WebSocket 连接失败，请检查网络或刷新页面重试')
  })
  
  // 初始化连接状态
  wsConnected.value = wsService.isConnected()
  
  // 等待一小段时间让连接建立，然后检查状态
  setTimeout(() => {
    if (currentSessionId.value) {
      if (wsConnected.value || wsService.isConnected()) {
        console.log('WebSocket 已连接，立即订阅会话:', currentSessionId.value)
        wsService.subscribeToSession(currentSessionId.value, handleMessage)
      } else {
        console.log('WebSocket 未连接，等待连接成功后自动订阅会话:', currentSessionId.value)
        // 如果 3 秒后还没连接，再次尝试订阅
        setTimeout(() => {
          if (currentSessionId.value && (wsConnected.value || wsService.isConnected())) {
            console.log('延迟订阅会话:', currentSessionId.value)
            wsService.subscribeToSession(currentSessionId.value, handleMessage)
          }
        }, 3000)
      }
    }
  }, 500)
})

onUnmounted(() => {
  // 断开 WebSocket 连接
  wsService.disconnect()
})
</script>

<template>
  <div class="app">
    <aside class="sidebar">
      <header class="sidebar-header">
        <h1>jay-chat</h1>
        <p class="subtitle">大型聊天室</p>
      </header>
      
      <div class="user-info">
        <div class="user-avatar">{{ currentUser?.nickname?.[0] || 'U' }}</div>
        <div class="user-details">
          <div class="user-name">{{ currentUser?.nickname || '未知用户' }}</div>
          <div class="user-id">@{{ currentUser?.username }}</div>
        </div>
        <button @click="handleLogout" class="logout-btn">退出</button>
      </div>

      <section class="room-list">
        <h2>会话列表</h2>
        <ul v-if="sessions.length > 0">
          <li 
            v-for="session in sessions" 
            :key="session.id" 
            class="room-item"
            :class="{ active: currentSessionId === session.id }"
            @click="switchSession(session.id)"
          >
            {{ session.name || (session.type === 'single' ? '单聊' : '群聊') }}
          </li>
        </ul>
        <p v-else class="empty-sessions">暂无会话</p>
      </section>
    </aside>

    <main class="chat">
      <header class="chat-header">
        <div>
          <h2>{{ currentSessionId ? (sessions.find(s => s.id === currentSessionId)?.name || '聊天') : '请选择会话' }}</h2>
          <p class="chat-subtitle">{{ wsConnected ? '已连接' : '连接中...' }}</p>
        </div>
      </header>

      <section class="chat-messages" v-if="currentSessionId">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="messages.length === 0" class="empty-messages">暂无消息</div>
        <div v-else>
          <div 
            v-for="msg in messages" 
            :key="msg.id" 
            class="message"
            :class="{ 'own-message': msg.senderId === currentUser?.id }"
          >
            <div class="message-meta">
              <span class="sender">{{ msg.sender }}</span>
              <span class="time">{{ msg.time }}</span>
            </div>
            <div class="message-content">
              {{ msg.content }}
            </div>
          </div>
        </div>
      </section>
      <section v-else class="chat-messages empty-state">
        <p>请从左侧选择一个会话开始聊天</p>
      </section>

      <footer class="chat-input" v-if="currentSessionId">
        <input
          v-model="inputMessage"
          @keyup.enter="sendMessage"
          type="text"
          placeholder="输入消息..."
          class="message-input"
          :disabled="!wsConnected"
        />
        <button 
          @click="sendMessage" 
          class="send-btn"
          :disabled="!wsConnected || !inputMessage.trim()"
        >
          发送
        </button>
      </footer>
    </main>
  </div>
</template>

<style scoped>
.app {
  display: flex;
  height: 100vh;
  background: #1a1a1a;
  color: #e0e0e0;
}

.sidebar {
  width: 280px;
  background: #252525;
  border-right: 1px solid #333;
  display: flex;
  flex-direction: column;
}

.sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #333;
}

.sidebar-header h1 {
  margin: 0 0 4px 0;
  font-size: 24px;
  color: #fff;
}

.subtitle {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.user-info {
  padding: 16px 20px;
  border-bottom: 1px solid #333;
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-id {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logout-btn {
  padding: 6px 12px;
  background: #444;
  color: #e0e0e0;
  border: none;
  border-radius: 4px;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.logout-btn:hover {
  background: #555;
}

.room-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
}

.room-list h2 {
  padding: 0 20px 12px;
  margin: 0;
  font-size: 14px;
  color: #999;
  text-transform: uppercase;
  font-weight: 500;
}

.room-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.room-item {
  padding: 12px 20px;
  cursor: pointer;
  transition: background 0.2s;
}

.room-item:hover {
  background: #2a2a2a;
}

.room-item.active {
  background: #333;
  border-left: 3px solid #667eea;
}

.empty-sessions {
  padding: 20px;
  text-align: center;
  color: #666;
  font-size: 14px;
}

.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #1a1a1a;
}

.chat-header {
  padding: 20px;
  border-bottom: 1px solid #333;
  background: #252525;
}

.chat-header h2 {
  margin: 0 0 4px 0;
  font-size: 20px;
  color: #fff;
}

.chat-subtitle {
  margin: 0;
  font-size: 12px;
  color: #999;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-meta {
  display: flex;
  gap: 8px;
  align-items: center;
}

.sender {
  font-weight: 500;
  color: #667eea;
  font-size: 14px;
}

.time {
  font-size: 12px;
  color: #666;
}

.message-content {
  padding: 8px 12px;
  background: #2a2a2a;
  border-radius: 6px;
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
}

.message.own-message {
  align-items: flex-end;
}

.message.own-message .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.loading, .empty-messages, .empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
  font-size: 14px;
}

.message-input:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #333;
  background: #252525;
  display: flex;
  gap: 12px;
}

.message-input {
  flex: 1;
  padding: 12px;
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 6px;
  color: #e0e0e0;
  font-size: 14px;
}

.message-input:focus {
  outline: none;
  border-color: #667eea;
}

.send-btn {
  padding: 12px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.send-btn:hover {
  opacity: 0.9;
}
</style>
