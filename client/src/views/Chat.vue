<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import type { UserInfo, UserDetail } from '../api/auth'
import { updateNickname, getUserDetail, uploadAvatar } from '../api/auth'
import {
  getSessions,
  getOrCreateDefaultSession,
  getSessionMembers,
  getSessionMemberStats,
  getOnlineUserIds,
  getUsers,
  createPrivateSession,
  getPrivateSessionOtherMember,
  deleteSession,
  recallMessage,
  getGroupInfo,
  createGroup,
  type ChatSession,
  type SessionMember,
  type SessionMemberStats,
  type ContactUser
} from '../api/chat'
import { wsService } from '../utils/websocket'
import { formatTime } from '../utils/date'
import { getUser, getToken, clearAuth, setUser } from '../utils/storage'
import { handleApiError, showError, showSuccess } from '../utils/error'
import MembersModal from '../components/chat/MembersModal.vue'
import ChatMessageList from '../components/chat/ChatMessageList.vue'
import ChatComposer from '../components/chat/ChatComposer.vue'
import GroupSettings from '../components/chat/GroupSettings.vue'
import { validateAvatarFile, withCacheBuster } from '../utils/avatar'
import { useChatMessages } from '../composables/useChatMessages'

const router = useRouter()

const currentUser = ref<UserInfo | null>(null)
const sessions = ref<ChatSession[]>([])
const currentSessionId = ref<number | null>(null)
const wsConnected = ref(false)
const messagesContainer = ref<HTMLElement | null>(null)
const showSidebar = ref(false)

// 联系人列表相关
const showContacts = ref(false)
const contacts = ref<ContactUser[]>([])
const loadingContacts = ref(false)
const contactSearchKeyword = ref('')

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

// 群设置相关
const showGroupSettings = ref(false)
const currentGroupInfo = ref<ChatSession | null>(null)

// 创建群聊相关
const showCreateGroup = ref(false)
const createGroupName = ref('')
const createGroupMembers = ref<number[]>([])
const creatingGroup = ref(false)

const userAvatarCache = ref<Map<number, string | null>>(new Map())

// 私人会话其他成员缓存 (sessionId -> contactUser)
const privateSessionMembers = ref<Map<number, ContactUser>>(new Map())

// 未读消息数量 (sessionId -> count)
const unreadCounts = ref<Map<number, number>>(new Map())

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
  replyingTo,
  setReplyingTo,
  cancelReply,
  handleImageFileChange,
  openImagePreview,
  closeImagePreview,
  handleMessage,
  handleRecallMessage
} = useChatMessages({
  currentSessionId,
  currentUser,
  getUserAvatar,
  scrollToBottom,
  showError,
  handleApiError,
  onMessageReceived: (message) => {
    // 收到非当前会话的消息，增加未读计数
    const sessionId = message.sessionId
    const currentCount = unreadCounts.value.get(sessionId) || 0
    unreadCounts.value.set(sessionId, currentCount + 1)

    // 将会话置顶
    const sessionIndex = sessions.value.findIndex(s => s.id === sessionId)
    if (sessionIndex > 0) {
      const sessionsToMove = sessions.value.splice(sessionIndex, 1)
      if (sessionsToMove[0]) {
        sessions.value.unshift(sessionsToMove[0])
      }
    }
  }
})

