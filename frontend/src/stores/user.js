import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { authApi } from '@/api/auth'
import router from '@/router'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref(localStorage.getItem('token') || '')
  const userInfo = ref(JSON.parse(localStorage.getItem('userInfo') || 'null'))
  
  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '')
  const role = computed(() => userInfo.value?.role || 'user')
  const isAdmin = computed(() => role.value === 'admin')
  
  // 设置Token
  function setToken(newToken) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }
  
  // 设置用户信息
  function setUserInfo(info) {
    userInfo.value = info
    localStorage.setItem('userInfo', JSON.stringify(info))
  }
  
  // 登录
  async function login(loginForm) {
    try {
      const res = await authApi.login(loginForm)
      if (res.code === 200) {
        setToken(res.data.token)
        setUserInfo(res.data.userInfo)
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message || '登录失败' }
    }
  }
  
  // 注册
  async function register(registerForm) {
    try {
      const res = await authApi.register(registerForm)
      if (res.code === 200) {
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message || '注册失败' }
    }
  }
  
  // 获取用户信息
  async function fetchUserInfo() {
    try {
      const res = await authApi.getUserInfo()
      if (res.code === 200) {
        setUserInfo(res.data)
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }
  
  // 清除本地登录状态（不调用API，用于被动登出如token过期）
  function clearLocalAuth() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
  }
  
  // 退出登录
  async function logout(callApi = true) {
    try {
      // 只有主动登出且有token时才调用API
      if (callApi && token.value) {
        await authApi.logout()
      }
    } catch (error) {
      // 忽略登出API错误
      console.warn('登出API调用失败:', error)
    } finally {
      // 无论成功失败都清除本地状态
      clearLocalAuth()
      router.push('/login')
    }
  }
  
  return {
    // 状态
    token,
    userInfo,
    // 计算属性
    isLoggedIn,
    username,
    nickname,
    role,
    isAdmin,
    // 方法
    setToken,
    setUserInfo,
    login,
    register,
    fetchUserInfo,
    clearLocalAuth,
    logout
  }
})
