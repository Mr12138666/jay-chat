<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { UserInfo, UserDetail } from '../api/auth'
import { updateNickname, getUserDetail, uploadAvatar } from '../api/auth'
import {
  getSessions,
  getOrCreateDefaultSession,
  getSessionMembers,
  getSessionMemberStats,
  getOnlineUserIds,
  type ChatSession,
  type SessionMember,
  type SessionMemberStats
} from '../api/chat'
import { wsService } from '../utils/websocket'
import { formatTime } from '../utils/date'
import { getUser, getToken, clearAuth, setUser } from '../utils/storage'
import { handleApiError, showError, showSuccess } from '../utils/error'
import MembersModal from '../components/chat/MembersModal.vue'
import ChatMessageList from '../components/chat/ChatMessageList.vue'
import ChatComposer from '../components/chat/ChatComposer.vue'
import { validateAvatarFile, withCacheBuster } from '../utils/avatar'
import { useChatMessages } from '../composables/useChatMessages'

const router = useRouter()

const currentUser = ref<UserInfo | null>(null)
const sessions = ref<ChatSession[]>([])
const currentSessionId = ref<number | null>(null)
const wsConnected = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const showSidebar = ref(false)

const showUserDetail = ref(false)
const viewingUserId = ref<number | null>(null)
const userDetail = ref<UserDetail | null>(null)
const loadingUserDetail = ref(false)
const showEditNickname = ref(false)
const newNickname = ref('')
const updatingNickname = ref(false)
const uploadingAvatar = ref(false)
const avatarFileInput = ref<HTMLInputElement | null>(null)

const showMembersList = ref(false)
const members = ref<SessionMember[]>([])
const memberStats = ref<SessionMemberStats | null>(null)
const onlineUserIds = ref<Set<number>>(new Set())
const loadingMembers = ref(false)

const userAvatarCache = ref<Map<number, string | null>>(new Map())

const scrollToBottom = (smooth = false) => {
  if (!messagesContainer.value) return
  if (smooth) {
    messagesContainer.value.scrollTo({
      top: messagesContainer.value.scrollHeight,
      behavior: 'smooth'
    })
  } else {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

const getUserAvatar = async (userId: number): Promise<string | null> => {
  if (userAvatarCache.value.has(userId)) {
    return userAvatarCache.value.get(userId) || null
  }

  try {
    const detail = await getUserDetail(userId)
    const avatar = detail.avatar
    userAvatarCache.value.set(userId, avatar)
    return avatar
  } catch {
    userAvatarCache.value.set(userId, null)
    return null
  }
}

const {
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
} = useChatMessages({
  currentSessionId,
  getUserAvatar,
  scrollToBottom,
  showError,
  handleApiError
})

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
    const errorMessage = handleApiError(error)
    
    // 如果获取会话列表失败，尝试直接创建默认会话
    if (errorMessage.includes('登录已过期') || errorMessage.includes('未登录')) {
      showError('登录已过期，请重新登录')
      clearAuth()
      router.push('/login')
      return
    }
    
    // 尝试直接创建默认会话
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
      showError('无法加载会话，请检查网络连接或刷新页面')
    }
  }
}

const switchSession = async (sessionId: number) => {
  if (currentSessionId.value === sessionId) return

  // 停止旧会话的统计刷新
  stopMemberStatsRefresh()

  currentSessionId.value = sessionId
  wsService.setCurrentSessionId(sessionId)

  // 移动端切换会话后关闭侧边栏
  if (window.innerWidth <= 768) {
    showSidebar.value = false
  }

  await loadMessages(sessionId)
  // 订阅新会话的消息
  if (wsConnected.value || wsService.isConnected()) {
    wsService.subscribeToSession(sessionId, handleMessage)
  }

  // 加载新会话的成员统计
  await loadMembersData()
  startMemberStatsRefresh()
}

const toggleSidebar = () => {
  showSidebar.value = !showSidebar.value
}

