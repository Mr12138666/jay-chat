<script setup lang="ts">
import type { SessionMember, SessionMemberStats } from '../../api/chat'

defineProps<{
  show: boolean
  loading: boolean
  members: SessionMember[]
  memberStats: SessionMemberStats | null
  isUserOnline: (userId: number) => boolean
}>()

const emit = defineEmits<{
  close: []
}>()
</script>

<template>
  <div v-if="show" class="modal-overlay" @click="emit('close')">
    <div class="modal-content members-modal" @click.stop>
      <div class="modal-header">
        <h3>群成员</h3>
        <button @click="emit('close')" class="modal-close-btn">x</button>
      </div>
      <div class="modal-body members-list-body">
        <div v-if="loading" class="loading-members">加载中...</div>
        <div v-else-if="members.length === 0" class="empty-members">暂无成员</div>
        <ul v-else class="members-list">
          <li v-for="member in members" :key="member.userId" class="member-item">
            <div class="member-avatar">{{ member.nickname?.[0] || 'U' }}</div>
            <div class="member-info">
              <div class="member-name-wrapper">
                <span class="member-name">{{ member.nickname }}</span>
                <span class="member-username">@{{ member.username }}</span>
              </div>
            </div>
            <div class="member-status">
              <span v-if="isUserOnline(member.userId)" class="online-indicator" title="在线">
                <span class="online-dot-small"></span>
              </span>
              <span v-else class="offline-indicator" title="离线">
                <span class="offline-dot-small"></span>
              </span>
            </div>
          </li>
        </ul>
      </div>
      <div class="modal-footer members-footer">
        <div class="members-stats">
          <span>总人数: {{ memberStats?.totalMembers || 0 }}</span>
          <span>在线: {{ memberStats?.onlineMembers || 0 }}</span>
        </div>
        <button @click="emit('close')" class="btn-close">关闭</button>
      </div>
    </div>
  </div>
</template>

<style scoped>
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
  border: 1px solid #333;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
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
  font-size: 18px;
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

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #333;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.members-modal {
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.members-list-body {
  flex: 1;
  overflow-y: auto;
  padding: 0;
  min-height: 300px;
  max-height: 500px;
}

.loading-members,
.empty-members {
  padding: 40px 24px;
  text-align: center;
  color: #999;
}

.members-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 24px;
  border-bottom: 1px solid #333;
  transition: background 0.2s;
}

.member-item:hover {
  background: #2a2a2a;
}

.member-item:last-child {
  border-bottom: none;
}

.member-avatar {
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
  font-size: 16px;
}

.member-info {
  flex: 1;
  min-width: 0;
}

.member-name-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.member-name {
  font-size: 14px;
  font-weight: 500;
  color: #fff;
}

.member-username {
  font-size: 12px;
  color: #999;
}

.member-status {
  flex-shrink: 0;
}

.online-indicator,
.offline-indicator {
  display: flex;
  align-items: center;
  justify-content: center;
}

.online-dot-small {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #4caf50;
  display: inline-block;
}

.offline-dot-small {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #666;
  display: inline-block;
}

.members-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid #333;
}

.members-stats {
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: #999;
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

@media (max-width: 768px) {
  .members-modal {
    max-width: 90vw;
    max-height: 85vh;
  }

  .member-item {
    padding: 10px 16px;
  }

  .members-footer {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .members-stats {
    justify-content: space-between;
  }

  .btn-close {
    width: 100%;
  }
}
</style>

