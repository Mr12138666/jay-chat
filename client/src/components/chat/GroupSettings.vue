<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import type { ChatSession, SessionMember } from '../../api/chat'
import { updateGroupInfo, inviteMembers, removeMember, leaveGroup, dissolveGroup, transferOwner } from '../../api/chat'
import { getUsers, type ContactUser } from '../../api/chat'
import AddBotModal from './AddBotModal.vue'

const props = defineProps<{
  show: boolean
  session: ChatSession | null
  members: SessionMember[]
  currentUserId: number | undefined
}>()

const emit = defineEmits<{
  close: []
  update: []
  kickMember: [userId: number]
}>()

const loading = ref(false)
const activeTab = ref<'info' | 'members' | 'invite' | 'bots'>('info')

// AI 机器人管理
const showAddBotModal = ref(false)

// 编辑群信息
const editGroupName = ref('')
const editNotice = ref('')
const isEditing = ref(false)

// 邀请成员
const allUsers = ref<ContactUser[]>([])
const loadingUsers = ref(false)
const selectedUserIds = ref<number[]>([])
const inviting = ref(false)

// 判断是否是群主
const isOwner = computed(() => {
  return props.session?.ownerId === props.currentUserId
})

// 监听弹窗打开
watch(() => props.show, async (newVal) => {
  if (newVal && props.session) {
    editGroupName.value = props.session.name || ''
    editNotice.value = props.session.notice || ''
    activeTab.value = 'info'
    selectedUserIds.value = []
    isEditing.value = false
  }
})

// 加载用户列表（用于邀请）
const loadUsers = async () => {
  if (allUsers.value.length > 0) return
  loadingUsers.value = true
  try {
    const users = await getUsers()
    // 过滤掉已在群中的成员
    const memberIds = props.members.map(m => m.userId)
    allUsers.value = users.filter(u => !memberIds.includes(u.userId))
  } catch (e) {
    console.error('加载用户列表失败', e)
  } finally {
    loadingUsers.value = false
  }
}

// 切换到邀请标签
const switchToInvite = async () => {
  activeTab.value = 'invite'
  await loadUsers()
}

// 保存群信息
const saveGroupInfo = async () => {
  if (!props.session) return
  if (!editGroupName.value.trim()) {
    alert('群名称不能为空')
    return
  }
  loading.value = true
  try {
    await updateGroupInfo(props.session.id, {
      groupName: editGroupName.value.trim(),
      notice: editNotice.value.trim() || undefined
    })
    isEditing.value = false
    emit('update')
  } catch (e: any) {
    alert(e.message || '更新失败')
  } finally {
    loading.value = false
  }
}

// 邀请成员
const doInvite = async () => {
  if (!props.session || selectedUserIds.value.length === 0) return
  inviting.value = true
  try {
    await inviteMembers(props.session.id, selectedUserIds.value)
    selectedUserIds.value = []
    emit('update')
    activeTab.value = 'members'
  } catch (e: any) {
    alert(e.message || '邀请失败')
  } finally {
    inviting.value = false
  }
}

// 踢出成员
const doRemoveMember = async (userId: number) => {
  if (!props.session) return
  if (!confirm('确定要将该成员移出群聊吗？')) return
  try {
    await removeMember(props.session.id, userId)
    emit('kickMember', userId)
  } catch (e: any) {
    alert(e.message || '操作失败')
  }
}

// 退群
const doLeaveGroup = async () => {
  if (!props.session) return
  if (!confirm('确定要退出该群聊吗？')) return
  try {
    await leaveGroup(props.session.id)
    emit('close')
    window.location.reload()
  } catch (e: any) {
    alert(e.message || '操作失败')
  }
}

// 解散群
const doDissolveGroup = async () => {
  if (!props.session) return
  if (!confirm('确定要解散该群聊吗？此操作不可恢复！')) return
  try {
    await dissolveGroup(props.session.id)
    emit('close')
    window.location.reload()
  } catch (e: any) {
    alert(e.message || '操作失败')
  }
}

