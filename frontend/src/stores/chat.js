import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { chatApi } from '@/api/chat'

// localStorage 键名
const STORAGE_KEY = 'patent_chat_state'

/**
 * 聊天状态管理 Store
 * 负责管理聊天会话的状态持久化，解决页面切换时会话历史消失的问题
 * 支持 MongoDB 持久化的会话管理
 */
export const useChatStore = defineStore('chat', () => {
  // ==================== 状态 ====================
  const sessionId = ref(null)
  const messages = ref([])
  const suggestions = ref([])
  const loading = ref(false)
  const streamingMessageIndex = ref(-1)
  const inputMessage = ref('')
  
  // 会话列表相关状态
  const sessions = ref([])
  const sessionsLoading = ref(false)
  const currentSessionTitle = ref('')
  
  // ==================== 计算属性 ====================
  const hasSession = computed(() => !!sessionId.value)
  const hasMessages = computed(() => messages.value.length > 0)
  const messageCount = computed(() => messages.value.length)
  const hasSessions = computed(() => sessions.value.length > 0)
  
  // ==================== 持久化方法 ====================
  
  /**
   * 保存状态到 localStorage
   */
  function saveToStorage() {
    try {
      const state = {
        sessionId: sessionId.value,
        messages: messages.value,
        suggestions: suggestions.value,
        inputMessage: inputMessage.value,
        savedAt: new Date().toISOString()
      }
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state))
    } catch (error) {
      console.error('保存聊天状态失败:', error)
    }
  }
  
  /**
   * 从 localStorage 恢复状态
   */
  function loadFromStorage() {
    try {
      const stored = localStorage.getItem(STORAGE_KEY)
      if (stored) {
        const state = JSON.parse(stored)
        
        // 检查是否过期（24小时）
        const savedAt = new Date(state.savedAt)
        const now = new Date()
        const hoursDiff = (now - savedAt) / (1000 * 60 * 60)
        
        if (hoursDiff < 24) {
          sessionId.value = state.sessionId || null
          messages.value = state.messages || []
          suggestions.value = state.suggestions || []
          inputMessage.value = state.inputMessage || ''
          return true
        } else {
          // 过期，清除存储
          clearStorage()
        }
      }
    } catch (error) {
      console.error('恢复聊天状态失败:', error)
      clearStorage()
    }
    return false
  }
  
  /**
   * 清除 localStorage 中的状态
   */
  function clearStorage() {
    localStorage.removeItem(STORAGE_KEY)
  }
  
  // ==================== 状态操作方法 ====================
  
  /**
   * 设置会话ID
   */
  function setSessionId(id) {
    sessionId.value = id
    saveToStorage()
  }
  
  /**
   * 设置建议问题列表
   */
  function setSuggestions(list) {
    suggestions.value = list || []
    saveToStorage()
  }
  
  /**
   * 添加用户消息
   */
  function addUserMessage(content) {
    messages.value.push({
      role: 'user',
      content,
      timestamp: new Date().toISOString()
    })
    saveToStorage()
  }
  
  /**
   * 添加空的助手消息（用于流式填充）
   * @returns {number} 消息索引
   */
  function addEmptyAssistantMessage() {
    const index = messages.value.length
    messages.value.push({
      role: 'assistant',
      content: '',
      patents: [],
      toolCalls: [],
      suggestions: [],
      timestamp: new Date().toISOString(),
      streaming: true
    })
    streamingMessageIndex.value = index
    return index
  }
  
  /**
   * 更新助手消息内容（流式追加）
   */
  function appendAssistantContent(chunk) {
    if (streamingMessageIndex.value >= 0 && messages.value[streamingMessageIndex.value]) {
      messages.value[streamingMessageIndex.value].content += chunk
    }
  }
  
  /**
   * 更新助手消息的工具调用信息
   */
  function updateAssistantTools(tools) {
    if (streamingMessageIndex.value >= 0 && messages.value[streamingMessageIndex.value]) {
      messages.value[streamingMessageIndex.value].toolCalls = tools
    }
  }
  
  /**
   * 更新助手消息的专利结果
   */
  function updateAssistantPatents(patents) {
    if (streamingMessageIndex.value >= 0 && messages.value[streamingMessageIndex.value]) {
      messages.value[streamingMessageIndex.value].patents = patents
    }
  }
  
  /**
   * 完成助手消息
   */
  function finishAssistantMessage(data = {}) {
    if (streamingMessageIndex.value >= 0 && messages.value[streamingMessageIndex.value]) {
      const msg = messages.value[streamingMessageIndex.value]
      msg.suggestions = data.suggestions || []
      msg.streaming = false
      msg.timestamp = data.timestamp || new Date().toISOString()
    }
    streamingMessageIndex.value = -1
    saveToStorage()
    
    // 对话完成后刷新会话列表（后台执行，不阻塞UI）
    fetchSessions()
  }
  
  /**
   * 设置助手消息错误
   */
  function setAssistantError(errorMessage) {
    if (streamingMessageIndex.value >= 0 && messages.value[streamingMessageIndex.value]) {
      const msg = messages.value[streamingMessageIndex.value]
      if (!msg.content) {
        msg.content = errorMessage || '抱歉，处理您的请求时出现了问题，请稍后重试。'
      }
      msg.streaming = false
    }
    streamingMessageIndex.value = -1
    saveToStorage()
  }
  
  /**
   * 设置加载状态
   */
  function setLoading(value) {
    loading.value = value
  }
  
  /**
   * 设置输入消息
   */
  function setInputMessage(value) {
    inputMessage.value = value
  }
  
  /**
   * 清除当前会话，开始新对话
   */
  async function clearSession() {
    if (sessionId.value) {
      try {
        await chatApi.clearSession(sessionId.value)
      } catch (error) {
        console.error('清除会话失败:', error)
      }
    }
    
    sessionId.value = null
    messages.value = []
    loading.value = false
    streamingMessageIndex.value = -1
    inputMessage.value = ''
    clearStorage()
  }
  
  /**
   * 初始化 Store（从 localStorage 恢复或加载建议）
   */
  async function initialize() {
    const restored = loadFromStorage()
    
    // 如果没有恢复到建议，则加载新的建议
    if (!suggestions.value.length) {
      try {
        const res = await chatApi.getSuggestions()
        if (res.code === 200) {
          suggestions.value = res.data || []
        }
      } catch (error) {
        console.error('加载建议失败:', error)
        // 使用默认建议
        suggestions.value = [
          '帮我搜索关于深度学习的专利',
          '查找图像识别相关的技术方案',
          '统计一下人工智能专利的领域分布'
        ]
      }
    }
    
    // 加载会话列表（后台执行，不阻塞）
    fetchSessions()
    
    return restored
  }
  
  // ==================== 会话管理方法 ====================
  
  /**
   * 获取用户会话列表
   */
  async function fetchSessions(page = 1, size = 20) {
    sessionsLoading.value = true
    try {
      const res = await chatApi.getSessions({ page, size })
      if (res.code === 200) {
        sessions.value = res.data || []
      }
    } catch (error) {
      console.error('获取会话列表失败:', error)
      // 如果是401未登录，清空会话列表
      if (error?.response?.status === 401) {
        sessions.value = []
      }
    } finally {
      sessionsLoading.value = false
    }
  }
  
  /**
   * 切换到指定会话
   */
  async function switchSession(targetSessionId) {
    if (targetSessionId === sessionId.value) {
      return // 已经是当前会话
    }
    
    loading.value = true
    try {
      // 获取会话消息历史
      const res = await chatApi.getSessionMessages(targetSessionId)
      if (res.code === 200) {
        // 更新当前会话ID
        sessionId.value = targetSessionId
        
        // 转换消息格式
        messages.value = (res.data || []).map(msg => ({
          role: msg.role,
          content: msg.content,
          timestamp: msg.timestamp,
          patents: msg.metadata?.patents || [],
          toolCalls: msg.metadata?.toolCalls || [],
          suggestions: [],
          streaming: false
        }))
        
        // 更新会话标题
        const session = sessions.value.find(s => s.sessionId === targetSessionId)
        if (session) {
          currentSessionTitle.value = session.title || '新会话'
        }
        
        // 保存到本地存储
        saveToStorage()
      }
    } catch (error) {
      console.error('切换会话失败:', error)
    } finally {
      loading.value = false
    }
  }
  
  /**
   * 创建新会话
   */
  function createNewSession() {
    sessionId.value = null
    messages.value = []
    currentSessionTitle.value = ''
    inputMessage.value = ''
    streamingMessageIndex.value = -1
    clearStorage()
  }
  
  /**
   * 删除会话
   */
  async function deleteSessionById(targetSessionId) {
    try {
      const res = await chatApi.deleteSession(targetSessionId)
      if (res.code === 200) {
        // 从列表中移除
        sessions.value = sessions.value.filter(s => s.sessionId !== targetSessionId)
        
        // 如果删除的是当前会话，则重置
        if (targetSessionId === sessionId.value) {
          createNewSession()
        }
        
        return true
      }
    } catch (error) {
      console.error('删除会话失败:', error)
    }
    return false
  }
  
  /**
   * 更新会话标题
   */
  async function updateSessionTitleById(targetSessionId, title) {
    try {
      const res = await chatApi.updateSessionTitle(targetSessionId, title)
      if (res.code === 200) {
        // 更新列表中的标题
        const session = sessions.value.find(s => s.sessionId === targetSessionId)
        if (session) {
          session.title = title
        }
        
        // 更新当前会话标题
        if (targetSessionId === sessionId.value) {
          currentSessionTitle.value = title
        }
        
        return true
      }
    } catch (error) {
      console.error('更新会话标题失败:', error)
    }
    return false
  }
  
  /**
   * 归档会话
   */
  async function archiveSessionById(targetSessionId) {
    try {
      const res = await chatApi.archiveSession(targetSessionId)
      if (res.code === 200) {
        // 从活跃列表中移除
        sessions.value = sessions.value.filter(s => s.sessionId !== targetSessionId)
        
        // 如果归档的是当前会话，则重置
        if (targetSessionId === sessionId.value) {
          createNewSession()
        }
        
        return true
      }
    } catch (error) {
      console.error('归档会话失败:', error)
    }
    return false
  }

  // ==================== 归档会话管理 ====================

  // 已归档会话列表
  const archivedSessions = ref([])
  const archivedSessionsLoading = ref(false)

  /**
   * 获取已归档会话列表
   */
  async function fetchArchivedSessions(page = 1, size = 20) {
    archivedSessionsLoading.value = true
    try {
      const res = await chatApi.getArchivedSessions({ page, size })
      if (res.code === 200) {
        archivedSessions.value = res.data || []
      }
    } catch (error) {
      console.error('获取已归档会话失败:', error)
    } finally {
      archivedSessionsLoading.value = false
    }
  }

  /**
   * 恢复已归档会话
   */
  async function restoreSessionById(targetSessionId) {
    try {
      const res = await chatApi.restoreSession(targetSessionId)
      if (res.code === 200) {
        // 从归档列表中移除
        const restoredSession = archivedSessions.value.find(s => s.sessionId === targetSessionId)
        archivedSessions.value = archivedSessions.value.filter(s => s.sessionId !== targetSessionId)
        
        // 添加到活跃列表开头
        if (restoredSession) {
          sessions.value.unshift(restoredSession)
        }
        
        return true
      }
    } catch (error) {
      console.error('恢复会话失败:', error)
    }
    return false
  }
  
  return {
    // 状态
    sessionId,
    messages,
    suggestions,
    loading,
    streamingMessageIndex,
    inputMessage,
    sessions,
    sessionsLoading,
    currentSessionTitle,
    archivedSessions,
    archivedSessionsLoading,
    
    // 计算属性
    hasSession,
    hasMessages,
    messageCount,
    hasSessions,
    
    // 方法
    saveToStorage,
    loadFromStorage,
    clearStorage,
    setSessionId,
    setSuggestions,
    addUserMessage,
    addEmptyAssistantMessage,
    appendAssistantContent,
    updateAssistantTools,
    updateAssistantPatents,
    finishAssistantMessage,
    setAssistantError,
    setLoading,
    setInputMessage,
    clearSession,
    initialize,
    
    // 会话管理方法
    fetchSessions,
    switchSession,
    createNewSession,
    deleteSessionById,
    updateSessionTitleById,
    archiveSessionById,
    
    // 归档会话管理
    fetchArchivedSessions,
    restoreSessionById
  }
})
