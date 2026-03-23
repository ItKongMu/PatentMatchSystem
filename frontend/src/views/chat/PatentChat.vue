<template>
  <div class="page-container patent-chat-page">
    <!-- 聊天主体区域（包含侧边栏和对话区） -->
    <div class="chat-main-area">
      <!-- 会话侧边栏（始终渲染，内部控制折叠） -->
      <ChatSessionSidebar class="session-sidebar-wrapper" />

      <!-- 对话主体区域 -->
      <div class="chat-container card">

      <!-- 助手信息头部 -->
      <div class="chat-header">
        <div class="assistant-identity">
          <div class="assistant-avatar">
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7H3a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
              <path d="M3 14v3a1 1 0 0 0 1 1h1v2a1 1 0 0 0 2 0v-2h10v2a1 1 0 0 0 2 0v-2h1a1 1 0 0 0 1-1v-3"/>
              <circle cx="9" cy="11" r="1" fill="currentColor"/>
              <circle cx="15" cy="11" r="1" fill="currentColor"/>
            </svg>
          </div>
          <div class="assistant-info">
            <div class="assistant-name-row">
              <span class="assistant-name">PatentMind</span>
              <span class="assistant-badge">专利智能顾问</span>
              <span class="assistant-status" :class="{ 'thinking': loading }">
                <span class="status-dot"></span>
                {{ loading ? '思考中...' : '在线' }}
              </span>
            </div>
            <p class="assistant-desc">专注于专利检索 · 技术匹配 · 知识图谱分析 · 领域洞察</p>
          </div>
        </div>
        <div class="header-actions">
          <el-button size="small" plain @click="handleNewSession" :disabled="loading">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 5v14M5 12h14"/>
            </svg>
            新建对话
          </el-button>
        </div>
      </div>

      <!-- 对话消息列表 -->
      <div ref="messageListRef" class="message-list">
        <!-- 欢迎消息 -->
        <div v-if="messages.length === 0" class="welcome-section">
          <div class="welcome-icon">
            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7H3a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
              <path d="M3 14v3a1 1 0 0 0 1 1h1v2a1 1 0 0 0 2 0v-2h10v2a1 1 0 0 0 2 0v-2h1a1 1 0 0 0 1-1v-3"/>
              <circle cx="9" cy="11" r="1" fill="currentColor"/>
              <circle cx="15" cy="11" r="1" fill="currentColor"/>
            </svg>
          </div>
          <h2>你好！我是 PatentMind</h2>
          <p>我是您的专利智能顾问，可帮您检索专利、分析技术匹配度、探索知识图谱关联，以及洞察领域发展趋势。</p>
          
          <!-- 建议问题 -->
          <div class="suggestion-list">
            <button 
              v-for="(suggestion, index) in suggestions" 
              :key="index"
              class="suggestion-btn"
              @click="handleSuggestionClick(suggestion)"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"/>
              </svg>
              {{ suggestion }}
            </button>
          </div>
        </div>

        <!-- 消息列表 -->
        <!-- 跳过正在流式输出但内容为空的消息（由下方的"正在思考..."提示替代） -->
        <div 
          v-for="(msg, index) in messages" 
          :key="index"
          v-show="!(msg.role === 'assistant' && msg.streaming && !msg.content)"
          class="message-item"
          :class="msg.role"
        >
          <div class="message-avatar" :class="msg.role">
            <!-- 用户头像 -->
            <svg v-if="msg.role === 'user'" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
              <circle cx="12" cy="7" r="4"/>
            </svg>
            <!-- AI头像：与头部栏保持一致的机器人图标 -->
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7H3a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
              <path d="M3 14v3a1 1 0 0 0 1 1h1v2a1 1 0 0 0 2 0v-2h10v2a1 1 0 0 0 2 0v-2h1a1 1 0 0 0 1-1v-3"/>
              <circle cx="9" cy="11" r="1" fill="currentColor"/>
              <circle cx="15" cy="11" r="1" fill="currentColor"/>
            </svg>
          </div>
          
          <div class="message-content">
            <div class="message-header">
              <span class="message-sender">{{ msg.role === 'user' ? '我' : 'PatentMind' }}</span>
              <span class="message-time">{{ formatTime(msg.timestamp) }}</span>
            </div>
            
            <div class="message-body">
              <!-- 用户消息 -->
              <div v-if="msg.role === 'user'" class="user-message">
                {{ msg.content }}
              </div>
              
              <!-- AI回复 -->
              <div v-else class="assistant-message">
                <div class="reply-text" :class="{ 'streaming': msg.streaming }">
                  <span v-html="formatReply(msg.content)"></span>
                  <span v-if="msg.streaming" class="streaming-cursor">▌</span>
                </div>
                
                <!-- 工具调用信息 -->
                <div v-if="msg.toolCalls?.length" class="tool-calls">
                  <div class="tool-call-header">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>
                    </svg>
                    工具调用
                  </div>
                  <div 
                    v-for="(tool, idx) in msg.toolCalls" 
                    :key="idx" 
                    class="tool-call-item"
                  >
                    <span class="tool-name">{{ tool.toolName }}</span>
                    <span class="tool-result">{{ tool.resultSummary }}</span>
                  </div>
                </div>
                
                <!-- 专利结果卡片 -->
                <div v-if="msg.patents?.length" class="patent-results">
                  <div class="results-header">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                      <polyline points="14 2 14 8 20 8"/>
                    </svg>
                    检索到 {{ msg.patents.length }} 项专利
                  </div>
                  
                  <div class="patent-cards">
                    <div 
                      v-for="patent in msg.patents.slice(0, 5)" 
                      :key="patent.id"
                      class="patent-card"
                      @click="viewPatent(patent.id)"
                    >
                      <div class="patent-card-header">
                        <span class="patent-no">{{ patent.publicationNo || 'ID:' + patent.id }}</span>
                        <span v-if="patent.relevanceScore" class="relevance-score">
                          {{ (patent.relevanceScore * 100).toFixed(0) }}%
                        </span>
                      </div>
                      <h4 class="patent-title">{{ patent.title }}</h4>
                      <p v-if="patent.applicant" class="patent-applicant">
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                          <circle cx="12" cy="7" r="4"/>
                        </svg>
                        {{ patent.applicant }}
                      </p>
                      <p v-if="patent.abstractText" class="patent-abstract">
                        {{ patent.abstractText }}
                      </p>
                      <div v-if="patent.entities?.length" class="patent-entities">
                        <span 
                          v-for="(entity, eIdx) in patent.entities.slice(0, 3)" 
                          :key="eIdx"
                          class="entity-tag"
                        >
                          {{ entity }}
                        </span>
                        <span v-if="patent.entities.length > 3" class="entity-more">
                          +{{ patent.entities.length - 3 }}
                        </span>
                      </div>
                    </div>
                    
                    <div v-if="msg.patents.length > 5" class="more-results">
                      还有 {{ msg.patents.length - 5 }} 项结果未显示
                    </div>
                  </div>
                </div>
                
                <!-- 图谱可视化 -->
                <div v-if="msg.graphData" class="graph-embed">
                  <div class="graph-embed-header">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <circle cx="12" cy="12" r="3"/><path d="M19.07 4.93a10 10 0 0 1 0 14.14"/><path d="M4.93 4.93a10 10 0 0 0 0 14.14"/>
                    </svg>
                    知识图谱：{{ msg.graphData.queryValue }}
                    <span class="graph-stats">{{ msg.graphData.nodes?.length || 0 }} 个节点 · {{ msg.graphData.links?.length || 0 }} 条关系</span>
                    <a class="graph-link" @click="openFullGraph(msg.graphData)">查看完整图谱 →</a>
                  </div>
                  <div
                    :ref="el => registerGraphRef(el, index)"
                    class="graph-embed-canvas"
                  ></div>
                </div>

                <!-- 后续建议 -->
                <div v-if="msg.suggestions?.length && index === messages.length - 1" class="follow-suggestions">
                  <span class="suggestion-label">您可以继续问：</span>
                  <button 
                    v-for="(sug, idx) in msg.suggestions" 
                    :key="idx"
                    class="follow-btn"
                    @click="handleSuggestionClick(sug)"
                  >
                    {{ sug }}
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 流式输出时的光标闪烁指示器（显示在消息末尾） -->
        <div v-if="loading && streamingMessageIndex >= 0 && messages[streamingMessageIndex]?.content === ''" class="message-item assistant loading">
          <div class="message-avatar assistant">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M12 2a2 2 0 0 1 2 2c0 .74-.4 1.39-1 1.73V7h1a7 7 0 0 1 7 7H3a7 7 0 0 1 7-7h1V5.73c-.6-.34-1-.99-1-1.73a2 2 0 0 1 2-2z"/>
              <path d="M3 14v3a1 1 0 0 0 1 1h1v2a1 1 0 0 0 2 0v-2h10v2a1 1 0 0 0 2 0v-2h1a1 1 0 0 0 1-1v-3"/>
              <circle cx="9" cy="11" r="1" fill="currentColor"/>
              <circle cx="15" cy="11" r="1" fill="currentColor"/>
            </svg>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="message-sender">PatentMind</span>
            </div>
            <div class="message-body">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
                <span class="typing-text">正在思考...</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="input-area">
        <div class="input-wrapper">
          <el-input
            v-model="inputMessage"
            type="textarea"
            :autosize="{ minRows: 1, maxRows: 4 }"
            placeholder="输入您的问题，例如：帮我搜索关于深度学习的专利..."
            :disabled="loading"
            @keydown.enter.exact.prevent="handleSend"
          />
          <el-button 
            type="primary" 
            :loading="loading"
            :disabled="!inputMessage.trim()"
            @click="handleSend"
          >
            <svg v-if="!loading" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="22" y1="2" x2="11" y2="13"/>
              <polygon points="22 2 15 22 11 13 2 9 22 2"/>
            </svg>
            发送
          </el-button>
        </div>
        <div class="input-tips">
          <span>按 Enter 发送，Shift + Enter 换行</span>
        </div>
      </div>
    </div>
    </div><!-- chat-main-area 结束 -->
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, onUnmounted, onActivated, onDeactivated, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { chatApi } from '@/api/chat'
import { useChatStore } from '@/stores/chat'
import { storeToRefs } from 'pinia'
import ChatSessionSidebar from '@/components/chat/ChatSessionSidebar.vue'
import * as echarts from 'echarts/core'
import { GraphChart } from 'echarts/charts'
import { TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([GraphChart, TooltipComponent, CanvasRenderer])

const router = useRouter()
const chatStore = useChatStore()

// 从 Store 获取响应式状态
const { 
  sessionId, 
  messages, 
  suggestions, 
  loading, 
  streamingMessageIndex,
  inputMessage
} = storeToRefs(chatStore)

// 组件内部状态
const messageListRef = ref(null)
const currentAbortController = ref(null)  // 用于取消流式请求
const isInitialized = ref(false)

// ==================== 图谱相关 ====================
// 存储各消息索引对应的图谱 DOM 元素 ref 和 ECharts 实例
const graphRefs = {}       // index -> DOM element
const graphInstances = {}  // index -> ECharts instance

const NODE_COLORS = {
  Patent: '#3B82F6',
  Entity: '#10B981',
  IPC: '#F59E0B',
  Applicant: '#8B5CF6',
  Concept: '#EC4899'
}

const getNodeColor = (label) => NODE_COLORS[label] || '#94A3B8'
const getNodeSize = (label) => {
  const sizeMap = { Patent: 24, IPC: 18, Applicant: 16, Concept: 14, Entity: 13 }
  return sizeMap[label] || 12
}

/**
 * 注册图谱容器 DOM ref（通过 :ref 动态绑定）
 */
const registerGraphRef = (el, index) => {
  if (el) {
    graphRefs[index] = el
    // 若此时已有图谱数据，立即渲染
    const msg = messages.value[index]
    if (msg?.graphData && !graphInstances[index]) {
      nextTick(() => renderGraphAt(index, msg.graphData))
    }
  }
}

/**
 * 在指定消息索引处渲染图谱
 */
const renderGraphAt = (index, graphData) => {
  const container = graphRefs[index]
  if (!container || !graphData) return

  // 销毁旧实例（如果存在）
  if (graphInstances[index]) {
    graphInstances[index].dispose()
  }

  const chart = echarts.init(container)
  graphInstances[index] = chart

  const nodes = (graphData.nodes || []).map(node => ({
    id: node.id,
    name: node.name,
    nodeType: node.label,
    symbolSize: getNodeSize(node.label),
    itemStyle: { color: getNodeColor(node.label) },
    label: {
      show: true,
      position: 'right',
      fontSize: 10,
      color: '#374151',
      formatter: (p) => {
        const n = p.data.name || ''
        return n.length > 10 ? n.substring(0, 10) + '…' : n
      }
    }
  }))

  const links = (graphData.links || []).map(link => ({
    source: link.source,
    target: link.target,
    label: { show: true, formatter: link.type || '', fontSize: 9, color: '#9CA3AF' },
    lineStyle: { color: '#CBD5E1', width: 1.2, curveness: 0.1 }
  }))

  chart.setOption({
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          return `<strong>${params.data.name}</strong><br/><span style="color:#9CA3AF">${params.data.nodeType || ''}</span>`
        }
        if (params.dataType === 'edge') {
          const t = typeof params.data.label === 'object' ? params.data.label.formatter : params.data.label
          return t ? `<span style="color:#9CA3AF">${t}</span>` : ''
        }
        return ''
      }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      roam: true,
      draggable: true,
      data: nodes,
      links,
      force: { repulsion: 150, edgeLength: [60, 150], gravity: 0.1, layoutAnimation: true },
      emphasis: { focus: 'adjacency' }
    }]
  })
}

