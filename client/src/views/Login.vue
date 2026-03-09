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
    <!-- 背景装饰 -->
    <div class="bg-decoration">
      <div class="floating-shape shape-1"></div>
      <div class="floating-shape shape-2"></div>
      <div class="floating-shape shape-3"></div>
      <div class="floating-shape shape-4"></div>
      <div class="glow-orb orb-1"></div>
      <div class="glow-orb orb-2"></div>
      <div class="glow-orb orb-3"></div>
    </div>

    <div class="login-box glass-card">
      <div class="login-header">
        <div class="logo-container">
          <span class="logo-emoji">💬</span>
        </div>
        <h1>jay-chat</h1>
        <p>和朋友们一起聊天吧</p>
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
            class="input-glass"
            :disabled="loading"
          />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            class="input-glass"
            :disabled="loading"
          />
        </div>
        <button type="submit" :disabled="loading" class="submit-btn gradient-btn">
          {{ loading ? '登录中...' : '登 录' }}
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
            class="input-glass"
            :disabled="loading"
          />
        </div>
        <div class="form-group">
          <label>昵称</label>
          <input
            v-model="registerForm.nickname"
            type="text"
            placeholder="请输入昵称（显示名称）"
            class="input-glass"
            :disabled="loading"
          />
        </div>
        <div class="form-group">
          <label>密码</label>
          <input
            v-model="registerForm.password"
            type="password"
            placeholder="请输入密码"
            class="input-glass"
            :disabled="loading"
          />
        </div>
        <button type="submit" :disabled="loading" class="submit-btn gradient-btn">
          {{ loading ? '注册中...' : '注 册' }}
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
  display: flex;
  align-items: center;
  justify-content: center;
  /* 柔和的粉红到薄荷绿渐变 */
  background: linear-gradient(135deg, #fff5f5 0%, #f0fffe 50%, #fff9f0 100%);
  padding: 20px;
  box-sizing: border-box;
  margin: 0;
  overflow: hidden;
}

/* 背景装饰 */
.bg-decoration {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
  pointer-events: none;
}

.floating-shape {
  position: absolute;
  border-radius: 50%;
  opacity: 0.6;
  animation: float 6s ease-in-out infinite;
}

.shape-1 {
  width: 200px;
  height: 200px;
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.3), rgba(255, 159, 67, 0.3));
  top: 10%;
  left: 10%;
  animation-delay: 0s;
}

.shape-2 {
  width: 150px;
  height: 150px;
  background: linear-gradient(135deg, rgba(78, 205, 196, 0.3), rgba(0, 210, 211, 0.3));
  top: 60%;
  right: 15%;
  animation-delay: 1s;
}

.shape-3 {
  width: 100px;
  height: 100px;
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.3), rgba(224, 195, 252, 0.3));
  bottom: 20%;
  left: 20%;
  animation-delay: 2s;
}

.shape-4 {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, rgba(255, 159, 67, 0.3), rgba(255, 107, 157, 0.3));
  top: 30%;
  right: 25%;
  animation-delay: 3s;
}

.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(60px);
  animation: pulse 4s ease-in-out infinite;
}

.orb-1 {
  width: 300px;
  height: 300px;
  background: rgba(255, 107, 157, 0.2);
  top: -100px;
  left: -100px;
}

.orb-2 {
  width: 250px;
  height: 250px;
  background: rgba(78, 205, 196, 0.2);
  bottom: -80px;
  right: -80px;
  animation-delay: 1s;
}

.orb-3 {
  width: 200px;
  height: 200px;
  background: rgba(102, 126, 234, 0.15);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: 2s;
}

/* 登录卡片 */
.login-box {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.8);
  border-radius: var(--radius-xl);
  padding: 40px;
  width: 100%;
  max-width: 420px;
  box-shadow: 0 8px 32px rgba(31, 38, 135, 0.1);
}

/* 移动端优化 */
@media (max-width: 480px) {
  .login-box {
    padding: 30px 24px;
    border-radius: var(--radius-lg);
  }

  .login-header h1 {
    font-size: 28px;
  }
}

.login-header {
  text-align: center;
  margin-bottom: 30px;
}

.logo-container {
  width: 80px;
  height: 80px;
  margin: 0 auto 16px;
  background: linear-gradient(135deg, #ff6b9d 0%, #ff9f43 100%);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8px 24px rgba(255, 107, 157, 0.3);
}

.logo-emoji {
  font-size: 40px;
}

.login-header h1 {
  font-size: 32px;
  background: linear-gradient(135deg, #ff6b9d, #ff9f43, #4ecdc4);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0 0 8px 0;
  font-weight: 700;
}

.login-header p {
  color: var(--text-secondary);
  font-size: 14px;
  margin: 0;
}

.error-message {
  background: linear-gradient(135deg, rgba(255, 107, 157, 0.1), rgba(255, 159, 67, 0.1));
  color: #e84393;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  margin-bottom: 20px;
  font-size: 14px;
  border: 1px solid rgba(255, 107, 157, 0.2);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-group label {
  font-size: 14px;
  color: var(--text-primary);
  font-weight: 500;
}

.form-group input {
  padding: 14px 16px;
  border: 2px solid transparent;
  border-radius: var(--radius-md);
  font-size: 14px;
  transition: all 0.3s ease;
  background: rgba(255, 255, 255, 0.8);
  background-image: linear-gradient(#fff, #fff), linear-gradient(135deg, #ff6b9d, #4ecdc4);
  background-origin: border-box;
  background-clip: padding-box, border-box;
}

.form-group input:focus {
  outline: none;
  box-shadow: 0 0 20px rgba(255, 107, 157, 0.2);
}

.form-group input:disabled {
  background: var(--bg-tertiary);
  cursor: not-allowed;
}

.form-group input::placeholder {
  color: var(--text-light);
}

.submit-btn {
  padding: 14px;
  font-size: 16px;
  font-weight: 600;
  margin-top: 8px;
}

.toggle-text {
  text-align: center;
  font-size: 14px;
  color: var(--text-secondary);
  margin: 0;
}

.toggle-text a {
  color: #ff6b9d;
  font-weight: 600;
}

.toggle-text a:hover {
  color: #ff9f43;
}
</style>
