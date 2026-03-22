<template>
  <div class="user-management-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-title">
        <h2>用户管理</h2>
        <p class="subtitle">管理系统用户账号、权限、登录状态及数据统计</p>
      </div>
      <el-button type="primary" :loading="tableLoading" @click="loadAll">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px">
          <polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 1 0 .49-4.95"/>
        </svg>
        刷新数据
      </el-button>
    </div>

    <!-- 统计面板 -->
    <div class="stats-grid" v-loading="statsLoading">
      <div class="stat-card">
        <div class="stat-icon total-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.totalUsers ?? '-' }}</div>
          <div class="stat-label">总用户数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon online-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value online-value">{{ stats.onlineUsers ?? '-' }}</div>
          <div class="stat-label">当前在线</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon new-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
            <line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ stats.todayNewUsers ?? '-' }}</div>
          <div class="stat-label">今日新增</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon disabled-icon">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-value disabled-value">{{ stats.disabledUsers ?? '-' }}</div>
          <div class="stat-label">已禁用账号</div>
        </div>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="toolbar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索昵称或用户名..."
        clearable
        style="width: 280px"
      >
        <template #prefix>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
        </template>
      </el-input>
      <el-select v-model="filterRole" placeholder="筛选角色" clearable style="width: 130px">
        <el-option label="全部" value="" />
        <el-option label="管理员" value="admin" />
        <el-option label="普通用户" value="user" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="筛选状态" clearable style="width: 130px">
        <el-option label="全部" value="" />
        <el-option label="启用" value="1" />
        <el-option label="禁用" value="0" />
      </el-select>
    </div>

    <!-- 用户列表表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        :data="filteredUsers"
        v-loading="tableLoading"
        row-key="id"
        stripe
        class="user-table"
        @row-click="openDrawer"
      >
        <!-- 昵称 -->
        <el-table-column label="昵称" min-width="120">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" class="table-avatar">
                {{ row.nickname?.charAt(0) || row.username?.charAt(0) || 'U' }}
              </el-avatar>
              <span class="cell-nickname">{{ row.nickname || row.username }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 用户名 -->
        <el-table-column label="用户名" prop="username" min-width="120">
          <template #default="{ row }">
            <span class="cell-username">@{{ row.username }}</span>
          </template>
        </el-table-column>

        <!-- 角色 -->
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'" size="small" effect="plain">
              {{ row.role === 'admin' ? '管理员' : '用户' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 状态 -->
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 在线状态 -->
        <el-table-column label="在线" width="80" align="center">
          <template #default="{ row }">
            <span :class="['online-dot', row.online ? 'is-online' : 'is-offline']"></span>
          </template>
        </el-table-column>

        <!-- 注册时间 -->
        <el-table-column label="注册时间" min-width="160">
          <template #default="{ row }">
            <span class="cell-time">{{ formatTime(row.createdAt) }}</span>
          </template>
        </el-table-column>

        <!-- 专利数 -->
        <el-table-column label="专利" width="70" align="center">
          <template #default="{ row }">
            <span class="cell-count">{{ row.patentCount ?? 0 }}</span>
          </template>
        </el-table-column>

        <!-- 匹配次数 -->
        <el-table-column label="匹配" width="70" align="center">
          <template #default="{ row }">
            <span class="cell-count">{{ row.matchCount ?? 0 }}</span>
          </template>
        </el-table-column>

        <!-- 收藏数 -->
        <el-table-column label="收藏" width="70" align="center">
          <template #default="{ row }">
            <span class="cell-count">{{ row.favoriteCount ?? 0 }}</span>
          </template>
        </el-table-column>

        <!-- 操作列 -->
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-btns" @click.stop>
              <!-- 踢出 -->
              <el-tooltip content="踢出登录" placement="top">
                <el-button
                  size="small"
                  :disabled="!row.online || row.id === currentUserId || kickoutLoading"
                  :loading="kickoutLoading && drawerUser?.id === row.id"
                  @click="handleKickout(row)"
                  circle
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                    <polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/>
                  </svg>
                </el-button>
              </el-tooltip>

              <!-- 禁用/启用 -->
              <el-tooltip :content="row.status === 1 ? '禁用账号' : '启用账号'" placement="top">
                <el-button
                  size="small"
                  :type="row.status === 1 ? 'warning' : 'success'"
                  :disabled="row.id === currentUserId || statusLoading"
                  @click="handleToggleStatus(row)"
                  circle
                  plain
                >
                  <svg v-if="row.status === 1" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <circle cx="12" cy="12" r="10"/><line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                  </svg>
                  <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <polyline points="20 6 9 17 4 12"/>
                  </svg>
                </el-button>
              </el-tooltip>

              <!-- 角色切换 -->
              <el-tooltip :content="row.role === 'admin' ? '降为普通用户' : '提升为管理员'" placement="top">
                <el-button
                  size="small"
                  :type="row.role === 'admin' ? 'danger' : 'primary'"
                  :disabled="row.id === currentUserId || roleLoading"
                  @click="handleToggleRole(row)"
                  circle
                  plain
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                  </svg>
                </el-button>
              </el-tooltip>

              <!-- 重置密码 -->
              <el-tooltip content="重置密码" placement="top">
                <el-button
                  size="small"
                  type="info"
                  @click="openResetDialog(row)"
                  circle
                  plain
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                    <path d="M7 11V7a5 5 0 0 1 9.9-1"/>
                  </svg>
                </el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 用户详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="`用户详情 · ${drawerUser?.nickname || drawerUser?.username || ''}`"
      direction="rtl"
      size="380px"
    >
      <div class="drawer-content" v-if="drawerUser">
        <div class="drawer-avatar-section">
          <el-avatar :size="64" class="drawer-avatar">
            {{ drawerUser.nickname?.charAt(0) || drawerUser.username?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="drawer-name">
            <div class="drawer-nickname">{{ drawerUser.nickname || drawerUser.username }}</div>
            <div class="drawer-username">@{{ drawerUser.username }}</div>
          </div>
        </div>

        <el-divider />

        <div class="drawer-detail-list">
          <div class="drawer-item">
            <span class="drawer-label">用户ID</span>
            <span class="drawer-value">{{ drawerUser.id }}</span>
          </div>
          <div class="drawer-item">
            <span class="drawer-label">角色</span>
            <el-tag :type="drawerUser.role === 'admin' ? 'danger' : 'primary'" size="small" effect="plain">
              {{ drawerUser.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </div>
          <div class="drawer-item">
            <span class="drawer-label">账号状态</span>
            <el-tag :type="drawerUser.status === 1 ? 'success' : 'info'" size="small">
              {{ drawerUser.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </div>
          <div class="drawer-item">
            <span class="drawer-label">在线状态</span>
            <span :class="['online-badge', drawerUser.online ? 'is-online' : 'is-offline']">
              {{ drawerUser.online ? '● 在线' : '○ 离线' }}
            </span>
          </div>
          <div class="drawer-item" v-if="drawerUser.online">
            <span class="drawer-label">Token剩余</span>
            <span class="drawer-value">{{ formatTimeout(drawerUser.tokenTimeout) }}</span>
          </div>
          <div class="drawer-item">
            <span class="drawer-label">注册时间</span>
            <span class="drawer-value">{{ formatTime(drawerUser.createdAt) }}</span>
          </div>
        </div>

        <el-divider content-position="left">数据统计</el-divider>

        <div class="drawer-stats">
          <div class="drawer-stat-item">
            <div class="drawer-stat-value">{{ drawerUser.patentCount ?? 0 }}</div>
            <div class="drawer-stat-label">上传专利</div>
          </div>
          <div class="drawer-stat-item">
            <div class="drawer-stat-value">{{ drawerUser.matchCount ?? 0 }}</div>
            <div class="drawer-stat-label">匹配次数</div>
          </div>
          <div class="drawer-stat-item">
            <div class="drawer-stat-value">{{ drawerUser.favoriteCount ?? 0 }}</div>
            <div class="drawer-stat-label">收藏专利</div>
          </div>
        </div>

        <el-divider />

        <div class="drawer-actions">
          <el-button
            :disabled="!drawerUser.online || drawerUser.id === currentUserId"
            :loading="kickoutLoading"
            @click="handleKickout(drawerUser)"
            style="width:100%"
          >踢出登录</el-button>
          <el-button
            :type="drawerUser.status === 1 ? 'warning' : 'success'"
            :disabled="drawerUser.id === currentUserId"
            :loading="statusLoading"
            @click="handleToggleStatus(drawerUser)"
            style="width:100%"
          >{{ drawerUser.status === 1 ? '禁用账号' : '启用账号' }}</el-button>
          <el-button
            :type="drawerUser.role === 'admin' ? 'danger' : 'primary'"
            :disabled="drawerUser.id === currentUserId"
            :loading="roleLoading"
            @click="handleToggleRole(drawerUser)"
            style="width:100%"
          >{{ drawerUser.role === 'admin' ? '降为普通用户' : '提升为管理员' }}</el-button>
          <el-button type="info" @click="openResetDialog(drawerUser)" style="width:100%">重置密码</el-button>
        </div>
      </div>
    </el-drawer>

    <!-- 重置密码弹窗 -->
    <el-dialog
      v-model="resetDialogVisible"
      :title="`重置密码 · ${resetTargetUser?.nickname || resetTargetUser?.username || ''}`"
      width="420px"
      :close-on-click-modal="false"
      @closed="handleResetPasswordDialogClose"
    >
      <el-alert
        v-if="resetTargetUser"
        :title="`将重置「${resetTargetUser.nickname || resetTargetUser.username}」的密码，无需验证旧密码`"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 16px"
      />
      <el-form
        ref="resetFormRef"
        :model="resetForm"
        :rules="resetRules"
        label-position="top"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="resetForm.newPassword" type="password" placeholder="请输入新密码（6-20位）" show-password clearable />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="resetForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password clearable />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetDialogVisible = false">取消</el-button>
        <el-button type="danger" :loading="resetLoading" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { authApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const currentUserId = computed(() => userStore.userInfo?.id)

// ==================== 统计面板 ====================
const statsLoading = ref(false)
const stats = reactive({
  totalUsers: null,
  onlineUsers: null,
  todayNewUsers: null,
  disabledUsers: null
})

const loadStats = async () => {
  statsLoading.value = true
  try {
    const res = await authApi.getUserStats()
    if (res.code === 200 && res.data) {
      Object.assign(stats, res.data)
    }
  } catch (e) {
    // 统计数据加载失败不阻塞主流程
    console.warn('获取统计数据失败:', e)
  } finally {
    statsLoading.value = false
  }
}

// ==================== 用户列表 ====================
const tableLoading = ref(false)
const allUsers = ref([])
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref('')

const filteredUsers = computed(() => {
  let list = allUsers.value
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase()
    list = list.filter(u =>
      u.username?.toLowerCase().includes(kw) ||
      u.nickname?.toLowerCase().includes(kw)
    )
  }
  if (filterRole.value !== '') {
    list = list.filter(u => u.role === filterRole.value)
  }
  if (filterStatus.value !== '') {
    list = list.filter(u => String(u.status) === filterStatus.value)
  }
  return list
})

const loadUsers = async () => {
  tableLoading.value = true
  try {
    const res = await authApi.getAllUsers()
    if (res.code === 200) {
      allUsers.value = res.data || []
    } else {
      ElMessage.error(res.message || '获取用户列表失败')
    }
  } catch (e) {
    ElMessage.error('获取用户列表失败，请检查网络连接')
  } finally {
    tableLoading.value = false
  }
}

const loadAll = () => {
  loadStats()
  loadUsers()
}

// ==================== 用户详情抽屉 ====================
const drawerVisible = ref(false)
const drawerUser = ref(null)

const openDrawer = (row) => {
  // 使用浅拷贝，同时保持与 allUsers 中对象的同步
  drawerUser.value = row
  drawerVisible.value = true
}

// 同步更新 allUsers 中目标用户及 drawerUser 的字段
const syncUserData = (userId, patch) => {
  const target = allUsers.value.find(u => u.id === userId)
  if (target) {
    Object.assign(target, patch)
  }
  // drawerUser 和 allUsers 中的对象是同一引用，无需再单独更新
}

// ==================== 踢出登录 ====================
const kickoutLoading = ref(false)
const handleKickout = async (user) => {
  try {
    await ElMessageBox.confirm(
      `确定要踢出用户「${user.nickname || user.username}」的登录吗？该用户 Token 将立即失效。`,
      '确认踢出',
      { confirmButtonText: '确认踢出', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  kickoutLoading.value = true
  try {
    const res = await authApi.kickoutUser(user.id)
    if (res.code === 200) {
      ElMessage.success(`已踢出「${user.nickname || user.username}」的登录`)
      syncUserData(user.id, { online: false, tokenTimeout: -1 })
    } else {
      ElMessage.error(res.message || '踢出失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '踢出失败')
  } finally {
    kickoutLoading.value = false
  }
}

// ==================== 禁用/启用账号 ====================
const statusLoading = ref(false)
const handleToggleStatus = async (user) => {
  const isDisable = user.status === 1
  const actionText = isDisable ? '禁用' : '启用'
  try {
    await ElMessageBox.confirm(
      `确定要${actionText}用户「${user.nickname || user.username}」的账号吗？${isDisable ? '禁用后该用户将无法登录，且会被立即踢出。' : ''}`,
      `确认${actionText}`,
      { confirmButtonText: `确认${actionText}`, cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  statusLoading.value = true
  try {
    const res = isDisable
      ? await authApi.disableUser(user.id)
      : await authApi.enableUser(user.id)
    if (res.code === 200) {
      ElMessage.success(`账号已${actionText}`)
      const patch = { status: isDisable ? 0 : 1 }
      if (isDisable) patch.online = false
      syncUserData(user.id, patch)
      loadStats()
    } else {
      ElMessage.error(res.message || `${actionText}失败`)
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || `${actionText}失败`)
  } finally {
    statusLoading.value = false
  }
}

// ==================== 角色切换 ====================
const roleLoading = ref(false)
const handleToggleRole = async (user) => {
  const newRole = user.role === 'admin' ? 'user' : 'admin'
  const actionText = newRole === 'admin' ? '提升为管理员' : '降为普通用户'
  try {
    await ElMessageBox.confirm(
      `确定要将用户「${user.nickname || user.username}」${actionText}吗？`,
      '确认角色切换',
      { confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  roleLoading.value = true
  try {
    const res = await authApi.updateUserRole({ userId: user.id, role: newRole })
    if (res.code === 200) {
      ElMessage.success(`角色已切换为「${newRole === 'admin' ? '管理员' : '普通用户'}」`)
      syncUserData(user.id, { role: newRole })
    } else {
      ElMessage.error(res.message || '角色切换失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '角色切换失败')
  } finally {
    roleLoading.value = false
  }
}

// ==================== 重置密码 ====================
const resetDialogVisible = ref(false)
const resetTargetUser = ref(null)
const resetLoading = ref(false)
const resetFormRef = ref(null)
const resetForm = reactive({ newPassword: '', confirmPassword: '' })

const validateConfirm = (rule, value, callback) => {
  if (value !== resetForm.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const resetRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const openResetDialog = (user) => {
  resetTargetUser.value = user
  resetForm.newPassword = ''
  resetForm.confirmPassword = ''
  drawerVisible.value = false
  resetDialogVisible.value = true
  // 等待 DOM 更新后清空表单验证状态
  nextTick(() => {
    resetFormRef.value?.clearValidate()
  })
}

const handleResetPasswordDialogClose = () => {
  resetFormRef.value?.clearValidate()
}

const handleResetPassword = async () => {
  if (!resetFormRef.value) return
  let valid = false
  try {
    await resetFormRef.value.validate()
    valid = true
  } catch {
    return
  }
  if (!valid) return

  resetLoading.value = true
  try {
    const res = await authApi.adminResetPassword({
      userId: resetTargetUser.value.id,
      newPassword: resetForm.newPassword,
      confirmPassword: resetForm.confirmPassword
    })
    if (res.code === 200) {
      ElMessage.success(`「${resetTargetUser.value.nickname || resetTargetUser.value.username}」的密码已重置`)
      resetDialogVisible.value = false
    } else {
      ElMessage.error(res.message || '重置失败')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || e.message || '重置失败')
  } finally {
    resetLoading.value = false
  }
}

// ==================== 工具方法 ====================
const formatTime = (time) => {
  if (!time) return '-'
  try {
    const d = new Date(time)
    if (isNaN(d.getTime())) return '-'
    return d.toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
  } catch {
    return '-'
  }
}

const formatTimeout = (seconds) => {
  if (seconds == null || seconds < 0) return '已过期'
  if (seconds === 0) return '即将过期'
  if (seconds > 3600) return `${Math.floor(seconds / 3600)} 小时`
  if (seconds > 60) return `${Math.floor(seconds / 60)} 分钟`
  return `${seconds} 秒`
}

onMounted(() => {
  loadAll()
})
</script>

<style lang="scss" scoped>
.user-management-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);

  .header-title {
    h2 {
      font-family: var(--font-heading);
      font-size: var(--text-2xl);
      font-weight: var(--font-bold);
      color: var(--color-text-primary);
      margin-bottom: var(--space-1);
    }
    .subtitle {
      font-size: var(--text-sm);
      color: var(--color-text-muted);
      margin: 0;
    }
  }
}

// 统计面板
.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);

  @media (max-width: 900px) {
    grid-template-columns: repeat(2, 1fr);
  }
}

.stat-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  transition: box-shadow var(--duration-fast) var(--ease-default);

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.total-icon  { background: rgba(29, 78, 216, 0.1); color: var(--color-accent); }
  &.online-icon { background: rgba(16, 185, 129, 0.1); color: #10b981; }
  &.new-icon    { background: rgba(245, 158, 11, 0.1); color: #f59e0b; }
  &.disabled-icon { background: rgba(239, 68, 68, 0.1); color: #ef4444; }
}

.stat-info {
  .stat-value {
    font-size: var(--text-2xl);
    font-weight: var(--font-bold);
    color: var(--color-text-primary);
    line-height: 1;
    margin-bottom: var(--space-1);

    &.online-value { color: #10b981; }
    &.disabled-value { color: #ef4444; }
  }
  .stat-label {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }
}

// 工具栏
.toolbar {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
  flex-wrap: wrap;
}

// 表格
.table-card {
  :deep(.el-card__body) { padding: 0; }
}

.user-table {
  width: 100%;
  cursor: pointer;
  :deep(.el-table__row:hover) td { background: var(--color-bg-secondary); }
}

.user-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.table-avatar {
  flex-shrink: 0;
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
  color: #fff;
  font-weight: var(--font-semibold);
  font-size: var(--text-sm);
}

.cell-nickname {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
}

.cell-username {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.cell-time {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.cell-count {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
}

// 在线状态圆点
.online-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 50%;

  &.is-online  { background: #10b981; box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2); }
  &.is-offline { background: var(--color-border-dark); }
}

// 操作按钮
.action-btns {
  display: flex;
  justify-content: center;
  gap: var(--space-1);
}

// 抽屉
.drawer-content {
  padding: 0 var(--space-2);
}

.drawer-avatar-section {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) 0;
}

.drawer-avatar {
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
  color: #fff;
  font-weight: var(--font-bold);
  font-size: var(--text-xl);
  flex-shrink: 0;
}

.drawer-name {
  .drawer-nickname {
    font-size: var(--text-lg);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
  }
  .drawer-username {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }
}

.drawer-detail-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.drawer-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.drawer-value {
  font-size: var(--text-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-medium);
}

.online-badge {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  &.is-online  { color: #10b981; }
  &.is-offline { color: var(--color-text-muted); }
}

.drawer-stats {
  display: flex;
  justify-content: space-around;
  text-align: center;
  padding: var(--space-2) 0;
}

.drawer-stat-item {
  .drawer-stat-value {
    font-size: var(--text-2xl);
    font-weight: var(--font-bold);
    color: var(--color-accent);
    line-height: 1;
    margin-bottom: var(--space-1);
  }
  .drawer-stat-label {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }
}

.drawer-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}
</style>
