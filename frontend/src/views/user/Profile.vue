<template>
  <div class="page-container profile-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <h1 class="page-title">个人信息</h1>
    </div>
    
    <div class="profile-content">
      <!-- 用户卡片 -->
      <div class="card user-card">
        <div class="user-header">
          <div class="user-avatar">
            {{ (userStore.nickname || userStore.username)?.charAt(0)?.toUpperCase() || 'U' }}
          </div>
          <div class="user-info">
            <h2 class="user-name">{{ userStore.nickname || userStore.username }}</h2>
            <span class="user-role" :class="userStore.userInfo?.role === 'admin' ? 'admin' : 'user'">
              {{ userStore.userInfo?.role === 'admin' ? '管理员' : '研究员' }}
            </span>
          </div>
        </div>
        
        <div class="user-stats">
          <div class="stat-item">
            <span class="stat-value">{{ userStore.userInfo?.patentCount || 0 }}</span>
            <span class="stat-label">上传专利</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">{{ userStore.userInfo?.matchCount || 0 }}</span>
            <span class="stat-label">匹配分析</span>
          </div>
          <div class="stat-item">
            <span class="stat-value">
              <span class="status-dot" :class="userStore.userInfo?.status === 1 ? 'active' : 'inactive'"></span>
              {{ userStore.userInfo?.status === 1 ? '正常' : '禁用' }}
            </span>
            <span class="stat-label">账号状态</span>
          </div>
        </div>
      </div>
      
      <!-- 信息详情 -->
      <div class="card info-card">
        <div class="card-header">
          <h3 class="card-title">账号信息</h3>
          <el-button type="primary" text @click="showEditDialog">
            <el-icon>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
              </svg>
            </el-icon>
            编辑
          </el-button>
        </div>
        
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">用户名</span>
            <span class="info-value code">{{ userStore.username }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">昵称</span>
            <span class="info-value">{{ userStore.nickname || '—' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">角色</span>
            <span class="info-value">{{ userStore.userInfo?.role === 'admin' ? '管理员' : '普通用户' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value">{{ formatTime(userStore.userInfo?.createdAt) }}</span>
          </div>
        </div>
      </div>
      
      <!-- 安全设置 -->
      <div class="card security-card">
        <div class="card-header">
          <h3 class="card-title">安全设置</h3>
        </div>
        
        <div class="security-item">
          <div class="security-info">
            <div class="security-icon">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <div class="security-text">
              <span class="security-title">登录密码</span>
              <span class="security-desc">定期修改密码可以保护账号安全</span>
            </div>
          </div>
          <el-button @click="showPasswordDialog">修改密码</el-button>
        </div>
      </div>
    </div>
    
    <!-- 修改信息对话框 -->
    <el-dialog v-model="editDialogVisible" title="修改个人信息" width="420px" class="profile-dialog">
      <el-form :model="editForm" :rules="editRules" ref="editFormRef" label-position="top">
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit" :loading="editLoading">保存</el-button>
      </template>
    </el-dialog>
    
    <!-- 修改密码对话框 -->
    <el-dialog v-model="passwordDialogVisible" title="修改密码" width="420px" class="profile-dialog">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请确认新密码" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handlePasswordSubmit" :loading="passwordLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

// 修改信息
const editDialogVisible = ref(false)
const editLoading = ref(false)
const editFormRef = ref(null)
const editForm = reactive({
  nickname: ''
})

const editRules = {
  nickname: [
    { max: 20, message: '昵称最多20个字符', trigger: 'blur' }
  ]
}

const showEditDialog = () => {
  editForm.nickname = userStore.nickname || ''
  editDialogVisible.value = true
}

const handleEditSubmit = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    editLoading.value = true
    try {
      ElMessage.warning('修改信息功能暂未实现')
      editDialogVisible.value = false
    } finally {
      editLoading.value = false
    }
  })
}

// 修改密码
const passwordDialogVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const showPasswordDialog = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordDialogVisible.value = true
}

const handlePasswordSubmit = async () => {
  if (!passwordFormRef.value) return
  
  await passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    passwordLoading.value = true
    try {
      ElMessage.warning('修改密码功能暂未实现')
      passwordDialogVisible.value = false
    } finally {
      passwordLoading.value = false
    }
  })
}

const formatTime = (time) => {
  if (!time) return '—'
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<style lang="scss" scoped>
.profile-page {
  max-width: 800px;
  margin: 0 auto;
}

.profile-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

// 用户卡片
.user-card {
  text-align: center;
}

.user-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  padding-bottom: var(--space-6);
  border-bottom: 1px solid var(--color-border-light);
}

.user-avatar {
  width: 80px;
  height: 80px;
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
}

.user-info {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-2);
}

.user-name {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin: 0;
}

.user-role {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-full);

  &.admin {
    background-color: #FEE2E2;
    color: var(--color-danger);
  }

  &.user {
    background-color: #EFF6FF;
    color: var(--color-accent);
  }
}

.user-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  padding-top: var(--space-6);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-1);
}

.stat-value {
  font-family: var(--font-heading);
  font-size: var(--text-xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

  &.active {
    background-color: var(--color-success);
  }

  &.inactive {
    background-color: var(--color-danger);
  }
}

// 信息卡片
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border-light);
}

.card-title {
  font-family: var(--font-heading);
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-5);
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.info-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.info-value {
  font-size: var(--text-base);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);

  &.code {
    font-family: var(--font-mono);
  }
}

// 安全设置
.security-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4);
  background-color: var(--color-bg-secondary);
  border-radius: var(--radius-md);
}

.security-info {
  display: flex;
  align-items: center;
  gap: var(--space-4);
}

.security-icon {
  width: 44px;
  height: 44px;
  background-color: var(--color-bg-primary);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
}

.security-text {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.security-title {
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
}

.security-desc {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

// 对话框
:deep(.profile-dialog) {
  .el-dialog__header {
    padding: var(--space-5) var(--space-6);
    border-bottom: 1px solid var(--color-border-light);
  }

  .el-dialog__body {
    padding: var(--space-6);
  }

  .el-dialog__footer {
    padding: var(--space-4) var(--space-6);
    border-top: 1px solid var(--color-border-light);
  }
}

// 响应式
@media (max-width: 640px) {
  .user-stats {
    grid-template-columns: 1fr;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .security-item {
    flex-direction: column;
    gap: var(--space-4);
    align-items: flex-start;
  }
}
</style>