const handleLogout = () => {
  wsService.disconnect()
  clearAuth()
  router.push('/login')
}

// 打开用户详情弹窗
const openUserDetail = async (userId: number) => {
  viewingUserId.value = userId
  showUserDetail.value = true
  loadingUserDetail.value = true
  
  try {
    userDetail.value = await getUserDetail(userId)
    // 更新头像缓存
    if (userDetail.value.avatar !== null) {
      userAvatarCache.value.set(userId, userDetail.value.avatar)
    }
  } catch (error: any) {
    const errorMessage = handleApiError(error)
    showError(errorMessage)
    closeUserDetail()
  } finally {
    loadingUserDetail.value = false
  }
}

// 关闭用户详情弹窗
const closeUserDetail = () => {
  showUserDetail.value = false
  viewingUserId.value = null
  userDetail.value = null
  showEditNickname.value = false
}

// 打开修改昵称弹窗（在详情弹窗内）
const openEditNickname = () => {
  if (!userDetail.value) return
  newNickname.value = userDetail.value.nickname || ''
  showEditNickname.value = true
}

// 关闭修改昵称弹窗
const closeEditNickname = () => {
  showEditNickname.value = false
  newNickname.value = ''
}

// 保存昵称
const saveNickname = async () => {
  const nickname = newNickname.value.trim()
  
  if (!nickname) {
    showError('昵称不能为空')
    return
  }
  
  if (!viewingUserId.value) {
    showError('用户ID不存在')
    return
  }
  
  // 只能修改自己的昵称
  if (viewingUserId.value !== currentUser.value?.id) {
    showError('只能修改自己的昵称')
    return
  }
  
  if (nickname === userDetail.value?.nickname) {
    closeEditNickname()
    return
  }
  
  updatingNickname.value = true
  
  try {
    await updateNickname({ nickname })
    
    // 更新用户详情
    if (userDetail.value) {
      userDetail.value.nickname = nickname
    }
    
    // 更新本地存储的用户信息
    if (currentUser.value && viewingUserId.value === currentUser.value.id) {
      currentUser.value.nickname = nickname
      setUser(currentUser.value)
    }
    
    showSuccess('昵称修改成功')
    closeEditNickname()
  } catch (error: any) {
    const errorMessage = handleApiError(error)
    showError(errorMessage)
  } finally {
    updatingNickname.value = false
  }
}

// 触发头像文件选择
const triggerAvatarUpload = () => {
  if (!viewingUserId.value || viewingUserId.value !== currentUser.value?.id) {
    showError('只能修改自己的头像')
    return
  }
  avatarFileInput.value?.click()
}

// 处理头像文件选择
const handleAvatarFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  try {
    await validateAvatarFile(file)
  } catch (error: any) {
    showError(error.message || '头像文件不合法')
    return
  }

  if (!viewingUserId.value || viewingUserId.value !== currentUser.value?.id) {
    showError('只能修改自己的头像')
    return
  }

  uploadingAvatar.value = true

  try {
    const avatarUrl = await uploadAvatar(file)
    const cacheSafeAvatarUrl = withCacheBuster(avatarUrl)

    // 更新用户详情
    if (userDetail.value) {
      userDetail.value.avatar = cacheSafeAvatarUrl
    }

    // 更新本地存储的用户信息
    if (currentUser.value) {
      currentUser.value.avatar = cacheSafeAvatarUrl
      setUser(currentUser.value)
    }

    // 更新头像缓存
    userAvatarCache.value.set(viewingUserId.value, cacheSafeAvatarUrl)

    showSuccess('头像上传成功')
  } catch (error: any) {
    const errorMessage = handleApiError(error)
    showError(errorMessage)
  } finally {
    uploadingAvatar.value = false
    // 清空文件输入
    if (target) {
      target.value = ''
    }
  }
}

// 打开成员列表
const openMembersList = async () => {
  if (!currentSessionId.value) return
  
  showMembersList.value = true
  await loadMembersData()
}

