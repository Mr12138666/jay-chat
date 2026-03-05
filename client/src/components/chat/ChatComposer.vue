<script setup lang="ts">
import { computed, ref } from 'vue'
import EmojiPicker from './EmojiPicker.vue'

const props = defineProps<{
  modelValue: string
  wsConnected: boolean
  uploadingImage: boolean
  showEmojiPicker: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
  send: []
  toggleEmoji: []
  closeEmoji: []
  selectEmoji: [code: string]
  imageSelected: [file: File]
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
  </footer>
</template>

<style scoped>
.chat-input {
  padding: 16px 20px;
  border-top: 1px solid #333;
  background: #252525;
  display: flex;
  gap: 12px;
  align-items: center;
}

.input-wrapper {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  position: relative;
}

.image-btn,
.emoji-btn {
  width: 40px;
  height: 40px;
  padding: 0;
  background: #1a1a1a;
  border: 1px solid #333;
  border-radius: 6px;
  color: #e0e0e0;
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  flex-shrink: 0;
}

.image-btn:hover:not(:disabled),
.emoji-btn:hover:not(:disabled) {
  background: #2a2a2a;
  border-color: #667eea;
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

.send-btn:hover:not(:disabled) {
  opacity: 0.9;
}
</style>

