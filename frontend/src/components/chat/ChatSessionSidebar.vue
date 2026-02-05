<template>
  <div class="session-sidebar" :class="{ collapsed: collapsed }">
    <!-- 侧边栏头部 -->
    <div class="sidebar-header">
      <h3 v-if="!collapsed">历史会话</h3>
      <el-button 
        v-if="!collapsed"
        type="primary" 
        size="small" 
        @click="handleNewSession"
      >
        <el-icon><Plus /></el-icon>
        新对话
      </el-button>
      <el-button 
        text 
        :icon="collapsed ? Expand : Fold" 
        @click="toggleCollapse"
      />
    </div>

    <!-- 标签页切换 -->
    <div v-if="!collapsed" class="tab-container">
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'active' }"
        @click="switchTab('active')"
      >
        <el-icon><ChatDotSquare /></el-icon>
        活跃
      </div>
      <div 
        class="tab-item" 
        :class="{ active: activeTab === 'archived' }"
        @click="switchTab('archived')"
      >
        <el-icon><FolderOpened /></el-icon>
        归档
      </div>
    </div>

    <!-- 活跃会话列表 -->
    <div v-if="!collapsed && activeTab === 'active'" class="session-list" v-loading="sessionsLoading">
      <div 
        v-for="session in sessions" 
        :key="session.sessionId"
        class="session-item"
        :class="{ active: currentSessionId === session.sessionId }"
        @click="handleSwitchSession(session.sessionId)"
      >
        <div class="session-info">
          <div class="session-title" :title="session.title || '新会话'">
            {{ session.title || '新会话' }}
          </div>
          <div class="session-meta">
            <span class="session-time">{{ formatTime(session.updatedAt) }}</span>
            <span class="session-count">{{ session.messageCount || 0 }} 条消息</span>
          </div>
        </div>
        <div class="session-actions" @click.stop>
          <el-dropdown trigger="click" @command="handleCommand($event, session)">
            <el-button text size="small" :icon="MoreFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="rename">
                  <el-icon><Edit /></el-icon>
                  重命名
                </el-dropdown-item>
                <el-dropdown-item command="archive">
                  <el-icon><FolderOpened /></el-icon>
                  归档
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>
                  <el-icon><Delete /></el-icon>
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="!sessionsLoading && sessions.length === 0" class="empty-state">
        <el-icon :size="32"><ChatDotSquare /></el-icon>
        <p>暂无历史会话</p>
        <p class="empty-tip">开始对话后，会话会自动保存在这里</p>
        <el-button type="primary" size="small" @click="handleNewSession">
          开始新对话
        </el-button>
      </div>
    </div>

    <!-- 归档会话列表 -->
    <div v-if="!collapsed && activeTab === 'archived'" class="session-list" v-loading="archivedSessionsLoading">
      <div 
        v-for="session in archivedSessions" 
        :key="session.sessionId"
        class="session-item archived"
      >
        <div class="session-info">
          <div class="session-title" :title="session.title || '新会话'">
            {{ session.title || '新会话' }}
          </div>
          <div class="session-meta">
            <span class="session-time">{{ formatTime(session.updatedAt) }}</span>
            <span class="session-count">{{ session.messageCount || 0 }} 条消息</span>
          </div>
        </div>
        <div class="session-actions" @click.stop>
          <el-dropdown trigger="click" @command="handleArchivedCommand($event, session)">
            <el-button text size="small" :icon="MoreFilled" />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="restore">
                  <el-icon><RefreshRight /></el-icon>
                  恢复
                </el-dropdown-item>
                <el-dropdown-item command="delete" divided>
                  <el-icon><Delete /></el-icon>
                  永久删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 归档空状态 -->
      <div v-if="!archivedSessionsLoading && archivedSessions.length === 0" class="empty-state">
        <el-icon :size="32"><FolderOpened /></el-icon>
        <p>暂无归档会话</p>
        <p class="empty-tip">归档的会话会显示在这里</p>
      </div>
    </div>

    <!-- 重命名对话框 -->
    <el-dialog
      v-model="renameDialogVisible"
      title="重命名会话"
      width="400px"
    >
      <el-input 
        v-model="newTitle" 
        placeholder="请输入新的会话标题"
        maxlength="100"
        show-word-limit
      />
      <template #footer>
        <el-button @click="renameDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmRename" :loading="renaming">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { storeToRefs } from 'pinia'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Plus, Expand, Fold, MoreFilled, Edit, Delete, 
  FolderOpened, ChatDotSquare, RefreshRight 
} from '@element-plus/icons-vue'
import { useChatStore } from '@/stores/chat'