/**
 * 点击"查看完整图谱"：跳转到知识图谱页面（可扩展为带参数跳转）
 */
const openFullGraph = (graphData) => {
  router.push({
    path: '/graph',
    query: { type: graphData.queryType, value: graphData.queryValue }
  })
}

// 发送消息（流式模式）
const handleSend = async () => {
  const content = inputMessage.value.trim()
  if (!content || loading.value) return

  // 添加用户消息
  chatStore.addUserMessage(content)

  chatStore.setInputMessage('')
  chatStore.setLoading(true)
  scrollToBottom()

  // 添加空的AI回复消息（用于流式填充）
  chatStore.addEmptyAssistantMessage()

  try {
    // 使用流式API
    currentAbortController.value = chatApi.sendStream(
      {
        message: content,
        sessionId: sessionId.value
      },
      {
        // 收到会话ID
        onSession: (newSessionId) => {
          chatStore.setSessionId(newSessionId)
        },
        // 收到内容片段
        onContent: (chunk) => {
          chatStore.appendAssistantContent(chunk)
          scrollToBottom()
        },
        // 收到工具调用信息
        onTools: (tools) => {
          chatStore.updateAssistantTools(tools)
        },
        // 收到专利结果
        onPatents: (patents) => {
          chatStore.updateAssistantPatents(patents)
          scrollToBottom()
        },
        // 收到图谱数据
        onGraph: (graphData) => {
          chatStore.updateAssistantGraph(graphData)
          // 等待 DOM 渲染后初始化图谱
          nextTick(() => {
            const idx = chatStore.streamingMessageIndex
            if (idx >= 0) {
              renderGraphAt(idx, graphData)
            }
            scrollToBottom()
          })
        },
        // 流式完成
        onDone: (data) => {
          chatStore.finishAssistantMessage(data)
          chatStore.setLoading(false)
          currentAbortController.value = null
          scrollToBottom()
        },
        // 错误处理
        onError: (error) => {
          console.error('流式对话失败:', error)
          chatStore.setAssistantError('抱歉，处理您的请求时出现了问题，请稍后重试。')
          chatStore.setLoading(false)
          currentAbortController.value = null
        }
      }
    )
  } catch (error) {
    console.error('发送消息失败:', error)
    chatStore.setAssistantError('抱歉，处理您的请求时出现了问题，请稍后重试。')
    chatStore.setLoading(false)
  }
}

