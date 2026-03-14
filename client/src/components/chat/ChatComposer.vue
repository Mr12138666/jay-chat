<script setup lang="ts">
import { computed, ref } from 'vue'
import EmojiPicker from './EmojiPicker.vue'

interface MentionBotItem {
  id: number
  name: string
  avatar?: string | null
}

const props = defineProps<{
  modelValue: string
  wsConnected: boolean
  uploadingImage: boolean
  showEmojiPicker: boolean
  replyingTo: { id: number; sender: string; content: string } | null
  mentionBots?: MentionBotItem[]
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
const messageInputRef = ref<HTMLInputElement | null>(null)

const showMentionList = ref(false)
const mentionQuery = ref('')
const mentionStart = ref(-1)
const mentionEnd = ref(-1)
const activeMentionIndex = ref(0)

const mentionCandidates = computed(() => {
  const bots = props.mentionBots || []
  const q = mentionQuery.value.trim().toLowerCase()
  if (!q) {
    return bots.slice(0, 8)
  }

  return bots
    .filter(bot => bot.name.toLowerCase().includes(q))
    .sort((a, b) => {
      const an = a.name.toLowerCase()
      const bn = b.name.toLowerCase()
      const aExact = an === q ? 0 : 1
      const bExact = bn === q ? 0 : 1
      if (aExact !== bExact) return aExact - bExact

      const aPrefix = an.startsWith(q) ? 0 : 1
      const bPrefix = bn.startsWith(q) ? 0 : 1
      if (aPrefix !== bPrefix) return aPrefix - bPrefix

      const ai = an.indexOf(q)
      const bi = bn.indexOf(q)
      if (ai !== bi) return ai - bi
      return a.name.localeCompare(b.name)
    })
    .slice(0, 8)
})

const getMentionNameParts = (name: string) => {
  const q = mentionQuery.value.trim()
  if (!q) {
    return { before: name, match: '', after: '' }
  }

  const lowerName = name.toLowerCase()
  const lowerQ = q.toLowerCase()
  const idx = lowerName.indexOf(lowerQ)
  if (idx === -1) {
    return { before: name, match: '', after: '' }
  }

  return {
    before: name.slice(0, idx),
    match: name.slice(idx, idx + q.length),
    after: name.slice(idx + q.length)
  }
}

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

const resetMentionState = () => {
  showMentionList.value = false
  mentionQuery.value = ''
  mentionStart.value = -1
  mentionEnd.value = -1
  activeMentionIndex.value = 0
}

const updateMentionState = () => {
  const inputEl = messageInputRef.value
  if (!inputEl) {
    resetMentionState()
    return
  }

  const value = inputValue.value
  const cursor = inputEl.selectionStart ?? value.length
  const left = value.slice(0, cursor)

  const atIdx = left.lastIndexOf('@')
  if (atIdx === -1) {
    resetMentionState()
    return
  }

  const prevChar = atIdx > 0 ? left.charAt(atIdx - 1) : ' '
  if (!/\s/.test(prevChar)) {
    resetMentionState()
    return
  }

  const typed = left.slice(atIdx + 1)
  if (/\s/.test(typed)) {
    resetMentionState()
    return
  }

  mentionStart.value = atIdx
  mentionEnd.value = cursor
  mentionQuery.value = typed
  showMentionList.value = mentionCandidates.value.length > 0
  activeMentionIndex.value = 0
}

const handleInput = () => {
  updateMentionState()
}

const insertMentionBot = (bot: MentionBotItem) => {
  const value = inputValue.value
  if (mentionStart.value < 0 || mentionEnd.value < 0) {
    return
  }

  const before = value.slice(0, mentionStart.value)
  const after = value.slice(mentionEnd.value)
  const mentionText = `@${bot.name} `
  const nextValue = `${before}${mentionText}${after}`
  emit('update:modelValue', nextValue)

  resetMentionState()

  requestAnimationFrame(() => {
    const inputEl = messageInputRef.value
    if (!inputEl) return
    const nextCursor = before.length + mentionText.length
    inputEl.focus()
    inputEl.setSelectionRange(nextCursor, nextCursor)
  })
}

const handleKeydown = (event: KeyboardEvent) => {
  if (!showMentionList.value) {
    if (event.key === 'Enter') {
      emit('send')
    }
    return
  }

  if (event.key === 'ArrowDown') {
    event.preventDefault()
    const max = mentionCandidates.value.length
    if (max > 0) {
      activeMentionIndex.value = (activeMentionIndex.value + 1) % max
    }
    return
  }

  if (event.key === 'ArrowUp') {
    event.preventDefault()
    const max = mentionCandidates.value.length
    if (max > 0) {
      activeMentionIndex.value = (activeMentionIndex.value - 1 + max) % max
    }
    return
  }

  if (event.key === 'Enter') {
    event.preventDefault()
    const bot = mentionCandidates.value[activeMentionIndex.value]
    if (bot) {
      insertMentionBot(bot)
    }
    return
  }

  if (event.key === 'Escape') {
    event.preventDefault()
    resetMentionState()
  }
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
          ref="messageInputRef"
          @keydown="handleKeydown"
          @input="handleInput"
          @click="() => { emit('closeEmoji'); updateMentionState() }"
          type="text"
          placeholder="输入消息..."
          class="message-input"
          :disabled="!wsConnected"
        />

        <div v-if="showMentionList" class="mention-list">
          <button
            v-for="(bot, index) in mentionCandidates"
            :key="bot.id"
            class="mention-item"
            :class="{ active: index === activeMentionIndex }"
            type="button"
            @mousedown.prevent="insertMentionBot(bot)"
          >
            <img v-if="bot.avatar" :src="bot.avatar" class="mention-avatar" :alt="bot.name" />
            <span v-else class="mention-avatar placeholder">{{ bot.name[0] }}</span>
            <span class="mention-name">
              {{ getMentionNameParts(bot.name).before }}<b class="mention-hit">{{ getMentionNameParts(bot.name).match }}</b>{{ getMentionNameParts(bot.name).after }}
            </span>
          </button>
        </div>
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

.mention-list {
  position: absolute;
  left: 108px;
  right: 0;
  bottom: 52px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 10px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  max-height: 220px;
  overflow-y: auto;
  z-index: 30;
}

/* 回滚：不再对 mention 弹层做额外响应式定位改动，避免影响主布局 */

.mention-item {
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
  border: none;
  background: transparent;
  padding: 10px 12px;
  cursor: pointer;
  text-align: left;
}

.mention-item:hover,
.mention-item.active {
  background: rgba(102, 126, 234, 0.1);
}

.mention-avatar {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  object-fit: cover;
}

.mention-avatar.placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.mention-name {
  color: #2d3436;
  font-size: 13px;
}

.mention-hit {
  color: #667eea;
  font-weight: 700;
}

/* 手机端输入区域自适应 */
@media (max-width: 768px) {
  .chat-input {
    padding: 12px 12px;
  }

  .input-row {
    gap: 8px;
  }

  .image-btn,
  .emoji-btn {
    width: 36px;
    height: 36px;
    font-size: 16px;
  }

  .message-input {
    padding: 10px 12px;
    font-size: 14px;
  }

  .send-btn {
    padding: 8px 12px;
    font-size: 13px;
    min-width: 50px;
  }
}
</style>