// 转让群主
const doTransferOwner = async (newOwnerId: number) => {
  if (!props.session) return
  if (!confirm('确定要将群主转让给该成员吗？')) return
  try {
    await transferOwner(props.session.id, newOwnerId)
    emit('update')
  } catch (e: any) {
    alert(e.message || '操作失败')
  }
}
</script>

<template>
  <div v-if="show" class="modal-overlay" @click.self="emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h2>群设置</h2>
        <button class="close-btn" @click="emit('close')">×</button>
      </div>

      <div class="tabs">
        <button
          :class="{ active: activeTab === 'info' }"
          @click="activeTab = 'info'"
        >群信息</button>
        <button
          :class="{ active: activeTab === 'members' }"
          @click="activeTab = 'members'"
        >成员列表</button>
        <button
          :class="{ active: activeTab === 'invite' }"
          @click="switchToInvite"
        >邀请成员</button>
        <button
          :class="{ active: activeTab === 'bots' }"
          @click="activeTab = 'bots'"
        >AI 机器人</button>
      </div>

      <div class="modal-body">
        <!-- 群信息 -->
        <div v-if="activeTab === 'info'" class="tab-content">
          <div class="info-item">
            <label>群名称</label>
            <div v-if="!isEditing" class="info-value">
              <span>{{ session?.name }}</span>
              <button v-if="isOwner" class="edit-btn" @click="isEditing = true">编辑</button>
            </div>
            <div v-else class="edit-form">
              <input v-model="editGroupName" type="text" placeholder="群名称" />
            </div>
          </div>

          <div v-if="isEditing" class="info-item">
            <label>群公告</label>
            <textarea v-model="editNotice" placeholder="群公告（选填）" rows="3"></textarea>
          </div>

          <div v-if="isEditing" class="info-item">
            <label>群主</label>
            <span class="info-value">{{ session?.ownerId }}</span>
          </div>

          <div v-if="isEditing" class="action-btns">
            <button class="btn-primary" @click="saveGroupInfo" :disabled="loading">
              {{ loading ? '保存中...' : '保存' }}
            </button>
            <button class="btn-secondary" @click="isEditing = false">取消</button>
          </div>

          <div v-if="!isEditing" class="danger-btns">
            <button v-if="isOwner" class="btn-danger" @click="doDissolveGroup">解散群聊</button>
            <button v-else class="btn-warning" @click="doLeaveGroup">退出群聊</button>
          </div>
        </div>

        <!-- 成员列表 -->
        <div v-if="activeTab === 'members'" class="tab-content">
          <div class="member-list">
            <div v-for="member in members" :key="member.userId" class="member-item">
              <div class="member-info">
                <img v-if="member.avatar" :src="member.avatar" class="member-avatar" />
                <div v-else class="member-avatar-placeholder">
                  {{ member.nickname?.[0] || member.username[0] }}
                </div>
                <div class="member-name">
                  <span>{{ member.nickname || member.username }}</span>
                  <span v-if="member.userId === session?.ownerId" class="owner-tag">群主</span>
                </div>
              </div>
              <div v-if="isOwner && member.userId !== session?.ownerId" class="member-actions">
                <button class="btn-sm" @click="doRemoveMember(member.userId)">移除</button>
                <button class="btn-sm btn-primary" @click="doTransferOwner(member.userId)">转让群主</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 邀请成员 -->
        <div v-if="activeTab === 'invite'" class="tab-content">
          <div v-if="loadingUsers" class="loading">加载中...</div>
          <div v-else-if="allUsers.length === 0" class="empty">暂无可选用户</div>
          <div v-else class="invite-list">
            <label v-for="user in allUsers" :key="user.userId" class="invite-item">
              <input type="checkbox" v-model="selectedUserIds" :value="user.userId" />
              <img v-if="user.avatar" :src="user.avatar" class="invite-avatar" />
              <div v-else class="invite-avatar-placeholder">
                {{ user.nickname?.[0] || user.username[0] }}
              </div>
              <span>{{ user.nickname || user.username }}</span>
            </label>
          </div>
          <div v-if="selectedUserIds.length > 0" class="action-btns">
            <button class="btn-primary" @click="doInvite" :disabled="inviting">
              {{ inviting ? '邀请中...' : `邀请 ${selectedUserIds.length} 位成员` }}
            </button>
          </div>
        </div>

        <!-- AI 机器人 -->
        <div v-if="activeTab === 'bots'" class="tab-content">
          <div class="bot-manage-section">
            <p class="bot-intro">管理群聊中的 AI 机器人，添加后可在群聊中 @机器人 进行对话</p>
            <button class="btn-primary" @click="showAddBotModal = true">管理 AI 机器人</button>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 添加机器人弹窗 -->
  <AddBotModal
    :show="showAddBotModal"
    :session-id="session?.id ?? null"
    @close="showAddBotModal = false"
    @update="emit('update')"
  />
