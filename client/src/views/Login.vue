<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, register, type LoginRequest, type RegisterRequest } from '../api/auth'
import { setToken, setUser } from '../utils/storage'
import { handleApiError } from '../utils/error'

const router = useRouter()

// 表单状态
const isLogin = ref(true) // true: 登录, false: 注册
const loading = ref(false)
const errorMsg = ref('')

// 登录表单
const loginForm = ref<LoginRequest>({
  username: '',
  password: ''
})

// 注册表单
const registerForm = ref<RegisterRequest>({
  username: '',
  password: '',
  nickname: ''
})

// 切换登录/注册
const toggleMode = () => {
  isLogin.value = !isLogin.value
  errorMsg.value = ''
}

// 登录
const handleLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    errorMsg.value = '请输入用户名和密码'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    const res = await login(loginForm.value)
    // 保存token和用户信息
    setToken(res.token)
    setUser(res.user)
    // 跳转到聊天室
    router.push('/chat')
  } catch (error: any) {
    console.error('登录错误:', error)
    errorMsg.value = handleApiError(error)
  } finally {
    loading.value = false
  }
}

// 注册
const handleRegister = async () => {
  if (!registerForm.value.username || !registerForm.value.password || !registerForm.value.nickname) {
    errorMsg.value = '请填写完整信息'
    return
  }

  loading.value = true
  errorMsg.value = ''

  try {
    await register(registerForm.value)
    // 注册成功后自动登录
    const res = await login({
      username: registerForm.value.username,
      password: registerForm.value.password
    })
    setToken(res.token)
    setUser(res.user)
    router.push('/chat')
  } catch (error: any) {
    console.error('注册错误:', error)
    errorMsg.value = handleApiError(error)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h1>jay-chat</h1>
        <p>大型聊天室</p>
      </div>

      <div v-if="errorMsg" class="error-message">
        {{ errorMsg }}
      </div>

      <!-- 登录表单 -->
      <form v-if="isLogin" @submit.prevent="handleLogin" class="login-form">
        <div class="form-group">
          <label>用户名</label>
          <input
            v-model="loginForm.username"
            type="text"
            placeholder="请输入用户名"
            :disabled="loading"
          />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :disabled="loading"
          />
        </div>
        <button type="submit" :disabled="loading" class="submit-btn">
          {{ loading ? '登录中...' : '登录' }}
        </button>
        <p class="toggle-text">
          还没有账号？
          <a href="#" @click.prevent="toggleMode">立即注册</a>
        </p>
      </form>

      <!-- 注册表单 -->
      <form v-else @submit.prevent="handleRegister" class="login-form">
        <div class="form-group">
          <label>用户名</label>
          <input
            v-model="registerForm.username"
            type="text"
            placeholder="请输入用户名（用于登录）"
            :disabled="loading"
          />
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input
            v-model="registerForm.nickname"
            type="text"
            placeholder="请输入昵称（显示名称）"
            :disabled="loading"
          />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            :disabled="loading"
          />
        </div>
        <button type="submit" :disabled="loading" class="submit-btn">
          {{ loading ? '注册中...' : '注册' }}
        </button>
        <p class="toggle-text">
          已有账号？
          <a href="#" @click.prevent="toggleMode">立即登录</a>
        </p>
      </form>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px;
  box-sizing: border-box;
  margin: 0;
  overflow: auto;
}

/* 移动端优化 */
@media (max-width: 480px) {
  .login-container {
    padding: 16px;
  }
  
  .login-box {
    padding: 30px 24px;
    border-radius: 8px;
  }
  
  .login-header h1 {
    font-size: 28px;
  }
  
  .form-group input {
    padding: 10px;
    font-size: 16; /* 防止iOS自动缩放 */
  }
}

.login-box {
  background: white;
  border-radius: 12px;
  padding: 40px;
  width: 100%;
  max-width: 450px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
  box-sizing: border-box;
}

/* PC端优化 - 从768px开始 */
@media (min-width: 768px) {
  .login-box {
    max-width: 500px !important;
    padding: 50px !important;
  }
  
  .login-header h1 {
    font-size: 36px !important;
  }
  
  .form-group input {
    padding: 14px !important;
    font-size: 15px !important;
  }
  
  .submit-btn {
    padding: 14px !important;
    font-size: 16px !important;
  }
}

/* 中等PC端 */
@media (min-width: 1024px) {
  .login-box {
    max-width: 550px !important;
    padding: 55px !important;
  }
}

/* 大屏PC端 */
@media (min-width: 1200px) {
  .login-box {
    max-width: 600px !important;
    padding: 60px !important;
  }
}

/* 超大屏PC端 */
@media (min-width: 1440px) {
  .login-box {
    max-width: 650px !important;
    padding: 65px !important;
  }
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.login-header h1 {
  font-size: 32px;
  color: #333;
  margin: 0 0 8px 0;
}

.login-header p {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.error-message {
  background: #fee;
  color: #c33;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 20px;
  font-size: 14px;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.form-group input {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.form-group input:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.submit-btn {
  padding: 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.2s;
}

.submit-btn:hover:not(:disabled) {
  opacity: 0.9;
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.toggle-text {
  text-align: center;
  font-size: 14px;
  color: #666;
  margin: 0;
}

.toggle-text a {
  color: #667eea;
  text-decoration: none;
  font-weight: 500;
}

.toggle-text a:hover {
  text-decoration: underline;
}
</style>