const loadSessions = async () => {
  try {
    console.log('开始加载会话列表...')
    
    // 先确保用户加入全局的"公共聊天室"
    console.log('确保加入全局公共聊天室...')
    const defaultSession = await getOrCreateDefaultSession()
    console.log('默认会话（公共聊天室）:', defaultSession)
    
    // 然后获取用户的所有会话列表
    const rawSessions = await getSessions()

    // 去重：以session id为key，只保留每个id的第一个会话
    const seen = new Set<number>()
    sessions.value = rawSessions.filter(session => {
      if (seen.has(session.id)) {
        console.warn('发现重复会话，已过滤:', session.id)
        return false
      }
      seen.add(session.id)
      return true
    })
    console.log('获取到的会话列表（去重后）:', sessions.value)

    // 为每个私人会话获取对方信息
    for (const session of sessions.value) {
      if (session.type === 'private') {
        try {
          const otherMember = await getPrivateSessionOtherMember(session.id)
          if (otherMember) {
            privateSessionMembers.value.set(session.id, otherMember)
          }
        } catch (e) {
          console.error('获取私人会话成员失败:', session.id, e)
        }
      }
      // 初始化未读数为0
      unreadCounts.value.set(session.id, 0)
    }

    // 确保"公共聊天室"在列表中（如果不在，添加到列表开头）
    const hasDefaultSession = sessions.value.some(s => s.id === defaultSession.id)
    if (!hasDefaultSession) {
      sessions.value.unshift(defaultSession)
    }

    // 订阅所有会话（这样可以收到所有会话的消息）
    if (wsConnected.value || wsService.isConnected()) {
      const allSessionIds = sessions.value.map(s => s.id)
      wsService.subscribeToAllSessions(allSessionIds, handleWsMessage)
      console.log('已订阅所有会话:', allSessionIds)
    }

    // 优先选择"公共聊天室"作为当前会话
    if (!currentSessionId.value) {
      currentSessionId.value = defaultSession.id
      // 更新 WebSocket 服务的当前会话ID
      wsService.setCurrentSessionId(defaultSession.id)
      await loadMessages(defaultSession.id)
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
        const allSessionIds = sessions.value.map(s => s.id)
        wsService.subscribeToAllSessions(allSessionIds, handleWsMessage)
      }
    } catch (createError) {
      console.error('创建默认会话也失败:', createError)
      showError('无法加载会话，请检查网络连接或刷新页面')
    }
  }
}

// 获取会话显示名称（私人会话显示对方昵称）
const getSessionDisplayName = (session: ChatSession): string => {
  if (session.name) {
    return session.name
  }
  if (session.type === 'private') {
    const otherMember = privateSessionMembers.value.get(session.id)
    if (otherMember) {
      return otherMember.nickname
    }
    return '单聊'
  }
  return '群聊'
}

// 获取会话未读数量
const getUnreadCount = (sessionId: number): number => {
  return unreadCounts.value.get(sessionId) || 0
}

// 清除会话未读数量
const clearUnreadCount = (sessionId: number) => {
  unreadCounts.value.set(sessionId, 0)
}

// 加载联系人列表
const loadContacts = async () => {
  loadingContacts.value = true
  try {
    contacts.value = await getUsers(contactSearchKeyword.value || undefined)
  } catch (error: any) {
    console.error('加载联系人列表失败:', error)
    const errorMessage = handleApiError(error)
    showError(errorMessage)
  } finally {
    loadingContacts.value = false
  }
}

// 搜索联系人
const searchContacts = async () => {
  await loadContacts()
}

// 打开联系人面板
const openContacts = async () => {
  showContacts.value = true
  showSidebar.value = false
  await loadContacts()
}

// 关闭联系人面板
const closeContacts = () => {
  showContacts.value = false
  contactSearchKeyword.value = ''
}

// 点击联系人发起私聊
const startPrivateChat = async (user: ContactUser) => {
  try {
    // 创建私人会话
    const session = await createPrivateSession(user.userId)
    console.log('创建私人会话成功:', session)

    // 刷新会话列表
    await loadSessions()

    // 切换到该会话
    await switchSession(session.id)

    // 关闭联系人面板
    closeContacts()
  } catch (error: any) {
    console.error('创建私人会话失败:', error)
    const errorMessage = handleApiError(error)
    showError(errorMessage)
  }
}

const switchSession = async (sessionId: number) => {
  if (currentSessionId.value === sessionId) return

  // 停止旧会话的统计刷新
  stopMemberStatsRefresh()

  currentSessionId.value = sessionId
  wsService.setCurrentSessionId(sessionId)

  // 清除该会话的未读数量
  clearUnreadCount(sessionId)

  // 移动端切换会话后关闭侧边栏
  if (window.innerWidth <= 768) {
    showSidebar.value = false
  }

  await loadMessages(sessionId)
  // 订阅所有会话的消息（保持对所有会话的订阅，以便显示未读红点）
  if (wsConnected.value || wsService.isConnected()) {
    const allSessionIds = sessions.value.map(s => s.id)
    wsService.subscribeToAllSessions(allSessionIds, handleWsMessage)
  }

  // 加载新会话的成员统计
  await loadMembersData()
  startMemberStatsRefresh()
}