// 取消当前流式请求
const cancelStream = () => {
  if (currentAbortController.value) {
    currentAbortController.value.abort()
    currentAbortController.value = null
  }
}

// 点击建议问题
const handleSuggestionClick = (suggestion) => {
  chatStore.setInputMessage(suggestion)
  handleSend()
}

// 开始新对话
const handleNewSession = async () => {
  // 取消当前的流式请求
  cancelStream()
  
  await chatStore.clearSession()
  ElMessage.success('已开始新对话')
}

// 查看专利详情
const viewPatent = (id) => {
  router.push(`/patent/detail/${id}`)
}

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

// 格式化时间
const formatTime = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

// 格式化回复（支持简单的 markdown）
const formatReply = (text) => {
  if (!text) return ''
  
  // 转义HTML
  let formatted = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  
  // 简单的格式化
  formatted = formatted
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')  // 粗体
    .replace(/\n/g, '<br>')  // 换行
  
  return formatted
}

// 监听消息变化，自动滚动
watch(messages, () => {
  scrollToBottom()
}, { deep: true })

// 初始化 Store（从 localStorage 恢复状态或加载建议）
const initializeChat = async () => {
  if (!isInitialized.value) {
    await chatStore.initialize()
    isInitialized.value = true
    // 如果有历史消息，滚动到底部
    if (messages.value.length > 0) {
      scrollToBottom()
    }
  }
}

