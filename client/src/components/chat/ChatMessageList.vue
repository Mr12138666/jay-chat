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
  replyMessage: [message: { id: number; sender: string; content: string }]
  recallMessage: [messageId: number]
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
          <!-- 撤回按钮：仅消息发送者可见，且消息未被撤回 -->
          <button
            v-if="msg.senderId === currentUserId && !msg.recalled"
            class="recall-btn"
            @click.stop="emit('recallMessage', msg.id)"
            title="撤回"
          >↙</button>
          <button class="reply-btn" @click.stop="emit('replyMessage', { id: msg.id, sender: msg.sender, content: msg.content })" title="回复">↩</button>
        </div>
        <!-- 引用消息显示 -->
        <div v-if="msg.replyToId" class="reply-quote">
          <span class="reply-quote-label">回复 {{ msg.replyToNickname }}:</span>
          <span class="reply-quote-content">{{ msg.replyToContent }}</span>
        </div>
        <div
          v-if="msg.recalled"
          class="message-content recalled"
        >
          <span class="recalled-text">你撤回了一条消息</span>
        </div>
        <div
          v-else
          class="message-content"
          :class="{ 'image-content': msg.contentType === 'image' }"
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
  color: var(--text-light);
  font-size: 15px;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 18px;
  min-height: min-content;
}

.message {
  display: flex;
  flex-direction: row;
  gap: 14px;
  max-width: 70%;
  min-width: 0;
  width: fit-content;
  align-items: flex-start;
}

/* 移动端消息宽度优化 */
@media (max-width: 768px) {
  .message {
    max-width: 85%;
  }
}

@media (max-width: 480px) {
  .message {
    max-width: 90%;
  }
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
  gap: 6px;
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
  width: 44px;
  height: 44px;
  border-radius: var(--radius-md);
  flex-shrink: 0;
  cursor: pointer;
  overflow: hidden;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff9f43 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.3);
  transition: transform 0.2s;
}

.message-avatar:hover {
  transform: scale(1.08);
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
  background: linear-gradient(135deg, #4ecdc4, #44a08d);
  color: white;
  font-weight: 600;
  font-size: 16px;
}

.message-meta {
  display: flex;
  gap: 10px;
  align-items: center;
}

.reply-btn {
  background: none;
  border: none;
  color: var(--text-light);
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  opacity: 0;
  transition: all 0.2s;
}

.message:hover .reply-btn {
  opacity: 1;
}

.reply-btn:hover {
  color: #ff6b9d;
  background: rgba(255, 107, 157, 0.1);
}

.recall-btn {
  background: none;
  border: none;
  color: var(--text-light);
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  opacity: 0;
  transition: all 0.2s;
}

.message:hover .recall-btn {
  opacity: 1;
}

.recall-btn:hover {
  color: #ff6b9d;
  background: rgba(255, 107, 157, 0.1);
}

.recalled {
  background: rgba(0, 0, 0, 0.03) !important;
  border: 1px dashed rgba(0, 0, 0, 0.1) !important;
}

.recalled-text {
  color: var(--text-light);
  font-size: 12px;
  font-style: italic;
}

.reply-quote {
  padding: 8px 12px;
  background: rgba(255, 107, 157, 0.08);
  border-left: 3px solid #ff6b9d;
  border-radius: var(--radius-sm);
  margin-bottom: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.reply-quote-label {
  color: #ff6b9d;
  font-size: 11px;
  font-weight: 500;
}

.reply-quote-content {
  color: var(--text-secondary);
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sender {
  font-weight: 600;
  color: #ff6b9d;
  font-size: 14px;
}

.time {
  font-size: 12px;
  color: var(--text-light);
}

.message-content {
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(10px);
  border-radius: var(--radius-lg);
  font-size: 14px;
  line-height: 1.5;
  word-wrap: break-word;
  word-break: break-word;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  display: inline-block;
  max-width: 100%;
  width: fit-content;
  min-width: 0;
  white-space: pre-wrap;
  overflow: hidden;
  box-sizing: border-box;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

/* 图片消息的特殊样式 */
.message-content.image-content {
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: fit-content;
  max-width: 100%;
  background: rgba(255, 255, 255, 0.95) !important;
  border: 1px solid rgba(255, 107, 157, 0.2);
}

/* 自己发送的消息气泡 - 粉橙渐变 */
.message.own-message .message-content {
  background: linear-gradient(135deg, #ff6b9d 0%, #ff9f43 100%);
  color: white;
  border-bottom-right-radius: 6px;
  border: none;
}

.message:not(.own-message) .message-content {
  border-bottom-left-radius: 6px;
}

.message-image {
  max-width: calc(100% - 0px);
  max-height: 400px;
  width: auto;
  height: auto;
  border-radius: var(--radius-md);
  cursor: pointer;
  display: block;
  object-fit: contain;
  box-sizing: border-box;
  margin: 0;
}

/* PC端图片尺寸优化 */
@media (min-width: 769px) {
  .message-image {
    max-width: min(400px, 100%);
    max-height: 500px;
  }
}

/* 移动端图片尺寸优化 */
@media (max-width: 768px) {
  .message-image {
    max-width: 100%;
    max-height: 300px;
  }
}

@media (max-width: 480px) {
  .message-image {
    max-width: 100%;
    max-height: 250px;
  }
}
</style>