const chatStore = useChatStore()
const { 
  sessions, 
  sessionsLoading, 
  sessionId: currentSessionId,
  archivedSessions,
  archivedSessionsLoading
} = storeToRefs(chatStore)

// 侧边栏折叠状态
const collapsed = ref(false)

// 当前激活的标签页
const activeTab = ref('active')

// 组件挂载时加载会话列表
onMounted(() => {
  chatStore.fetchSessions()
})

// 切换标签页
const switchTab = (tab) => {
  activeTab.value = tab
  if (tab === 'archived' && archivedSessions.value.length === 0) {
    chatStore.fetchArchivedSessions()
  }
}

// 重命名相关
const renameDialogVisible = ref(false)
const newTitle = ref('')
const renamingSessionId = ref(null)
const renaming = ref(false)

// 折叠/展开侧边栏
const toggleCollapse = () => {
  collapsed.value = !collapsed.value
}

// 创建新会话
const handleNewSession = () => {
  chatStore.createNewSession()
  activeTab.value = 'active'
}

// 切换会话
const handleSwitchSession = async (sessionId) => {
  if (sessionId === currentSessionId.value) return
  await chatStore.switchSession(sessionId)
}

// 处理活跃会话下拉菜单命令
const handleCommand = async (command, session) => {
  switch (command) {
    case 'rename':
      openRenameDialog(session)
      break
    case 'archive':
      await handleArchive(session)
      break
    case 'delete':
      await handleDelete(session)
      break
  }
}

// 处理归档会话下拉菜单命令
const handleArchivedCommand = async (command, session) => {
  switch (command) {
    case 'restore':
      await handleRestore(session)
      break
    case 'delete':
      await handleDeleteArchived(session)
      break
  }
}

// 打开重命名对话框
const openRenameDialog = (session) => {
  newTitle.value = session.title || ''
  renamingSessionId.value = session.sessionId
  renameDialogVisible.value = true
}

// 确认重命名
const confirmRename = async () => {
  if (!newTitle.value.trim()) {
    ElMessage.warning('标题不能为空')
    return
  }
  
  renaming.value = true
  try {
    const success = await chatStore.updateSessionTitleById(
      renamingSessionId.value, 
      newTitle.value.trim()
    )
    if (success) {
      ElMessage.success('重命名成功')
      renameDialogVisible.value = false
    } else {
      ElMessage.error('重命名失败')
    }
  } finally {
    renaming.value = false
  }
}

// 归档会话
const handleArchive = async (session) => {
  try {
    await ElMessageBox.confirm(
      `确定要归档会话"${session.title || '新会话'}"吗？`,
      '归档确认',
      { type: 'info' }
    )
    
    const success = await chatStore.archiveSessionById(session.sessionId)
    if (success) {
      ElMessage.success('已归档')
      // 刷新归档列表
      chatStore.fetchArchivedSessions()
    } else {
      ElMessage.error('归档失败')
    }
  } catch {
    // 用户取消
  }
}

// 恢复归档会话
const handleRestore = async (session) => {
  try {
    await ElMessageBox.confirm(
      `确定要恢复会话"${session.title || '新会话'}"吗？`,
      '恢复确认',
      { type: 'info' }
    )
    
    const success = await chatStore.restoreSessionById(session.sessionId)
    if (success) {
      ElMessage.success('已恢复')
    } else {
      ElMessage.error('恢复失败')
    }
  } catch {
    // 用户取消
  }
}

