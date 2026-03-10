<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { getMyBots, createBot, updateBot, deleteBot, type AIBot, type CreateBotRequest, type UpdateBotRequest } from '../../api/aiBot'

const props = defineProps<{
  show: boolean
}>()

const emit = defineEmits<{
  close: []
}>()

const bots = ref<AIBot[]>([])
const loading = ref(false)
const loadingBots = ref(false)

// 创建/编辑弹窗
const showEditModal = ref(false)
const editingBot = ref<AIBot | null>(null)
const editForm = ref<CreateBotRequest>({
  name: '',
  avatar: '',
  description: '',
  systemPrompt: '',
  model: 'deepseek-chat'
})

// 加载机器人列表
const loadBots = async () => {
  loadingBots.value = true
  try {
    bots.value = await getMyBots()
  } catch (e) {
    console.error('加载机器人列表失败', e)
  } finally {
    loadingBots.value = false
  }
}

// 打开创建弹窗
const openCreateModal = () => {
  editingBot.value = null
  editForm.value = {
    name: '',
    avatar: '',
    description: '',
    systemPrompt: '',
    model: 'deepseek-chat'
  }
  showEditModal.value = true
}

// 打开编辑弹窗
const openEditModal = (bot: AIBot) => {
  editingBot.value = bot
  editForm.value = {
    name: bot.name,
    avatar: bot.avatar || '',
    description: bot.description || '',
    systemPrompt: bot.systemPrompt || '',
    model: bot.model
  }
  showEditModal.value = true
}

// 保存机器人
const saveBot = async () => {
  if (!editForm.value.name?.trim()) {
    alert('请输入机器人名称')
    return
  }

  loading.value = true
  try {
    if (editingBot.value) {
      // 更新
      const data: UpdateBotRequest = {
        name: editForm.value.name.trim(),
        avatar: editForm.value.avatar || undefined,
        description: editForm.value.description || undefined,
        systemPrompt: editForm.value.systemPrompt || undefined,
        model: editForm.value.model
      }
      await updateBot(editingBot.value.id, data)
    } else {
      // 创建
      const data: CreateBotRequest = {
        name: editForm.value.name.trim(),
        avatar: editForm.value.avatar || undefined,
        description: editForm.value.description || undefined,
        systemPrompt: editForm.value.systemPrompt || undefined,
        model: editForm.value.model
      }
      await createBot(data)
    }
    showEditModal.value = false
    await loadBots()
  } catch (e: any) {
    alert(e.message || '操作失败')
  } finally {
    loading.value = false
  }
}

// 删除机器人
const removeBot = async (bot: AIBot) => {
  if (!confirm(`确定要删除机器人 "${bot.name}" 吗？`)) {
    return
  }

  loading.value = true
  try {
    await deleteBot(bot.id)
    await loadBots()
  } catch (e: any) {
    alert(e.message || '删除失败')
  } finally {
    loading.value = false
  }
}

// 监听弹窗打开
watch(() => props.show, async (newVal) => {
  if (newVal) {
    await loadBots()
  }
})

onMounted(() => {
  if (props.show) {
    loadBots()
  }
})
</script>