// 关闭成员列表
const closeMembersList = () => {
  showMembersList.value = false
}

// 加载成员数据
const loadMembersData = async () => {
  if (!currentSessionId.value) return
  
  loadingMembers.value = true
  try {
    // 并行加载成员列表、统计和在线用户
    const [membersData, statsData, onlineIds] = await Promise.all([
      getSessionMembers(currentSessionId.value),
      getSessionMemberStats(currentSessionId.value),
      getOnlineUserIds(currentSessionId.value)
    ])
    
    members.value = membersData
    memberStats.value = statsData
    onlineUserIds.value = new Set(onlineIds)
  } catch (error: any) {
    console.error('加载成员数据失败:', error)
    const errorMessage = handleApiError(error)
    showError(errorMessage)
  } finally {
    loadingMembers.value = false
  }
}

// 检查用户是否在线
const isUserOnline = (userId: number): boolean => {
  return onlineUserIds.value.has(userId)
}

// 获取当前会话的统计信息（用于标题显示）
const getCurrentSessionStats = () => {
  if (!currentSessionId.value || !memberStats.value) {
    return null
  }
  return memberStats.value
}

// 定期刷新成员统计（每30秒）
let memberStatsInterval: number | null = null

const startMemberStatsRefresh = () => {
  if (memberStatsInterval) {
    clearInterval(memberStatsInterval)
  }
  
  memberStatsInterval = window.setInterval(async () => {
    if (currentSessionId.value && !showMembersList.value) {
      // 只刷新统计，不刷新完整列表（节省资源）
      try {
        const stats = await getSessionMemberStats(currentSessionId.value)
        const onlineIds = await getOnlineUserIds(currentSessionId.value)
        memberStats.value = stats
        onlineUserIds.value = new Set(onlineIds)
      } catch (error) {
        console.error('刷新成员统计失败:', error)
      }
    }
  }, 30000) // 30秒刷新一次
}

const stopMemberStatsRefresh = () => {
  if (memberStatsInterval) {
    clearInterval(memberStatsInterval)
    memberStatsInterval = null
  }
}

const handleResize = () => {
  if (window.innerWidth > 768) {
    showSidebar.value = false
  }
}

onMounted(async () => {
  // 从本地存储读取用户信息和Token
  const user = getUser<UserInfo>()
  const token = getToken()
  
  if (!user || !token) {
    router.push('/login')
    return
  }
  
  // 添加窗口大小变化监听
  window.addEventListener('resize', handleResize)
  
  // 更新当前用户信息
  currentUser.value = user
  console.log('当前登录用户:', user)
  
  // 确保 WebSocket 断开旧连接（如果有）
  wsService.disconnect()
  
  // 先加载会话列表（这样可以在 WebSocket 连接成功后立即订阅）
  await loadSessions()
  
  // 加载当前会话的成员统计
  if (currentSessionId.value) {
    await loadMembersData()
    startMemberStatsRefresh()
  }
  
  // 确保消息滚动到底部（延迟执行，确保DOM完全渲染）
  setTimeout(() => {
    scrollToBottom()
  }, 300)
  
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
    // 连接失败时，显示错误提示
    showError('WebSocket 连接失败，请检查网络或刷新页面重试')
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
  // 移除窗口大小变化监听
  window.removeEventListener('resize', handleResize)
  // 停止成员统计刷新
  stopMemberStatsRefresh()
  // 断开 WebSocket 连接
  wsService.disconnect()
})
</script>

