/**
 * 对话式检索 API
 */
import request from './request'

export const chatApi = {
  /**
   * 发送对话消息（同步模式）
   * @param {Object} data - 消息数据
   * @param {string} data.message - 用户消息内容
   * @param {string} data.sessionId - 会话ID（可选）
   */
  send(data) {
    return request({
      url: '/chat',
      method: 'post',
      data,
      timeout: 120000 // 对话可能需要较长时间
    })
  },

  /**
   * 发送对话消息（流式模式 SSE）
   * @param {Object} data - 消息数据
   * @param {string} data.message - 用户消息内容
   * @param {string} data.sessionId - 会话ID（可选）
   * @param {Function} onMessage - 收到消息回调
   * @param {Function} onError - 错误回调
   * @param {Function} onComplete - 完成回调
   * @returns {AbortController} - 用于取消请求
   */
  sendStream(data, { onSession, onContent, onTools, onPatents, onDone, onError }) {
    const abortController = new AbortController()
    
    // 检查是否有 token
    const token = localStorage.getItem('token')
    if (!token) {
      // 未登录，直接触发错误回调
      setTimeout(() => {
        if (onError) {
          onError(new Error('未登录，请先登录'))
        }
      }, 0)
      return abortController
    }
    
    fetch('/api/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': token
      },
      body: JSON.stringify(data),
      signal: abortController.signal
    })
      .then(response => {
        if (!response.ok) {
          // 401 错误特殊处理
          if (response.status === 401) {
            // 清除本地登录状态
            localStorage.removeItem('token')
            localStorage.removeItem('userInfo')
            throw new Error('登录已过期，请重新登录')
          }
          throw new Error(`HTTP error! status: ${response.status}`)
        }
        
        const reader = response.body.getReader()
        const decoder = new TextDecoder()
        let buffer = ''
        // 将事件状态移到外部，确保跨 chunk 保持状态
        let currentEvent = null
        let currentData = ''

        function processText(text) {
          buffer += text
          const lines = buffer.split('\n')
          buffer = lines.pop() || '' // 保留未完成的行

          for (const line of lines) {
            // 处理带冒号的行
            const colonIndex = line.indexOf(':')
            if (colonIndex > 0) {
              const field = line.slice(0, colonIndex)
              // SSE 规范：如果冒号后有空格，则跳过该空格
              let value = line.slice(colonIndex + 1)
              if (value.startsWith(' ')) {
                value = value.slice(1)
              }
              
              if (field === 'event') {
                // 新事件开始前，如果有未处理的数据，先处理
                if (currentEvent && currentData !== '') {
                  handleEvent(currentEvent, currentData)
                  currentData = ''
                }
                currentEvent = value
              } else if (field === 'data') {
                // SSE 规范：多个 data: 行用换行符连接
                if (currentData !== '') {
                  currentData += '\n' + value
                } else {
                  currentData = value
                }
              }
              // 忽略其他字段如 id:, retry: 等
            } else if (line === '') {
              // 空行表示事件结束，处理事件
              if (currentEvent) {
                handleEvent(currentEvent, currentData)
                currentEvent = null
                currentData = ''
              }
            }
          }
        }

        function handleEvent(event, data) {
          console.log('[SSE] 收到事件:', event, '数据长度:', data?.length, '数据预览:', data?.substring(0, 100))
          try {
            switch (event) {
              case 'session':
                if (onSession) {
                  const parsed = JSON.parse(data)
                  console.log('[SSE] 会话ID:', parsed.sessionId)
                  onSession(parsed.sessionId)
                }
                break
              case 'content':
                if (onContent) {
                  console.log('[SSE] 内容块:', data)
                  onContent(data)
                }
                break
              case 'tools':
                if (onTools) {
                  const parsed = JSON.parse(data)
                  console.log('[SSE] 工具调用:', parsed)
                  onTools(parsed)
                }
                break
              case 'patents':
                if (onPatents) {
                  const parsed = JSON.parse(data)
                  console.log('[SSE] 专利结果:', parsed?.length)
                  onPatents(parsed)
                }
                break
              case 'done':
                if (onDone) {
                  const parsed = JSON.parse(data)
                  console.log('[SSE] 完成:', parsed)
                  onDone(parsed)
                }
                break
              case 'error':
                if (onError) {
                  const parsed = JSON.parse(data)
                  console.error('[SSE] 错误:', parsed)
                  onError(new Error(parsed.message))
                }
                break
              default:
                console.log('[SSE] 未知事件类型:', event)
            }
          } catch (e) {
            console.error('[SSE] 解析事件失败:', e, '事件:', event, '数据:', data)
          }
        }

        function read() {
          reader.read().then(({ done, value }) => {
            if (done) {
              // 处理剩余的buffer
              if (buffer.trim()) {
                processText('\n')
              }
              return
            }
            processText(decoder.decode(value, { stream: true }))
            read()
          }).catch(error => {
            if (error.name !== 'AbortError' && onError) {
              onError(error)
            }
          })
        }

        read()
      })
      .catch(error => {
        if (error.name !== 'AbortError' && onError) {
          onError(error)
        }
      })

    return abortController
  },

  /**
   * 清除会话历史
   * @param {string} sessionId - 会话ID
   */
  clearSession(sessionId) {
    return request({
      url: `/chat/session/${sessionId}`,
      method: 'delete'
    })
  },

  /**
   * 获取建议问题
   */
  getSuggestions() {
    return request({
      url: '/chat/suggestions',
      method: 'get'
    })
  },

  // ==================== 会话管理 API ====================

  /**
   * 获取用户会话列表
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码（从1开始）
   * @param {number} params.size - 每页数量
   */
  getSessions(params = { page: 1, size: 20 }) {
    return request({
      url: '/chat/sessions',
      method: 'get',
      params
    })
  },

  /**
   * 获取会话消息历史
   * @param {string} sessionId - 会话ID
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码（从1开始）
   * @param {number} params.size - 每页数量
   */
  getSessionMessages(sessionId, params = { page: 1, size: 50 }) {
    return request({
      url: `/chat/sessions/${sessionId}/messages`,
      method: 'get',
      params
    })
  },

  /**
   * 删除会话
   * @param {string} sessionId - 会话ID
   */
  deleteSession(sessionId) {
    return request({
      url: `/chat/sessions/${sessionId}`,
      method: 'delete'
    })
  },

  /**
   * 更新会话标题
   * @param {string} sessionId - 会话ID
   * @param {string} title - 新标题
   */
  updateSessionTitle(sessionId, title) {
    return request({
      url: `/chat/sessions/${sessionId}/title`,
      method: 'put',
      data: { title }
    })
  },

  /**
   * 归档会话
   * @param {string} sessionId - 会话ID
   */
  archiveSession(sessionId) {
    return request({
      url: `/chat/sessions/${sessionId}/archive`,
      method: 'put'
    })
  },

  /**
   * 获取已归档会话列表
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码（从1开始）
   * @param {number} params.size - 每页数量
   */
  getArchivedSessions(params = { page: 1, size: 20 }) {
    return request({
      url: '/chat/sessions/archived',
      method: 'get',
      params
    })
  },

  /**
   * 恢复已归档会话
   * @param {string} sessionId - 会话ID
   */
  restoreSession(sessionId) {
    return request({
      url: `/chat/sessions/${sessionId}/restore`,
      method: 'put'
    })
  }
}