<template>
  <div v-if="show" class="modal-overlay" @click.self="emit('close')">
    <div class="modal-content">
      <div class="modal-header">
        <h2>AI 机器人管理</h2>
        <button class="close-btn" @click="emit('close')">×</button>
      </div>

      <div class="modal-body">
        <div class="bot-list-header">
          <span>我的机器人 ({{ bots.length }})</span>
          <button class="btn-primary" @click="openCreateModal">+ 创建机器人</button>
        </div>

        <div v-if="loadingBots" class="loading">加载中...</div>
        <div v-else-if="bots.length === 0" class="empty">
          <p>暂无机器人</p>
          <p class="hint">点击上方按钮创建你的第一个 AI 机器人</p>
        </div>
        <div v-else class="bot-list">
          <div v-for="bot in bots" :key="bot.id" class="bot-item">
            <div class="bot-avatar">
              <img v-if="bot.avatar" :src="bot.avatar" />
              <div v-else class="avatar-placeholder">{{ bot.name[0] }}</div>
            </div>
            <div class="bot-info">
              <div class="bot-name">{{ bot.name }}</div>
              <div class="bot-desc">{{ bot.description || '暂无描述' }}</div>
            </div>
            <div class="bot-actions">
              <button class="btn-sm" @click="openEditModal(bot)">编辑</button>
              <button class="btn-sm btn-danger" @click="removeBot(bot)">删除</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 创建/编辑弹窗 -->
    <div v-if="showEditModal" class="modal-overlay" @click.self="showEditModal = false">
      <div class="edit-modal-content">
        <div class="modal-header">
          <h2>{{ editingBot ? '编辑机器人' : '创建机器人' }}</h2>
          <button class="close-btn" @click="showEditModal = false">×</button>
        </div>

        <div class="modal-body">
          <div class="form-group">
            <label>名称 *</label>
            <input v-model="editForm.name" type="text" placeholder="机器人名称" />
          </div>

          <div class="form-group">
            <label>头像 URL</label>
            <input v-model="editForm.avatar" type="text" placeholder="头像图片URL（可选）" />
          </div>

          <div class="form-group">
            <label>描述</label>
            <input v-model="editForm.description" type="text" placeholder="机器人描述（可选）" />
          </div>

          <div class="form-group">
            <label>系统提示词</label>
            <textarea v-model="editForm.systemPrompt" placeholder="设定机器人的角色和行为（可选）" rows="4"></textarea>
          </div>

          <div class="form-group">
            <label>模型</label>
            <select v-model="editForm.model">
              <option value="deepseek-chat">DeepSeek Chat</option>
              <option value="deepseek-reasoner">DeepSeek R1</option>
            </select>
          </div>

          <div class="action-btns">
            <button class="btn-primary" @click="saveBot" :disabled="loading">
              {{ loading ? '保存中...' : '保存' }}
            </button>
            <button class="btn-secondary" @click="showEditModal = false">取消</button>
          </div>
        </div>
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

.modal-content,
.edit-modal-content {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.edit-modal-content {
  max-width: 450px;
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

.bot-list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.bot-list-header span {
  font-weight: 500;
}

.loading,
.empty {
  text-align: center;
  color: #999;
  padding: 40px 20px;
}

.empty p {
  margin: 0;
}

.empty .hint {
  font-size: 12px;
  margin-top: 8px;
}

.bot-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.bot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #f9f9f9;
  border-radius: 8px;
}

.bot-avatar {
  flex-shrink: 0;
}

.bot-avatar img,
.avatar-placeholder {
  width: 48px;
  height: 48px;
  border-radius: 8px;
}

.avatar-placeholder {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 20px;
}

.bot-info {
  flex: 1;
  min-width: 0;
}

.bot-name {
  font-weight: 500;
  margin-bottom: 4px;
}

.bot-desc {
  font-size: 12px;
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.bot-actions {
  display: flex;
  gap: 8px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.btn-primary:disabled {
  opacity: 0.6;
}

.btn-secondary {
  background: #f5f5f5;
  color: #666;
  border: none;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
}

.btn-sm {
  padding: 6px 12px;
  border-radius: 4px;
  border: 1px solid #ddd;
  background: white;
  cursor: pointer;
  font-size: 12px;
}

.btn-sm.btn-danger {
  background: #ff4d4f;
  border-color: #ff4d4f;
  color: white;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  font-size: 14px;
  margin-bottom: 6px;
  color: #333;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}

.form-group textarea {
  resize: vertical;
}

.form-group select {
  background: white;
}

.action-btns {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.action-btns .btn-primary {
  flex: 1;
}
</style>