// 获取 layout-content 元素并动态控制滚动（直接操作 style，不依赖 scoped class）
const getLayoutContent = () => document.querySelector('.layout-content')

const disableParentScroll = () => {
  const el = getLayoutContent()
  if (el) el.style.overflow = 'hidden'
}

const enableParentScroll = () => {
  const el = getLayoutContent()
  if (el) el.style.overflow = ''
}

onMounted(() => {
  initializeChat()
  disableParentScroll()
})

// keep-alive 激活时恢复状态
onActivated(() => {
  // 如果尚未初始化，则初始化
  if (!isInitialized.value) {
    initializeChat()
  }
  // 滚动到底部
  scrollToBottom()
  disableParentScroll()
})

// keep-alive 停用时保存状态
onDeactivated(() => {
  // 取消流式请求
  cancelStream()
  // 保存当前状态
  chatStore.saveToStorage()
  enableParentScroll()
})

// 组件卸载时取消流式请求，并销毁所有图谱实例
onUnmounted(() => {
  cancelStream()
  enableParentScroll()
  Object.values(graphInstances).forEach(inst => {
    try { inst.dispose() } catch (e) { /* ignore */ }
  })
})
</script>

<style lang="scss" scoped>
.patent-chat-page {
  // 用负 margin 完全抵消 layout-content 四周 24px padding，撑满内容区
  margin: -24px;
  // layout-content 已是 overflow:hidden + flex:1，高度自动 = 100vh - header - footer
  // 子元素用 100% 继承父高度（比固定 calc 更可靠）
  height: calc(100% + 48px); // 补偿上下各 24px 的 margin
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  overflow: hidden;
}

