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
  border-radius: 20px;
  width: 90%;
  border: 1px solid rgba(255, 255, 255, 0.8);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
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
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
  border-radius: 12px;
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
  padding: 0;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.members-modal {
  max-width: 480px;
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
  color: var(--text-light);
  font-size: 14px;
}

.members-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
  transition: all 0.2s;
}

.member-item:hover {
  background: rgba(255, 107, 157, 0.05);
}

.member-item:last-child {
  border-bottom: none;
}

.member-avatar {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: linear-gradient(135deg, #4ecdc4, #44a08d);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  color: white;
  flex-shrink: 0;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(78, 205, 196, 0.3);
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
  font-weight: 600;
  color: var(--text-primary);
}

.member-username {
  font-size: 12px;
  color: var(--text-light);
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
  background: #4ecdc4;
  display: inline-block;
  box-shadow: 0 0 8px rgba(78, 205, 196, 0.5);
}

.offline-dot-small {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--text-light);
  display: inline-block;
}

.members-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
}

.members-stats {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: var(--text-secondary);
}

.btn-close {
  padding: 10px 24px;
  background: linear-gradient(135deg, #ff6b9d, #ff9f43);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
}

.btn-close:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(255, 107, 157, 0.4);
}

@media (max-width: 768px) {
  .members-modal {
    max-width: 90vw;
    max-height: 85vh;
  }

  .member-item {
    padding: 12px 16px;
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

