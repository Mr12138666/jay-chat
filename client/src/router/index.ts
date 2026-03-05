import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import Login from '../views/Login.vue'
import Chat from '../views/Chat.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: Login
  },
  {
    path: '/chat',
    name: 'Chat',
    component: Chat,
    meta: { requiresAuth: true } // 需要登录
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查登录状态
router.beforeEach((to, _from, next) => {
  try {
    const token = localStorage.getItem('token')
    
    if (to.meta.requiresAuth && !token) {
      // 需要登录但未登录，跳转到登录页
      next('/login')
    } else if (to.path === '/login' && token) {
      // 已登录，访问登录页时跳转到聊天室
      next('/chat')
    } else {
      next()
    }
  } catch (error) {
    console.error('路由守卫错误:', error)
    next('/login')
  }
})

export default router