</template>

<style scoped>
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
  max-width: 500px;
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

.tabs {
  display: flex;
  border-bottom: 1px solid #eee;
}

.tabs button {
  flex: 1;
  padding: 12px;
  background: none;
  border: none;
  cursor: pointer;
  color: #666;
}

.tabs button.active {
  color: #ff6b9d;
  border-bottom: 2px solid #ff6b9d;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.tab-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.info-item label {
  display: block;
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}

.info-value {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.edit-btn {
  background: none;
  border: 1px solid #ff6b9d;
  color: #ff6b9d;
  padding: 4px 12px;
  border-radius: 4px;
  cursor: pointer;
}

.edit-form input,
.info-item textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 8px;
  font-size: 14px;
}

.info-item textarea {
  resize: none;
}

.action-btns {
  display: flex;
  gap: 12px;
  margin-top: 12px;
}

.btn-primary {
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  flex: 1;
}

.btn-primary:disabled {
  opacity: 0.6;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
}

.danger-btns {
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid #eee;
}

.btn-danger {
  background: #ff4d4f;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  width: 100%;
}

.btn-warning {
  background: #faad14;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 8px;
  cursor: pointer;
  width: 100%;
}

.member-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.member-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  background: #f9f9f9;
  border-radius: 8px;
}

.member-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.member-avatar,
.member-avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 8px;
}

.member-avatar-placeholder {
  background: linear-gradient(135deg, #4ecdc4, #44a08d);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.member-name {
  display: flex;
  flex-direction: column;
}

.owner-tag {
  font-size: 10px;
  color: #ff6b9d;
  background: rgba(255, 107, 157, 0.1);
  padding: 2px 6px;
  border-radius: 4px;
}

.member-actions {
  display: flex;
  gap: 8px;
}

.btn-sm {
  padding: 6px 12px;
  border-radius: 4px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  font-size: 12px;
}

.btn-sm.btn-primary {
  background: #ff6b9d;
  border-color: #ff6b9d;
  color: white;
}

.invite-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 300px;
  overflow-y: auto;
}

.invite-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px;
  cursor: pointer;
  border-radius: 8px;
}

.invite-item:hover {
  background: #f9f9f9;
}

.invite-item input {
  width: 18px;
  height: 18px;
}

.invite-avatar,
.invite-avatar-placeholder {
  width: 36px;
  height: 36px;
  border-radius: 6px;
}

.invite-avatar-placeholder {
  background: linear-gradient(135deg, #4ecdc4, #44a08d);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 14px;
}

.loading,
.empty {
  text-align: center;
  color: #999;
  padding: 20px;
}

.bot-manage-section {
  text-align: center;
  padding: 20px 0;
}

.bot-intro {
  color: #666;
  font-size: 14px;
  margin-bottom: 16px;
}

.bot-manage-section .btn-primary {
  width: 100%;
}
</style>