// 删除活跃会话
const handleDelete = async (session) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除会话"${session.title || '新会话'}"吗？删除后将无法恢复。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' }
    )
    
    const success = await chatStore.deleteSessionById(session.sessionId)
    if (success) {
      ElMessage.success('删除成功')
    } else {
      ElMessage.error('删除失败')
    }
  } catch {
    // 用户取消
  }
}

// 永久删除归档会话
const handleDeleteArchived = async (session) => {
  try {
    await ElMessageBox.confirm(
      `确定要永久删除会话"${session.title || '新会话'}"吗？此操作不可恢复！`,
      '永久删除',
      { type: 'error', confirmButtonText: '永久删除', confirmButtonClass: 'el-button--danger' }
    )
    
    const success = await chatStore.deleteSessionById(session.sessionId)
    if (success) {
      // 从归档列表中移除
      chatStore.archivedSessions = chatStore.archivedSessions.filter(
        s => s.sessionId !== session.sessionId
      )
      ElMessage.success('已永久删除')
    } else {
      ElMessage.error('删除失败')
    }
  } catch {
    // 用户取消
  }
}

// 格式化时间
const formatTime = (dateStr) => {
  if (!dateStr) return ''
  
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now - date
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  } else if (days === 1) {
    return '昨天'
  } else if (days < 7) {
    return `${days}天前`
  } else {
    return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  }
}
</script>

<style lang="scss" scoped>
.session-sidebar {
  width: 280px;
  height: 100%;
  background: var(--color-bg-secondary);
  border-right: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  transition: width var(--duration-normal) var(--ease-default);

  &.collapsed {
    width: 48px;

    .sidebar-header {
      padding: var(--space-3);
      justify-content: center;
    }
  }
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  border-bottom: 1px solid var(--color-border);

  h3 {
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }

  .el-button {
    &:first-of-type {
      flex-shrink: 0;
    }
  }
}

// 标签页容器
.tab-container {
  display: flex;
  border-bottom: 1px solid var(--color-border);
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  padding: var(--space-3);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  border-bottom: 2px solid transparent;

  &:hover {
    color: var(--color-text-secondary);
    background: var(--color-bg-tertiary);
  }

  &.active {
    color: var(--color-accent);
    border-bottom-color: var(--color-accent);
    font-weight: var(--font-medium);
  }

  .el-icon {
    font-size: 14px;
  }
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-2);

  &::-webkit-scrollbar {
    width: 4px;
  }

  &::-webkit-scrollbar-track {
    background: transparent;
  }

  &::-webkit-scrollbar-thumb {
    background: var(--color-border);
    border-radius: 2px;
  }
}

.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);
  margin-bottom: var(--space-1);

  &:hover {
    background: var(--color-bg-tertiary);

    .session-actions {
      opacity: 1;
    }
  }

  &.active {
    background: rgba(59, 130, 246, 0.1);
    border-left: 3px solid var(--color-accent);

    .session-title {
      color: var(--color-text-primary);
      font-weight: var(--font-semibold);
    }

    .session-meta {
      color: var(--color-text-secondary);
    }
  }

  &.archived {
    opacity: 0.8;
    
    .session-title {
      color: var(--color-text-secondary);
    }
  }
}

.session-info {
  flex: 1;
  min-width: 0;
}

.session-title {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: var(--space-1);
}

.session-meta {
  display: flex;
  gap: var(--space-2);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.session-actions {
  opacity: 0;
  transition: opacity var(--duration-fast) var(--ease-default);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-8) var(--space-4);
  color: var(--color-text-muted);
  text-align: center;

  .el-icon {
    margin-bottom: var(--space-3);
    color: var(--color-border);
  }

  p {
    margin: 0 0 var(--space-2) 0;
    font-size: var(--text-sm);
  }

  .empty-tip {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    margin-bottom: var(--space-4);
  }
}

.loading-tip {
  text-align: center;
  padding: var(--space-4);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}
</style>