// 聊天主体区域布局 - 填满剩余高度
.chat-main-area {
  flex: 1;
  min-height: 0;
  display: flex;
  gap: var(--space-4);
}

// 会话侧边栏包装器
.session-sidebar-wrapper {
  flex-shrink: 0;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

// 对话容器
.chat-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  height: 100%;
  padding: 0;
  overflow: hidden;
}

// 助手信息头部
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-5);
  border-bottom: 1px solid var(--color-border);
  background: var(--color-bg-primary);
  flex-shrink: 0;
}

.assistant-identity {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.assistant-avatar {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #6366F1 0%, #3B82F6 100%);
  border-radius: 12px;
  color: #fff;
  box-shadow: 0 2px 8px rgba(99, 102, 241, 0.3);
}

.assistant-info {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.assistant-name-row {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.assistant-name {
  font-family: var(--font-heading);
  font-size: var(--text-base);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  letter-spacing: 0.02em;
}

.assistant-badge {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: #6366F1;
  background: rgba(99, 102, 241, 0.1);
  padding: 1px 8px;
  border-radius: var(--radius-full);
  border: 1px solid rgba(99, 102, 241, 0.2);
}

.assistant-status {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  color: var(--color-text-muted);

  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--color-success);
    flex-shrink: 0;
  }

  &.thinking {
    color: #F59E0B;

    .status-dot {
      background: #F59E0B;
      animation: pulse-dot 1s ease-in-out infinite;
    }
  }
}

@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.5; transform: scale(0.8); }
}

