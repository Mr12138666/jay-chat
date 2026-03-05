<template>
  <div class="emoji-picker" v-if="show" @click.stop>
    <div class="emoji-picker-header">
      <span>选择表情</span>
      <button class="close-btn" @click="close">×</button>
    </div>
    <div class="emoji-picker-content">
      <div 
        v-for="item in emojiList" 
        :key="item.code"
        class="emoji-item"
        :title="item.code"
        @click="selectEmoji(item.code)"
      >
        {{ item.emoji }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAllEmojis } from '../../utils/emoji'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  (e: 'select', code: string): void
  (e: 'close'): void
}>()

const emojiList = ref<Array<{ code: string; emoji: string }>>([])

onMounted(() => {
  emojiList.value = getAllEmojis()
})

const selectEmoji = (code: string) => {
  emit('select', code)
}

const close = () => {
  emit('close')
}
</script>

<style scoped>
.emoji-picker {
  position: absolute;
  bottom: 100%;
  left: 0;
  width: 320px;
  max-height: 300px;
  background: white;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  overflow: hidden;
  margin-bottom: 8px;
}

.emoji-picker-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
  background: #f5f5f5;
  font-size: 14px;
  font-weight: 500;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.close-btn:hover {
  color: #333;
}

.emoji-picker-content {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
  padding: 12px;
  max-height: 240px;
  overflow-y: auto;
}

.emoji-item {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
  user-select: none;
}

.emoji-item:hover {
  background-color: #f0f0f0;
}

.emoji-item:active {
  background-color: #e0e0e0;
}

/* 滚动条样式 */
.emoji-picker-content::-webkit-scrollbar {
  width: 6px;
}

.emoji-picker-content::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 3px;
}

.emoji-picker-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.emoji-picker-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .emoji-picker {
    width: 100%;
    max-width: 320px;
  }
  
  .emoji-picker-content {
    grid-template-columns: repeat(6, 1fr);
  }
  
  .emoji-item {
    width: 40px;
    height: 40px;
    font-size: 24px;
  }
}
</style>
