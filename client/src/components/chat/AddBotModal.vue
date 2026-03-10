<script setup lang="ts">
import { ref, watch } from 'vue'
import { getMyBots, getSessionBots, addBotToSession, removeBotFromSession, type AIBot } from '../../api/aiBot'

const props = defineProps<{
  show: boolean
  sessionId: number | null
}>()

const emit = defineEmits<{
  close: []
  update: []
}>()

const myBots = ref<AIBot[]>([])
const sessionBots = ref<AIBot[]>([])
const loading = ref(false)
const loadingBots = ref(false)

// 加载机器人列表
const loadBots = async () => {
  if (!props.sessionId) return

  loadingBots.value = true
  try {
    const [myBotsData, sessionBotsData] = await Promise.all([
      getMyBots(),
      getSessionBots(props.sessionId)
    ])
    myBots.value = myBotsData
    sessionBots.value = sessionBotsData
  } catch (e) {
    console.error('加载机器人列表失败', e)
  } finally {
    loadingBots.value = false
  }
}

// 检查机器人是否已在群聊中
const isBotInSession = (botId: number) => {
  return sessionBots.value.some(b => b.id === botId)
}

// 添加机器人到群聊
const addBot = async (bot: AIBot) => {
  if (!props.sessionId) return

  loading.value = true
  try {
    await addBotToSession(bot.id, props.sessionId)
    await loadBots()
    emit('update')
  } catch (e: any) {
    alert(e.message || '添加失败')
  } finally {
    loading.value = false
  }
}

// 从群聊移除机器人
const removeBot = async (bot: AIBot) => {
  if (!props.sessionId) return
  if (!confirm(`确定要从群聊中移除机器人 "${bot.name}" 吗？`)) return

  loading.value = true
  try {
    await removeBotFromSession(bot.id, props.sessionId)
    await loadBots()
    emit('update')
  } catch (e: any) {
    alert(e.message || '移除失败')
  } finally {
    loading.value = false
  }
}

// 获取未添加到群聊的机器人
const availableBots = () => {
  return myBots.value.filter(bot => !isBotInSession(bot.id))
}

// 监听弹窗打开
watch(() => props.show, async (newVal) => {
  if (newVal) {
    await loadBots()
  }
})
</script>

<template>
  <div v-if="show" class="modal-overlay" @click.self="emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h2>添加 AI 机器人到群聊</h2>
        <button class="close-btn" @click="emit('close')">×</button>
      </div>

      <div class="modal-body">
        <div v-if="loadingBots" class="loading">加载中...</div>
        <template v-else>
          <!-- 已在群聊中的机器人 -->
          <div class="section-title">群聊中的机器人 ({{ sessionBots.length }})</div>
          <div v-if="sessionBots.length === 0" class="empty-tip">暂无机器人</div>
          <div v-else class="bot-list">
            <div v-for="bot in sessionBots" :key="bot.id" class="bot-item in-session">
              <div class="bot-avatar">
                <img v-if="bot.avatar" :src="bot.avatar" />
                <div v-else class="avatar-placeholder">{{ bot.name[0] }}</div>
              </div>
              <div class="bot-info">
                <div class="bot-name">{{ bot.name }}</div>
                <div class="bot-desc">{{ bot.description || '暂无描述' }}</div>
              </div>
              <button class="btn-sm btn-danger" @click="removeBot(bot)" :disabled="loading">移除</button>
            </div>
          </div>

          <!-- 可添加的机器人 -->
          <div class="section-title" v-if="availableBots().length > 0">
            可添加的机器人 ({{ availableBots().length }})
          </div>
          <div v-if="availableBots().length === 0 && sessionBots.length > 0" class="empty-tip">
            你的所有机器人已添加到此群聊
          </div>
          <div v-else-if="availableBots().length === 0 && sessionBots.length === 0" class="empty-tip">
            暂无可添加的机器人，请先创建机器人
          </div>
          <div v-else class="bot-list">
            <div v-for="bot in availableBots()" :key="bot.id" class="bot-item">
              <div class="bot-avatar">
                <img v-if="bot.avatar" :src="bot.avatar" />
                <div v-else class="avatar-placeholder">{{ bot.name[0] }}</div>
              </div>
              <div class="bot-info">
                <div class="bot-name">{{ bot.name }}</div>
                <div class="bot-desc">{{ bot.description || '暂无描述' }}</div>
              </div>
              <button class="btn-sm btn-primary" @click="addBot(bot)" :disabled="loading">添加</button>
            </div>
          </div>

          <!-- 使用说明 -->
          <div class="tips">
            <h4>使用说明</h4>
            <ul>
              <li>添加机器人后，在群聊中 @机器人名称 即可触发 AI 回复</li>
              <li>例如：@小助手 你好</li>
              <li>只有机器人创建者可以添加或移除机器人</li>
            </ul>
          </div>
        </template>
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

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
}

.loading,
.empty-tip {
  text-align: center;
  color: #999;
  padding: 20px;
}

.section-title {
  font-weight: 500;
  margin-bottom: 12px;
  margin-top: 16px;
  color: #333;
}

.section-title:first-child {
  margin-top: 0;
}

.bot-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.bot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
}

.bot-item.in-session {
  background: #f0f7ff;
}

.bot-avatar {
  flex-shrink: 0;
}

.bot-avatar img,
.avatar-placeholder {
  width: 40px;
  height: 40px;
  border-radius: 8px;
}

.avatar-placeholder {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
}

.bot-info {
  flex: 1;
  min-width: 0;
}

.bot-name {
  font-weight: 500;
  font-size: 14px;
  margin-bottom: 2px;
}

.bot-desc {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.btn-sm {
  padding: 6px 12px;
  border-radius: 4px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  font-size: 12px;
  flex-shrink: 0;
}

.btn-sm.btn-primary {
  background: #667eea;
  border-color: #667eea;
  color: white;
}

.btn-sm.btn-primary:hover {
  background: #5a6fd6;
}

.btn-sm.btn-danger {
  background: #ff4d4f;
  border-color: #ff4d4f;
  color: white;
}

.btn-sm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.tips {
  margin-top: 20px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
  font-size: 12px;
  color: #666;
}

.tips h4 {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #333;
}

.tips ul {
  margin: 0;
  padding-left: 20px;
}

.tips li {
  margin-bottom: 4px;
}
</style>