<template>
  <div class="app">
    <!-- 移动端遮罩层 -->
    <div 
      v-if="showSidebar" 
      class="sidebar-overlay"
      @click="showSidebar = false"
    ></div>
    
    <!-- 移动端菜单按钮 -->
    <button class="mobile-menu-btn" @click="toggleSidebar">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
        <line x1="3" y1="6" x2="21" y2="6"></line>
        <line x1="3" y1="12" x2="21" y2="12"></line>
        <line x1="3" y1="18" x2="21" y2="18"></line>
      </svg>
    </button>
    
    <aside class="sidebar" :class="{ 'sidebar-open': showSidebar }">
      <header class="sidebar-header">
        <h1>jay-chat</h1>
        <p class="subtitle">大型聊天室</p>
      </header>
      
      <div class="user-info">
        <div 
          class="user-avatar"
          @click="currentUser && openUserDetail(currentUser.id)"
          :title="`点击查看详情`"
        >
          <img 
            v-if="currentUser?.avatar" 
            :src="currentUser.avatar" 
            :alt="currentUser.nickname"
            class="avatar-img"
            @error="currentUser && (currentUser.avatar = null)"
          />
          <div v-else class="avatar-placeholder">
            {{ currentUser?.nickname?.[0] || 'U' }}
          </div>
        </div>
        <div class="user-details">
          <div class="user-name-wrapper">
            <div class="user-name">{{ currentUser?.nickname || '未知用户' }}</div>
          </div>
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
        <div class="chat-header-content">
          <div class="chat-title-section">
            <h2>
              {{ currentSessionId ? (sessions.find(s => s.id === currentSessionId)?.name || '聊天') : '请选择会话' }}
              <span v-if="getCurrentSessionStats()" class="member-count">
                ({{ getCurrentSessionStats()?.totalMembers || 0 }})
              </span>
            </h2>
            <button 
              v-if="currentSessionId && sessions.find(s => s.id === currentSessionId)?.type === 'group'"
              @click="openMembersList" 
              class="members-btn"
              title="查看群成员"
            >
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                <circle cx="9" cy="7" r="4"></circle>
                <path d="M23 21v-2a4 4 0 0 0-3-3.87"></path>
                <path d="M16 3.13a4 4 0 0 1 0 7.75"></path>
              </svg>
            </button>
          </div>
          <p class="chat-subtitle">
            <span class="status-dot" :class="{ 'connected': wsConnected }"></span>
            {{ wsConnected ? '已连接' : '连接中...' }}
          </p>
          <p v-if="getCurrentSessionStats()" class="online-count">
            <span class="online-dot"></span>
            {{ getCurrentSessionStats()?.onlineMembers || 0 }}
          </p>
        </div>
      </header>

      <section 
        ref="messagesContainer"
        class="chat-messages" 
        v-if="currentSessionId"
      >
        <ChatMessageList
          :loading="loading"
          :messages="messages"
          :current-user-id="currentUser?.id"
          @open-user-detail="openUserDetail"
          @open-image-preview="openImagePreview"
        />
      </section>
      <section v-else class="chat-messages empty-state">
        <p>请从左侧选择一个会话开始聊天</p>
      </section>

      <ChatComposer
        v-if="currentSessionId"
        v-model="inputMessage"
        :ws-connected="wsConnected"
        :uploading-image="uploadingImage"
        :show-emoji-picker="showEmojiPicker"
        @send="sendMessage"
        @toggle-emoji="toggleEmojiPicker"
        @close-emoji="closeEmojiPicker"
        @select-emoji="selectEmoji"
        @image-selected="handleImageFileChange"
      />

      <!-- 图片预览弹窗 -->
      <div v-if="showImagePreview" class="image-preview-overlay" @click="closeImagePreview">
        <div class="image-preview-container" @click.stop>
          <button class="image-preview-close" @click="closeImagePreview">×</button>
          <img :src="previewImageUrl || ''" alt="预览图片" class="image-preview-img" />
        </div>
      </div>
    </main>

    <!-- 用户详情弹窗 -->
    <div v-if="showUserDetail" class="modal-overlay" @click="closeUserDetail">
      <div class="modal-content user-detail-modal" @click.stop>
        <div class="modal-header">
          <h3>用户详情</h3>
          <button @click="closeUserDetail" class="modal-close-btn">×</button>
        </div>
        <div class="modal-body user-detail-body">
          <div v-if="loadingUserDetail" class="loading-detail">加载中...</div>
          <div v-else-if="userDetail" class="user-detail-content">
            <!-- 头像区域 -->
            <div class="detail-avatar-section">
              <div class="detail-avatar-wrapper">
                <img 
                  v-if="userDetail.avatar" 
                  :src="userDetail.avatar" 
                  :alt="userDetail.nickname"
                  class="detail-avatar-img"
                  @error="userDetail && (userDetail.avatar = null)"
                />
                <div v-else class="detail-avatar-placeholder">
                  {{ userDetail.nickname?.[0] || 'U' }}
                </div>
                <!-- 如果是自己，显示修改头像按钮 -->
                <button 
                  v-if="viewingUserId === currentUser?.id"
                  @click="triggerAvatarUpload"
                  class="avatar-edit-btn"
                  :disabled="uploadingAvatar"
                  title="修改头像"
                >
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                    <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                  </svg>
                  <span v-if="uploadingAvatar">上传中...</span>
                  <span v-else>修改头像</span>
                </button>
              </div>
              <input 
                ref="avatarFileInput"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handleAvatarFileChange"
              />
            </div>
            
            <!-- 用户信息 -->
            <div class="detail-info-section">
              <div class="detail-info-item">
                <label>昵称</label>
                <div class="detail-info-value">
                  <span>{{ userDetail.nickname }}</span>
                  <span class="detail-username">(@{{ userDetail.username }})</span>
                  <!-- 如果是自己，显示修改按钮 -->
                  <button 
                    v-if="viewingUserId === currentUser?.id"
                    @click="openEditNickname"
                    class="edit-btn-small"
                    title="修改昵称"
                  >
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                    </svg>
                  </button>
                </div>
              </div>
              
              <div class="detail-info-item">
                <label>用户名</label>
                <div class="detail-info-value">{{ userDetail.username }}</div>
              </div>
              
              <div class="detail-info-item">
                <label>注册时间</label>
                <div class="detail-info-value">{{ formatTime(userDetail.createdAt) }}</div>
              </div>
              
              <div class="detail-info-item" v-if="userDetail.lastLoginAt">
                <label>上次登录</label>
                <div class="detail-info-value">{{ formatTime(userDetail.lastLoginAt) }}</div>
              </div>
              
              <div class="detail-info-item" v-if="userDetail.lastMessageAt">
                <label>上次发言</label>
                <div class="detail-info-value">{{ formatTime(userDetail.lastMessageAt) }}</div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="closeUserDetail" class="btn-close">关闭</button>
        </div>
      </div>
    </div>

    <!-- 修改昵称弹窗（在详情弹窗内显示） -->
    <div v-if="showEditNickname" class="modal-overlay" @click="closeEditNickname">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>修改昵称</h3>
          <button @click="closeEditNickname" class="modal-close-btn">×</button>
        </div>
        <div class="modal-body">
          <input
            v-model="newNickname"
            type="text"
            placeholder="请输入新昵称"
            class="nickname-input"
            maxlength="20"
            @keyup.enter="saveNickname"
            @keyup.esc="closeEditNickname"
          />
        </div>
        <div class="modal-footer">
          <button @click="closeEditNickname" class="btn-cancel">取消</button>
          <button @click="saveNickname" class="btn-save" :disabled="updatingNickname || !newNickname.trim()">
            {{ updatingNickname ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 群成员列表弹窗 -->
    <MembersModal
      :show="showMembersList"
      :loading="loadingMembers"
      :members="members"
      :member-stats="memberStats"
      :is-user-online="isUserOnline"
      @close="closeMembersList"
    />
  </div>
</template>

<style scoped>
.app {
  display: flex;
  height: 100vh;
  background: #1a1a1a;
  color: #e0e0e0;
  position: relative;
  overflow: hidden;
}

/* 移动端菜单按钮 */
.mobile-menu-btn {
  display: none;
  position: fixed;
  top: 16px;
  left: 16px;
  z-index: 1001;
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: #252525;
  border: 1px solid #333;
  color: #e0e0e0;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.mobile-menu-btn:hover {
  background: #2a2a2a;
}

.mobile-menu-btn svg {
  width: 20px;
  height: 20px;
}

/* 移动端遮罩层 */
.sidebar-overlay {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 999;
  backdrop-filter: blur(2px);
}

.sidebar {
  width: 280px;
  background: #252525;
  border-right: 1px solid #333;
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
  z-index: 1000;
  position: relative;
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
  cursor: pointer;
  transition: transform 0.2s;
  overflow: hidden;
}

.user-avatar:hover {
  transform: scale(1.05);
}

.user-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-avatar .avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-details {
  flex: 1;
  min-width: 0;
}

.user-name-wrapper {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.edit-nickname-btn {
  width: 20px;
  height: 20px;
  padding: 0;
  background: transparent;
  border: none;
  color: #999;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
  flex-shrink: 0;
}

.edit-nickname-btn:hover {
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
}

.edit-nickname-btn svg {
  width: 14px;
  height: 14px;
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
  padding: 16px 20px;
  border-bottom: 1px solid #333;
  background: #252525;
  position: sticky;
  top: 0;
  z-index: 100;
  backdrop-filter: blur(10px);
  background: rgba(37, 37, 37, 0.95);
}

.chat-header-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-header h2 {
  margin: 0;
  font-size: 18px;
  color: #fff;
  font-weight: 600;
}

.chat-subtitle {
  margin: 0;
  font-size: 12px;
  color: #999;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #666;
  display: inline-block;
  transition: background 0.3s;
}

.status-dot.connected {
  background: #4ade80;
  box-shadow: 0 0 8px rgba(74, 222, 128, 0.5);
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scroll-behavior: smooth;
  /* 优化滚动性能 */
  -webkit-overflow-scrolling: touch;
}

.loading, .empty-messages, .empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
  font-size: 14px;
}

/* PC端优化 */
@media (min-width: 769px) {
  .chat-messages {
    padding: 24px;
    max-width: 1200px;
    margin: 0 auto;
    width: 100%;
  }
  
  .message {
    max-width: 65%;
  }
}

/* 响应式设计 - 移动端 */
@media (max-width: 768px) {
  .mobile-menu-btn {
    display: flex;
  }

  .sidebar-overlay {
    display: block;
  }

  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 280px;
    transform: translateX(-100%);
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.3);
  }

  .sidebar.sidebar-open {
    transform: translateX(0);
  }

  .chat {
    width: 100%;
  }

  .chat-header {
    padding: 12px 16px;
    padding-left: 60px;
  }

  .chat-header h2 {
    font-size: 16px;
  }

  .chat-messages {
    padding: 16px;
    gap: 12px;
  }


  .user-info {
    padding: 12px 16px;
  }

  .sidebar-header {
    padding: 16px;
  }

  .sidebar-header h1 {
    font-size: 20px;
  }
}