// 删除当前会话
const handleDeleteSession = async (sessionId: number, event: Event) => {
  event.stopPropagation()

  const session = sessions.value.find(s => s.id === sessionId)
  if (!session) return

  const confirmMessage = session.type === 'private'
    ? '确定要删除与该联系人的聊天吗？'
    : '确定要退出会话吗？'

  if (!confirm(confirmMessage)) return

  try {
    await deleteSession(sessionId)
    showSuccess('会话已删除')

    // 如果删除的是当前会话，切换到第一个可用会话
    if (currentSessionId.value === sessionId) {
      const remainingSessions = sessions.value.filter(s => s.id !== sessionId)
      if (remainingSessions.length > 0 && remainingSessions[0]) {
        await switchSession(remainingSessions[0].id)
      } else {
        currentSessionId.value = null
        messages.value = []
      }
    }

    // 刷新会话列表
    await loadSessions()
  } catch (error: any) {
    console.error('删除会话失败:', error)
    showError(handleApiError(error))
  }
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

// 打开群设置
const openGroupSettings = async () => {
  if (!currentSessionId.value) return
  try {
    const groupInfo = await getGroupInfo(currentSessionId.value)
    currentGroupInfo.value = groupInfo
    showGroupSettings.value = true
  } catch (error: any) {
    showError('获取群信息失败')
  }
}

// 关闭群设置
const closeGroupSettings = () => {
  showGroupSettings.value = false
  currentGroupInfo.value = null
}

// 更新群信息后刷新
const handleGroupUpdate = async () => {
  if (!currentSessionId.value) return
  // 刷新会话列表
  await loadSessions()
  // 刷新群信息
  const groupInfo = await getGroupInfo(currentSessionId.value)
  currentGroupInfo.value = groupInfo
  // 刷新成员列表
  await loadMembersData()
}

// 踢出成员后刷新
const handleKickMember = async () => {
  await loadMembersData()
}

// 判断当前会话是否是群聊
const isCurrentSessionGroup = computed(() => {
  const session = sessions.value.find(s => s.id === currentSessionId.value)
  return session?.type === 'group'
})

// 打开创建群聊弹窗
const openCreateGroup = () => {
  showCreateGroup.value = true
  createGroupName.value = ''
  createGroupMembers.value = []
}

// 关闭创建群聊弹窗
const closeCreateGroup = () => {
  showCreateGroup.value = false
}

// 创建群聊
const doCreateGroup = async () => {
  if (!createGroupName.value.trim()) {
    showError('请输入群名称')
    return
  }
  creatingGroup.value = true
  try {
    const newGroup = await createGroup({
      groupName: createGroupName.value.trim(),
      memberIds: createGroupMembers.value
    })
    closeCreateGroup()
    // 刷新会话列表
    await loadSessions()
    // 切换到新创建的群
    await switchSession(newGroup.id)
    showSuccess('群聊创建成功')
  } catch (error: unknown) {
    const msg = handleApiError(error)
    showError(msg)
  } finally {
    creatingGroup.value = false
  }
}

// 包装消息处理函数，区分普通消息和撤回消息
const handleWsMessage = (message: import('../utils/websocket').ChatMessage) => {
  if (message.recalled) {
    handleRecallMessage(message)
  } else {
    handleMessage(message)
  }
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

// 撤回消息
const handleRecallMessageClick = async (messageId: number) => {
  try {
    await recallMessage(messageId)
    showSuccess('消息已撤回')
  } catch (error: unknown) {
    const errorMessage = handleApiError(error)
    showError(errorMessage)
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
  wsService.connect(token, handleWsMessage, (error) => {
    console.error('WebSocket 连接错误:', error)
    wsConnected.value = false
    // 连接失败时，显示错误提示
    showError('WebSocket 连接失败，请检查网络或刷新页面重试')
  })
  
  // 初始化连接状态
  wsConnected.value = wsService.isConnected()
  
  // 等待一小段时间让连接建立，然后检查状态
  setTimeout(() => {
    if (sessions.value.length > 0) {
      if (wsConnected.value || wsService.isConnected()) {
        console.log('WebSocket 已连接，立即订阅所有会话')
        const allSessionIds = sessions.value.map(s => s.id)
        wsService.subscribeToAllSessions(allSessionIds, handleWsMessage)
      } else {
        console.log('WebSocket 未连接，等待连接成功后自动订阅所有会话')
        // 如果 3 秒后还没连接，再次尝试订阅
        setTimeout(() => {
          if (sessions.value.length > 0 && (wsConnected.value || wsService.isConnected())) {
            console.log('延迟订阅所有会话')
            const allSessionIds = sessions.value.map(s => s.id)
            wsService.subscribeToAllSessions(allSessionIds, handleWsMessage)
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
        <div class="tab-header">
          <button
            class="tab-btn"
            :class="{ active: !showContacts }"
            @click="showContacts = false"
          >
            会话
          </button>
          <button
            class="tab-btn"
            :class="{ active: showContacts }"
            @click="openContacts"
          >
            联系人
          </button>
          <button class="tab-btn create-group-btn" @click="openCreateGroup" title="创建群聊">
            + 建群
          </button>
        </div>

        <!-- 会话列表 -->
        <template v-if="!showContacts">
          <ul v-if="sessions.length > 0">
            <li
              v-for="session in sessions"
              :key="session.id"
              class="room-item"
              :class="{ active: currentSessionId === session.id }"
              @click="switchSession(session.id)"
            >
              <span class="session-icon">
                {{ session.type === 'private' ? '👤' : '👥' }}
              </span>
              <span class="session-name">{{ getSessionDisplayName(session) }}</span>
              <span v-if="getUnreadCount(session.id) > 0" class="unread-badge">
                {{ getUnreadCount(session.id) > 99 ? '99+' : getUnreadCount(session.id) }}
              </span>
              <button
                class="delete-session-btn"
                @click="handleDeleteSession(session.id, $event)"
                title="删除会话"
              >
                ×
              </button>
            </li>
          </ul>
          <p v-else class="empty-sessions">暂无会话</p>
        </template>

        <!-- 联系人列表 -->
        <template v-else>
          <div class="contact-search">
            <input
              v-model="contactSearchKeyword"
              type="text"
              placeholder="搜索联系人..."
              class="search-input"
              @keyup.enter="searchContacts"
            />
          </div>
          <div v-if="loadingContacts" class="loading-contacts">加载中...</div>
          <ul v-else-if="contacts.length > 0">
            <li
              v-for="contact in contacts"
              :key="contact.userId"
              class="contact-item"
              @click="startPrivateChat(contact)"
            >
              <div class="contact-avatar">
                <img
                  v-if="contact.avatar"
                  :src="contact.avatar"
                  :alt="contact.nickname"
                  class="avatar-img"
                  @error="contact.avatar = null"
                />
                <div v-else class="avatar-placeholder">
                  {{ contact.nickname?.[0] || 'U' }}
                </div>
              </div>
              <div class="contact-info">
                <div class="contact-name">{{ contact.nickname }}</div>
                <div class="contact-username">@{{ contact.username }}</div>
              </div>
            </li>
          </ul>
          <p v-else class="empty-sessions">
            {{ contactSearchKeyword ? '未找到匹配的联系人' : '暂无联系人' }}
          </p>
        </template>
      </section>
    </aside>

    <main class="chat">
      <header class="chat-header">
        <div class="chat-header-content">
          <div class="chat-title-section">
            <h2>
              {{ currentSessionId ? getSessionDisplayName(sessions.find(s => s.id === currentSessionId)!) : '请选择会话' }}
              <span v-if="getCurrentSessionStats()" class="member-count">
                ({{ getCurrentSessionStats()?.totalMembers || 0 }})
              </span>
            </h2>
            <button
              v-if="currentSessionId && isCurrentSessionGroup"
              @click="openGroupSettings"
              class="members-btn"
              title="群设置"
            >
              ⚙️
            </button>
            <button
              v-if="currentSessionId && isCurrentSessionGroup"
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
          @reply-message="setReplyingTo"
          @recall-message="handleRecallMessageClick"
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
        :replying-to="replyingTo"
        @send="sendMessage"
        @toggle-emoji="toggleEmojiPicker"
        @close-emoji="closeEmojiPicker"
        @select-emoji="selectEmoji"
        @image-selected="handleImageFileChange"
        @cancel-reply="cancelReply"
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

    <!-- 群设置弹窗 -->
    <GroupSettings
      :show="showGroupSettings"
      :session="currentGroupInfo"
      :members="members"
      :current-user-id="currentUser?.id"
      @close="closeGroupSettings"
      @update="handleGroupUpdate"
      @kick-member="handleKickMember"
    />

    <!-- 创建群聊弹窗 -->
    <div v-if="showCreateGroup" class="modal-overlay" @click.self="closeCreateGroup">
      <div class="modal-content">
        <div class="modal-header">
          <h2>创建群聊</h2>
          <button class="close-btn" @click="closeCreateGroup">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>群名称</label>
            <input v-model="createGroupName" type="text" placeholder="请输入群名称" />
          </div>
          <div class="form-group">
            <label>选择成员</label>
            <div class="member-select">
              <label v-for="user in contacts" :key="user.userId" class="member-option">
                <input type="checkbox" v-model="createGroupMembers" :value="user.userId" />
                <img v-if="user.avatar" :src="user.avatar" class="member-avatar" />
                <div v-else class="member-avatar-placeholder">
                  {{ user.nickname?.[0] || user.username[0] }}
                </div>
                <span>{{ user.nickname || user.username }}</span>
              </label>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-primary" @click="doCreateGroup" :disabled="creatingGroup">
            {{ creatingGroup ? '创建中...' : '创建群聊' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 主容器 - 明亮主题 */
.app {
  display: flex;
  height: 100vh;
  width: 100%;
  /* 柔和的粉彩渐变背景 */
  background: linear-gradient(135deg, #fff5f5 0%, #f0fffe 30%, #fff9f0 60%, #f5f0ff 100%);
  color: var(--text-primary);
  position: relative;
  overflow: hidden;
}

/* 背景装饰 */
.app::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background:
    radial-gradient(circle at 20% 80%, rgba(255, 107, 157, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 80% 20%, rgba(78, 205, 196, 0.1) 0%, transparent 50%),
    radial-gradient(circle at 40% 40%, rgba(102, 126, 234, 0.08) 0%, transparent 40%);
  pointer-events: none;
  animation: bgFloat 20s ease-in-out infinite;
}

@keyframes bgFloat {
  0%, 100% { transform: translate(0, 0); }
  50% { transform: translate(-2%, -2%); }
}

/* 移动端菜单按钮 */
.mobile-menu-btn {
  display: none;
  position: fixed;
  top: 16px;
  left: 16px;
  z-index: 1001;
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  color: var(--text-primary);
  cursor: pointer;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
  box-shadow: var(--shadow-md);
}

.mobile-menu-btn:hover {
  transform: scale(1.05);
  box-shadow: var(--shadow-lg);
}

.mobile-menu-btn svg {
  width: 22px;
  height: 22px;
}

/* 移动端遮罩层 */
.sidebar-overlay {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(4px);
  z-index: 999;
}

/* 侧边栏 - 毛玻璃效果 */
.sidebar {
  width: 300px;
  min-width: 300px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(255, 255, 255, 0.5);
  display: flex;
  flex-direction: column;
  transition: transform 0.3s ease;
  z-index: 1000;
  position: relative;
  box-shadow: 4px 0 24px rgba(0, 0, 0, 0.05);
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.sidebar-header h1 {
  margin: 0 0 4px 0;
  font-size: 26px;
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  font-weight: 700;
}

.subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
}

/* 用户信息区域 */
.user-info {
  padding: 20px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  gap: 14px;
}

.user-avatar {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #ff6b9d 0%, #ff9f43 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
  cursor: pointer;
  transition: all 0.3s;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
}

.user-avatar:hover {
  transform: scale(1.08);
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
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.edit-nickname-btn {
  width: 22px;
  height: 22px;
  padding: 0;
  background: transparent;
  border: none;
  color: var(--text-light);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
  flex-shrink: 0;
}

.edit-nickname-btn:hover {
  color: #ff6b9d;
  background: rgba(255, 107, 157, 0.1);
}

.edit-nickname-btn svg {
  width: 14px;
  height: 14px;
}

.user-id {
  font-size: 12px;
  color: var(--text-light);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.logout-btn {
  padding: 8px 14px;
  background: rgba(255, 107, 157, 0.1);
  color: #ff6b9d;
  border: none;
  border-radius: var(--radius-md);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: rgba(255, 107, 157, 0.2);
}

/* 会话列表 */
.room-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 0;
}

/* 标签页样式 */
.tab-header {
  display: flex;
  padding: 0 16px 14px;
  gap: 10px;
}

.tab-btn {
  flex: 1;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.6);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
}

.tab-btn:hover {
  background: rgba(255, 255, 255, 0.9);
  color: #ff6b9d;
}

.tab-btn.active {
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  color: white;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
}

/* 会话图标 */
.session-icon {
  margin-right: 10px;
  font-size: 18px;
}

.room-list h2 {
  padding: 0 20px 12px;
  margin: 0;
  font-size: 13px;
  color: var(--text-light);
  text-transform: uppercase;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.room-list ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.room-item {
  padding: 14px 20px;
  cursor: pointer;
  transition: all 0.2s;
  border-left: 3px solid transparent;
  margin: 2px 0;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 500;
  color: var(--text-primary);
}

.room-item:hover {
  background: rgba(255, 255, 255, 0.6);
}

.room-item.active {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.15), rgba(255, 159, 67, 0.1));
  border-left-color: #ff6b9d;
}

/* 未读消息徽章 */
.unread-badge {
  margin-left: auto;
  padding: 3px 8px;
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  color: white;
  font-size: 11px;
  font-weight: 600;
  border-radius: var(--radius-full);
  min-width: 20px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(255, 107, 157, 0.3);
}

/* 会话名称 */
.session-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 删除会话按钮 */
.delete-session-btn {
  opacity: 0;
  width: 24px;
  height: 24px;
  padding: 0;
  background: rgba(255, 107, 157, 0.1);
  border: none;
  border-radius: 8px;
  color: #ff6b9d;
  font-size: 16px;
  font-weight: bold;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.room-item:hover .delete-session-btn {
  opacity: 1;
}

.delete-session-btn:hover {
  background: rgba(255, 107, 157, 0.2);
  transform: scale(1.1);
}

/* 联系人搜索 */
.contact-search {
  padding: 0 16px 12px;
}

.search-input {
  width: 100%;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 14px;
  outline: none;
  box-sizing: border-box;
  transition: all 0.3s;
  background-image: linear-gradient(#fff, #fff), linear-gradient(135deg, #ff6b9d, #4ecdc4);
  background-origin: border-box;
  background-clip: padding-box, border-box;
}

.search-input:focus {
  box-shadow: 0 0 20px rgba(255, 107, 157, 0.15);
}

.search-input::placeholder {
  color: var(--text-light);
}

.loading-contacts {
  text-align: center;
  padding: 20px;
  color: var(--text-light);
  font-size: 14px;
}

/* 联系人列表 */
.contact-item {
  display: flex;
  align-items: center;
  padding: 14px 20px;
  cursor: pointer;
  transition: all 0.2s;
}

.contact-item:hover {
  background: rgba(255, 255, 255, 0.6);
}

.contact-item .contact-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.contact-item .contact-username {
  font-size: 13px;
  color: var(--text-light);
}

.contact-avatar {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, #4ecdc4, #44a08d);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
  overflow: hidden;
  margin-right: 14px;
  box-shadow: 0 4px 12px rgba(78, 205, 196, 0.3);
}

.contact-avatar .avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.contact-avatar .avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.contact-info {
  flex: 1;
  min-width: 0;
}

.contact-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-username {
  font-size: 12px;
  color: var(--text-light);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.empty-sessions {
  padding: 30px 20px;
  text-align: center;
  color: var(--text-light);
  font-size: 14px;
}

/* 聊天区域 */
.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(10px);
  position: relative;
  min-width: 0;
}

/* 聊天头部 */
.chat-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(20px);
  position: sticky;
  top: 0;
  z-index: 100;
}

.chat-header-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chat-header h2 {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
  font-weight: 600;
}

.chat-subtitle {
  margin: 0;
  font-size: 13px;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--text-light);
  display: inline-block;
  transition: all 0.3s;
}

.status-dot.connected {
  background: #4ecdc4;
  box-shadow: 0 0 8px rgba(78, 205, 196, 0.5);
}

/* 聊天消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
}

/* 响应式 */
@media (min-width: 769px) {
  .chat-messages {
    padding: 28px;
    max-width: 100%;
    margin: 0 auto;
    width: 100%;
    box-sizing: border-box;
  }
}

.loading, .empty-messages, .empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: var(--text-light);
  font-size: 15px;
}

/* 移动端响应式 */
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
    width: 300px;
    transform: translateX(-100%);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.1);
  }

  .sidebar.sidebar-open {
    transform: translateX(0);
  }

  .chat {
    width: 100%;
  }

  .chat-header {
    padding: 16px 20px;
    padding-left: 70px;
  }

  .chat-header h2 {
    font-size: 18px;
  }

  .chat-messages {
    padding: 20px 16px;
    gap: 14px;
  }

  .user-info {
    padding: 16px 20px;
  }

  .sidebar-header {
    padding: 20px;
  }

  .sidebar-header h1 {
    font-size: 22px;
  }
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar,
.room-list::-webkit-scrollbar {
  width: 5px;
}

.chat-messages::-webkit-scrollbar-track,
.room-list::-webkit-scrollbar-track {
  background: transparent;
}

.chat-messages::-webkit-scrollbar-thumb,
.room-list::-webkit-scrollbar-thumb {
  background: linear-gradient(#ff6b9d, #ff9f43);
  border-radius: var(--radius-full);
}

.chat-messages::-webkit-scrollbar-thumb:hover,
.room-list::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(#ff6b9d, #4ecdc4);
}

/* 用户详情弹窗 */
.user-detail-modal {
  max-width: 480px;
  width: 90%;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-xl);
  overflow: hidden;
}

.user-detail-body {
  padding: 28px;
}

.loading-detail {
  text-align: center;
  padding: 40px;
  color: var(--text-light);
}

.user-detail-content {
  display: flex;
  flex-direction: column;
  align-items: center;
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
  border-radius: var(--radius-lg);
  object-fit: cover;
  border: 3px solid transparent;
  background-image: linear-gradient(#fff, #fff), linear-gradient(135deg, #ff6b9d, #4ecdc4);
  background-origin: border-box;
  background-clip: padding-box, border-box;
}

.detail-avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  color: white;
  font-size: 48px;
  font-weight: 600;
}

.avatar-edit-btn {
  position: absolute;
  bottom: -4px;
  right: -4px;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  border: 2px solid white;
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
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
}

.avatar-edit-btn:hover:not(:disabled) {
  transform: scale(1.1);
}

.avatar-edit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.avatar-edit-btn svg {
  width: 16px;
  height: 16px;
}

.detail-info-section {
  display: flex;
  flex-direction: column;
  gap: 18px;
  width: 100%;
}

.detail-info-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.detail-info-item label {
  font-size: 12px;
  color: var(--text-light);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.detail-info-value {
  font-size: 15px;
  color: var(--text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.detail-username {
  color: var(--text-secondary);
  font-size: 14px;
}

.edit-btn-small {
  width: 24px;
  height: 24px;
  padding: 0;
  background: transparent;
  border: none;
  color: #ff6b9d;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: var(--radius-sm);
  transition: all 0.2s;
  margin-left: auto;
}

.edit-btn-small:hover {
  background: rgba(255, 107, 157, 0.1);
}

/* 弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.8);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.modal-content {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-xl);
  width: 90%;
  max-width: 400px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  animation: modalFadeIn 0.3s ease;
  overflow: hidden;
}

@keyframes modalFadeIn {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h3 {
  margin: 0;
  font-size: 18px;
  color: var(--text-primary);
  font-weight: 600;
}

.modal-close-btn {
  width: 32px;
  height: 32px;
  padding: 0;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  color: var(--text-secondary);
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.modal-close-btn:hover {
  background: rgba(255, 107, 157, 0.1);
  color: #ff6b9d;
}

.modal-body {
  padding: 24px;
}

.nickname-input {
  width: 100%;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.8);
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  color: var(--text-primary);
  font-size: 15px;
  outline: none;
  transition: all 0.3s;
  box-sizing: border-box;
  background-image: linear-gradient(#fff, #fff), linear-gradient(135deg, #ff6b9d, #4ecdc4);
  background-origin: border-box;
  background-clip: padding-box, border-box;
}

.nickname-input:focus {
  box-shadow: 0 0 20px rgba(255, 107, 157, 0.15);
}

.nickname-input::placeholder {
  color: var(--text-light);
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.btn-cancel,
.btn-save {
  padding: 12px 24px;
  border-radius: var(--radius-md);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.btn-cancel {
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-secondary);
}

.btn-cancel:hover {
  background: rgba(0, 0, 0, 0.1);
}

.btn-save {
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
}

.btn-save:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.4);
}

.btn-save:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 聊天标题 */
.chat-title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-count {
  font-size: 15px;
  font-weight: normal;
  color: var(--text-secondary);
  margin-left: 4px;
}

.members-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  background: rgba(78, 205, 196, 0.1);
  border: 1px solid rgba(78, 205, 196, 0.3);
  border-radius: var(--radius-md);
  color: #4ecdc4;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.members-btn:hover {
  background: rgba(78, 205, 196, 0.2);
  transform: scale(1.05);
}

.members-btn svg {
  width: 18px;
  height: 18px;
}

.create-group-btn {
  margin-left: auto;
  padding: 6px 12px;
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

.create-group-btn:hover {
  transform: scale(1.05);
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 450px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.form-group input[type="text"] {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
  box-sizing: border-box;
}

.member-select {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.member-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px;
  cursor: pointer;
}

.member-option:hover {
  background: #f9f9f9;
}

.member-option input {
  width: 16px;
  height: 16px;
}

.member-avatar,
.member-avatar-placeholder {
  width: 32px;
  height: 32px;
  border-radius: 6px;
}

.member-avatar-placeholder {
  background: linear-gradient(135deg, #4ecdc4, #44a08d);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 12px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
}

.modal-footer .btn-primary {
  width: 100%;
  padding: 12px;
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 15px;
  cursor: pointer;
}

.modal-footer .btn-primary:disabled {
  opacity: 0.6;
}

.online-count {
  margin: 6px 0 0 0;
  font-size: 13px;
  color: var(--text-secondary);
  text-align: center;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.online-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #4ecdc4;
  display: inline-block;
  animation: pulse 2s infinite;
}

.btn-close {
  padding: 10px 24px;
  background: rgba(0, 0, 0, 0.05);
  color: var(--text-secondary);
  border: none;
  border-radius: var(--radius-md);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-close:hover {
  background: rgba(0, 0, 0, 0.1);
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.7;
    transform: scale(0.9);
  }
}

/* 图片预览 */
.image-preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10000;
  cursor: pointer;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
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
  top: -50px;
  right: 0;
  width: 40px;
  height: 40px;
  background: rgba(255, 107, 157, 0.1);
  border: none;
  border-radius: 50%;
  color: #ff6b9d;
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
  background: rgba(255, 107, 157, 0.2);
  transform: scale(1.1);
}

.image-preview-img {
  max-width: 100%;
  max-height: 90vh;
  object-fit: contain;
  border-radius: var(--radius-lg);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
</style>