.assistant-desc {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin: 0;
  line-height: 1.4;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;

  .el-button {
    display: flex;
    align-items: center;
    gap: var(--space-1);
    font-size: var(--text-xs);
  }
}

// 消息列表
.message-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-6);
  
  &::-webkit-scrollbar {
    width: 6px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  &::-webkit-scrollbar-thumb {
    background: var(--color-border);
    border-radius: 3px;
  }
}

// 欢迎区域
.welcome-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-12) var(--space-6);
  text-align: center;

  .welcome-icon {
    width: 80px;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-primary) 100%);
    border-radius: 50%;
    margin-bottom: var(--space-6);

    svg {
      color: #fff;
    }
  }

  h2 {
    font-family: var(--font-heading);
    font-size: var(--text-2xl);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-3) 0;
  }

  p {
    font-size: var(--text-base);
    color: var(--color-text-secondary);
    max-width: 400px;
    margin: 0 0 var(--space-8) 0;
    line-height: var(--leading-relaxed);
  }
}

// 建议问题列表
.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  width: 100%;
  max-width: 400px;
}

.suggestion-btn {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-family: var(--font-body);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  text-align: left;

  svg {
    flex-shrink: 0;
    color: var(--color-accent);
  }

  &:hover {
    background: var(--color-bg-tertiary);
    border-color: var(--color-accent);
    color: var(--color-accent);
    transform: translateX(4px);
  }
}

// 消息项
.message-item {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-5);

  &.user {
    flex-direction: row-reverse;

    .message-avatar {
      background: var(--color-accent);
      color: #fff;
    }

    .message-content {
      align-items: flex-end;
    }

    .message-body {
      background: var(--color-accent);
      color: #fff;
      border-radius: var(--radius-lg) var(--radius-sm) var(--radius-lg) var(--radius-lg);
    }
  }

  &.assistant {
    .message-avatar {
      background: linear-gradient(135deg, #6366F1 0%, #3B82F6 100%);
      color: #fff;
      border-radius: 10px;
      box-shadow: 0 2px 6px rgba(99, 102, 241, 0.25);
    }

    .message-body {
      background: var(--color-bg-secondary);
      border-radius: var(--radius-sm) var(--radius-lg) var(--radius-lg) var(--radius-lg);
    }
  }
}

.message-avatar {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.message-content {
  display: flex;
  flex-direction: column;
  max-width: 75%;
}

.message-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  margin-bottom: var(--space-1);
  padding: 0 var(--space-2);
}

.message-sender {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
}

.message-time {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.message-body {
  padding: var(--space-3) var(--space-4);
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
}

.user-message {
  white-space: pre-wrap;
  word-break: break-word;
}

.reply-text {
  white-space: pre-wrap;
  word-break: break-word;
  color: var(--color-text-primary);

  :deep(strong) {
    font-weight: var(--font-semibold);
  }

  // 流式输出时的样式
  &.streaming {
    .streaming-cursor {
      display: inline-block;
      color: var(--color-accent);
      animation: blink 1s step-end infinite;
      margin-left: 2px;
    }
  }
}

// 光标闪烁动画
@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

// 工具调用信息
.tool-calls {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--color-border);
}

.tool-call-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-muted);
  margin-bottom: var(--space-2);
}

.tool-call-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2);
  background: var(--color-bg-primary);
  border-radius: var(--radius-sm);
  margin-bottom: var(--space-1);
  font-size: var(--text-xs);

  .tool-name {
    font-family: var(--font-mono);
    color: var(--color-accent);
    font-weight: var(--font-medium);
  }

  .tool-result {
    color: var(--color-text-muted);
  }
}