/* 响应式设计 - 小屏手机 */
@media (max-width: 480px) {
  .sidebar {
    width: 100%;
  }


  .chat-header {
    padding-left: 56px;
  }
}

/* 滚动条样式优化 */
.chat-messages::-webkit-scrollbar,
.room-list::-webkit-scrollbar {
  width: 6px;
}

.chat-messages::-webkit-scrollbar-track,
.room-list::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb,
.room-list::-webkit-scrollbar-thumb {
  background: #444;
  border-radius: 3px;
}

.chat-messages::-webkit-scrollbar-thumb:hover,
.room-list::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 输入框聚焦动画 */
.message-input:focus {
  transform: scale(1.01);
  transition: all 0.2s;
}

/* 加载动画 */
.loading {
  position: relative;
}

.loading::after {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  top: 50%;
  left: 50%;
  margin-left: -10px;
  margin-top: -10px;
  border: 2px solid #444;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* 用户详情弹窗样式 */
.user-detail-modal {
  max-width: 500px;
  width: 90%;
}

.user-detail-body {
  padding: 24px;
}

.loading-detail {
  text-align: center;
  padding: 40px;
  color: #999;
}

.user-detail-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.detail-avatar-section {
  display: flex;
  justify-content: center;
}

.detail-avatar-wrapper {
  position: relative;
  width: 120px;
  height: 120px;
}

.detail-avatar-img,
.detail-avatar-placeholder {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #667eea;
}

.detail-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-size: 48px;
  font-weight: 600;
}

