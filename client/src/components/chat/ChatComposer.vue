<script setup lang="ts">
import { computed, ref } from 'vue'
import EmojiPicker from './EmojiPicker.vue'

const props = defineProps<{
  modelValue: string
  wsConnected: boolean
  uploadingImage: boolean
  showEmojiPicker: boolean
  replyingTo: { id: number; sender: string; content: string } | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
  toggleEmoji: []
  closeEmoji: []
  selectEmoji: [code: string]
  imageSelected: [file: File]
  cancelReply: []
}>()

const imageFileInput = ref<HTMLInputElement | null>(null)

const inputValue = computed({
  get: () => props.modelValue,
  set: (value: string) => emit('update:modelValue', value)
})

const openImageFileDialog = () => {
  imageFileInput.value?.click()
}

const handleImageFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  emit('imageSelected', file)
  target.value = ''
}
</script>

<template>
  <footer class="chat-input">
    <!-- 回复消息显示 -->
    <div v-if="replyingTo" class="reply-preview">
      <div class="reply-content">
        <span class="reply-label">回复 {{ replyingTo.sender }}:</span>
        <span class="reply-text">{{ replyingTo.content }}</span>
      </div>
      <button class="reply-cancel" @click="emit('cancelReply')" title="取消回复">×</button>
    </div>
    <div class="input-row">
      <div class="input-wrapper">
        <button
          @click="openImageFileDialog"
          class="image-btn"
          type="button"
          :disabled="!wsConnected || uploadingImage"
          title="上传图片"
        >
          📷
        </button>
        <input
          ref="imageFileInput"
          type="file"
          accept="image/*"
          style="display: none"
          @change="handleImageFileChange"
        />
        <button
          @click="emit('toggleEmoji')"
          class="emoji-btn"
          type="button"
          :disabled="!wsConnected"
          title="表情"
        >
          😊
        </button>
        <EmojiPicker
          v-if="showEmojiPicker"
          :show="showEmojiPicker"
          @select="emit('selectEmoji', $event)"
          @close="emit('closeEmoji')"
        />
        <input
          v-model="inputValue"
          @keyup.enter="emit('send')"
          @click="emit('closeEmoji')"
          type="text"
          placeholder="输入消息..."
          class="message-input"
          :disabled="!wsConnected"
        />
      </div>
      <button
        @click="emit('send')"
        class="send-btn"
        :disabled="!wsConnected || !modelValue.trim()"
      >
        发送
      </button>
    </div>
  </footer>
</template>

<style scoped>
.chat-input {
  padding: 20px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.05);
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(20px);
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-items: stretch;
}

.reply-preview {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: rgba(255, 107, 157, 0.08);
  border-left: 3px solid #ff6b9d;
  border-radius: 12px;
}

.reply-content {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  overflow: hidden;
}

.reply-label {
  color: #ff6b9d;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.reply-text {
  color: #636e72;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.reply-cancel {
  background: none;
  border: none;
  color: #b2bec3;
  font-size: 18px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 8px;
  transition: all 0.2s;
}

.reply-cancel:hover {
  color: #ff6b9d;
  background: rgba(255, 107, 157, 0.1);
}

.input-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
  position: relative;
}

.image-btn,
.emoji-btn {
  width: 44px;
  height: 44px;
  padding: 0;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid rgba(0, 0, 0, 0.05);
  border-radius: 12px;
  color: #636e72;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.image-btn:hover:not(:disabled),
.emoji-btn:hover:not(:disabled) {
  background: rgba(255, 107, 157, 0.1);
  border-color: rgba(255, 107, 157, 0.3);
  color: #ff6b9d;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(255, 107, 157, 0.2);
}

.image-btn:disabled,
.emoji-btn:disabled,
.message-input:disabled,
.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.message-input {
  flex: 1;
  padding: 14px 18px;
  background: rgba(255, 255, 255, 0.9);
  border: 2px solid transparent;
  border-radius: 16px;
  color: #2d3436;
  font-size: 15px;
  transition: all 0.3s;
  background-image: linear-gradient(#fff, #fff), linear-gradient(135deg, #ff6b9d, #4ecdc4);
  background-origin: border-box;
  background-clip: padding-box, border-box;
}

.message-input:focus {
  outline: none;
  box-shadow: 0 0 20px rgba(255, 107, 157, 0.15);
}

.message-input::placeholder {
  color: #b2bec3;
}

.send-btn {
  padding: 12px 20px;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff9f43 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s;
  box-shadow: 0 4px 16px rgba(255, 107, 157, 0.3);
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(255, 107, 157, 0.4);
}
</style>