// 专利结果卡片
.patent-results {
  margin-top: var(--space-4);
}

.results-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-3);

  svg {
    color: var(--color-accent);
  }
}

.patent-cards {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.patent-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  padding: var(--space-3);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);

  &:hover {
    border-color: var(--color-accent);
    box-shadow: var(--shadow-sm);
  }
}

.patent-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.patent-no {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.relevance-score {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: var(--font-bold);
  color: var(--color-success);
  background: #ECFDF5;
  padding: 2px 6px;
  border-radius: var(--radius-sm);
}

.patent-title {
  font-family: var(--font-heading);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2) 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.patent-applicant {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  margin: 0 0 var(--space-2) 0;
}

.patent-abstract {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
  line-height: var(--leading-relaxed);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.patent-entities {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  margin-top: var(--space-2);
}

.entity-tag {
  font-size: 10px;
  padding: 2px 6px;
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-xs);
  color: var(--color-text-muted);
}

.entity-more {
  font-size: 10px;
  color: var(--color-text-muted);
}

.more-results {
  text-align: center;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  padding: var(--space-2);
}

// 后续建议
.follow-suggestions {
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--color-border);
}

.suggestion-label {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  display: block;
  margin-bottom: var(--space-2);
}

.follow-btn {
  display: inline-block;
  padding: var(--space-2) var(--space-3);
  margin-right: var(--space-2);
  margin-bottom: var(--space-2);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-family: var(--font-body);
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);

  &:hover {
    border-color: var(--color-accent);
    color: var(--color-accent);
  }
}

// 加载动画
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 4px;

  span:not(.typing-text) {
    width: 8px;
    height: 8px;
    background: var(--color-text-muted);
    border-radius: 50%;
    animation: typing 1.4s ease-in-out infinite;

    &:nth-child(2) {
      animation-delay: 0.2s;
    }

    &:nth-child(3) {
      animation-delay: 0.4s;
    }
  }

  .typing-text {
    margin-left: var(--space-2);
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }
}

@keyframes typing {
  0%, 100% {
    opacity: 0.4;
    transform: scale(1);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
}

// 图谱嵌入组件
.graph-embed {
  margin-top: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  background: var(--color-bg-primary);
}

.graph-embed-header {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border);

  svg {
    color: var(--color-accent);
    flex-shrink: 0;
  }

  .graph-stats {
    margin-left: auto;
    font-size: 10px;
    color: var(--color-text-muted);
    font-weight: var(--font-normal);
  }

  .graph-link {
    font-size: 10px;
    color: var(--color-accent);
    cursor: pointer;
    margin-left: var(--space-2);
    white-space: nowrap;
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.graph-embed-canvas {
  width: 100%;
  height: 260px;
}

// 输入区域
.input-area {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--color-border);
  background: var(--color-bg-secondary);
}

.input-wrapper {
  display: flex;
  gap: var(--space-3);
  align-items: flex-end;

  .el-input {
    flex: 1;
  }

  :deep(.el-textarea__inner) {
    border-radius: var(--radius-lg);
    resize: none;
    font-family: var(--font-body);
    padding: var(--space-3) var(--space-4);
  }

  .el-button {
    height: 44px;
    min-width: 88px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--space-2);
  }
}

.input-tips {
  margin-top: var(--space-2);
  text-align: right;
  
  span {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }
}

// 响应式
@media (max-width: 768px) {
  .patent-chat-page {
    // 移动端同样撑满去掉 header+footer 的剩余高度，负 margin 已抵消 padding
    height: calc(100vh - 64px - 48px);
    overflow: hidden;
  }

  .chat-main-area {
    flex-direction: column;
    flex: 1;
    min-height: 0;
    height: 100%;
  }

  .session-sidebar-wrapper {
    width: 100% !important;
    max-height: 160px;
    flex-shrink: 0;
  }

  .chat-container {
    flex: 1;
    min-height: 0;
    height: auto;
  }

  .message-content {
    max-width: 88%;
  }

  .suggestion-btn {
    font-size: var(--text-xs);
  }
}
</style>