.avatar-edit-btn {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #667eea;
  border: 2px solid #1a1a1a;
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  font-size: 10px;
  flex-direction: column;
  gap: 2px;
  padding: 2px;
}

.avatar-edit-btn:hover:not(:disabled) {
  background: #5568d3;
  transform: scale(1.1);
}

.avatar-edit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.avatar-edit-btn svg {
  width: 14px;
  height: 14px;
}

.detail-info-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.detail-info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-info-item label {
  font-size: 12px;
  color: #999;
  font-weight: 500;
}

.detail-info-value {
  font-size: 14px;
  color: #fff;
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-username {
  color: #999;
  font-size: 13px;
}

.edit-btn-small {
  width: 20px;
  height: 20px;
  padding: 0;
  background: transparent;
  border: none;
  color: #667eea;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
  margin-left: auto;
}

.edit-btn-small:hover {
  background: rgba(102, 126, 234, 0.1);
  color: #5568d3;
}

/* 修改昵称弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  backdrop-filter: blur(4px);
}

.modal-content {
  background: #252525;
  border-radius: 12px;
  width: 90%;
  max-width: 400px;
  border: 1px solid #333;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  animation: modalFadeIn 0.3s ease;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: scale(0.9);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #333;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: #fff;
  font-weight: 500;
}

.modal-close-btn {
  width: 28px;
  height: 28px;
  padding: 0;
  background: transparent;
  border: none;
  color: #999;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.modal-close-btn:hover {
  background: #333;
  color: #fff;
}

.modal-body {
  padding: 24px;
}

.nickname-input {
  width: 100%;
  padding: 12px 16px;
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 8px;
  color: #fff;
  font-size: 14px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.nickname-input:focus {
  border-color: #667eea;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
}

.nickname-input::placeholder {
  color: #666;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #333;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-cancel,
.btn-save {
  padding: 10px 20px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-cancel {
  background: #333;
  color: #e0e0e0;
}

.btn-cancel:hover {
  background: #3a3a3a;
}

.btn-save {
  background: #667eea;
  color: #fff;
}

.btn-save:hover:not(:disabled) {
  background: #5568d3;
}

.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 聊天标题区域 */
.chat-title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-count {
  font-size: 16px;
  font-weight: normal;
  color: #999;
  margin-left: 4px;
}

.members-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  background: transparent;
  border: 1px solid #444;
  border-radius: 6px;
  color: #e0e0e0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.members-btn:hover {
  background: #333;
  border-color: #555;
  color: #fff;
}

.members-btn svg {
  width: 18px;
  height: 18px;
}

.online-count {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #999;
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.online-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4caf50;
  display: inline-block;
  animation: pulse 2s infinite;
}

.btn-close {
  padding: 8px 20px;
  background: #333;
  color: #e0e0e0;
  border: none;
  border-radius: 6px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #3a3a3a;
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
  }
  50% {
    opacity: 0.5;
  }
}

/* 群成员列表弹窗 */
/* 已迁移至 components/chat/MembersModal.vue */

/* 图片预览弹窗 */
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  cursor: pointer;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.image-preview-container {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: default;
}

.image-preview-close {
  position: absolute;
  top: -40px;
  right: 0;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  color: white;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  z-index: 10001;
}

.image-preview-close:hover {
  background: rgba(255, 255, 255, 0.3);
  transform: scale(1.1);
}

.image-preview-img {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
}
</style>
