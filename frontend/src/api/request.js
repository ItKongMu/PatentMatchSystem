import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

// 创建axios实例
const service = axios.create({
  baseURL: '/api',
  timeout: 60000, // 60秒超时（LLM处理可能较慢）
  headers: {
    'Content-Type': 'application/json'
  }
})

// 用于防止重复处理401错误的标志
let isHandling401 = false

// 处理401未授权错误（防止重复处理）
function handle401Error() {
  // 如果已经在处理401错误，直接返回
  if (isHandling401) {
    return
  }
  
  isHandling401 = true
  
  // 清除本地登录状态（不调用API，避免循环）
  localStorage.removeItem('token')
  localStorage.removeItem('userInfo')
  
  // 获取当前路由
  const currentPath = window.location.pathname
  
  // 如果不在登录页，跳转到登录页
  if (currentPath !== '/login' && currentPath !== '/register') {
    // 使用 setTimeout 确保只执行一次
    setTimeout(() => {
      router.push({
        path: '/login',
        query: { redirect: currentPath }
      })
      // 延迟重置标志，给路由跳转一些时间
      setTimeout(() => {
        isHandling401 = false
      }, 1000)
    }, 0)
  } else {
    // 如果已经在登录页，重置标志
    isHandling401 = false
  }
}

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 直接从localStorage获取token，避免在Pinia未初始化时出错
    const token = localStorage.getItem('token')
    // 添加token到请求头
    if (token) {
      config.headers['Authorization'] = token
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    
    // 业务状态码判断
    if (res.code !== 200) {
      // 401 - 未授权，直接处理跳转，不弹出消息
      if (res.code === 401) {
        handle401Error()
        return Promise.reject(new Error(res.message || '未登录'))
      }
      
      // 其他业务错误，显示错误消息
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    
    return res
  },
  error => {
    console.error('响应错误:', error)
    
    // 如果请求被取消，不处理
    if (axios.isCancel(error)) {
      return Promise.reject(error)
    }
    
    let message = '请求失败'
    let showMessage = true
    
    if (error.response) {
      const status = error.response.status
      // 优先使用后端返回的业务消息
      const backendMessage = error.response.data?.message
      switch (status) {
        case 400:
          message = backendMessage || '请求参数错误'
          break
        case 401:
          // 401错误统一处理，不弹出消息
          handle401Error()
          showMessage = false
          break
        case 403:
          // 优先显示后端返回的权限描述，无则显示通用提示
          message = backendMessage || '无权限执行该操作'
          break
        case 404:
          message = backendMessage || '请求资源不存在'
          break
        case 500:
          message = backendMessage || '服务器内部错误'
          break
        default:
          message = backendMessage || `请求失败(${status})`
      }
    } else if (error.code === 'ECONNABORTED') {
      message = '请求超时，请稍后重试'
    } else if (!window.navigator.onLine) {
      message = '网络连接失败'
    }
    
    if (showMessage) {
      ElMessage.error(message)
    }
    return Promise.reject(error)
  }
)

export default service
