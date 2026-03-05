<script setup lang="ts">
import { getMessageBackgroundColor } from '../../utils/color'
import type { UiChatMessage } from '../../types/chat-ui'

defineProps<{
  loading: boolean
  messages: UiChatMessage[]
  currentUserId?: number
}>()

const emit = defineEmits<{
  openUserDetail: [userId: number]
  openImagePreview: [imageUrl: string]
}>()
</script>

<template>
  <div v-if="loading" class="loading">加载中...</div>
  <div v-else-if="messages.length === 0" class="empty-messages">暂无消息</div>
  <div v-else class="messages-list">
    <div
      v-for="msg in messages"
      :key="msg.id"
      class="message"
      :class="{ 'own-message': msg.senderId === currentUserId }"
    >
      <div
        class="message-avatar"
        @click="msg.senderId && emit('openUserDetail', msg.senderId)"
        :title="`点击查看 ${msg.sender} 的详情`"
      >
        <img
          v-if="msg.senderAvatar"
          :src="msg.senderAvatar"
          :alt="msg.sender"
          class="avatar-img"
          @error="msg.senderAvatar = null"
        />
        <div v-else class="avatar-placeholder">
          {{ msg.sender?.[0] || 'U' }}
        </div>
      </div>
      <div class="message-body">
        <div class="message-meta">
          <span class="sender">{{ msg.sender }}</span>
          <span class="time">{{ msg.time }}</span>
        </div>
        <div
          class="message-content"
          :style="msg.senderId !== currentUserId ? {
            backgroundColor: getMessageBackgroundColor(msg.senderId),
            color: '#ffffff'
          } : {}"
        >
          <img
            v-if="msg.contentType === 'image'"
            :src="msg.content"
            :alt="msg.sender + ' 的图片'"
            class="message-image"
            @click="emit('openImagePreview', msg.content)"
            @error="(e) => { const target = e.target as HTMLImageElement; if (target) target.style.display = 'none' }"
          />
          <span v-else>{{ msg.content }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.loading, .empty-messages {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
  font-size: 14px;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-height: min-content;
}

.message {
  display: flex;
  flex-direction: row;
  gap: 12px;
  max-width: 70%;
  min-width: 0;
  width: fit-content;
  align-items: flex-start;
}

.message.own-message {
  flex-direction: row-reverse;
  align-self: flex-end;
  margin-left: auto;
  align-items: flex-end;
  justify-content: flex-end;
}

.message-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
  justify-content: flex-start;
  align-items: flex-start;
}

.message.own-message .message-body {
  flex: 0 1 auto;
  align-items: flex-end;
  max-width: 100%;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  flex-shrink: 0;
  cursor: pointer;
  overflow: hidden;
  background: #333;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  font-weight: 600;
  font-size: 16px;
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
  padding: 10px 14px;
  background: #2a2a2a;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
  word-break: break-word;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
  display: inline-block;
  max-width: 100%;
  width: fit-content;
  min-width: 0;
  white-space: pre-wrap;
}

.message.own-message .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-bottom-right-radius: 4px;
}

.message:not(.own-message) .message-content {
  border-bottom-left-radius: 4px;
}

.message-image {
  max-width: 300px;
  max-height: 400px;
  border-radius: 8px;
  cursor: pointer;
  display: block;
  object-fit: contain;
}
</style>

