<template>
  <div class="register-page">
    <!-- 左侧品牌区域 -->
    <div class="register-brand">
      <div class="brand-content">
        <div class="brand-header">
          <div class="brand-logo">
            <svg width="48" height="48" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
              <rect width="48" height="48" rx="12" fill="#1D4ED8"/>
              <path d="M14 16h20v2H14v-2zm0 7h20v2H14v-2zm0 7h14v2H14v-2z" fill="#fff" opacity="0.9"/>
              <circle cx="34" cy="32" r="6" stroke="#fff" stroke-width="2" fill="none"/>
              <path d="M38 36l4 4" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </div>
          <h1 class="brand-title">PatentMatch</h1>
          <p class="brand-subtitle">专利技术匹配系统</p>
        </div>
        
        <div class="brand-desc">
          <p>加入我们的平台，探索基于大语言模型的专利技术匹配新体验。</p>
          <ul class="feature-list">
            <li>智能解析专利PDF文档</li>
            <li>自动提取技术实体和领域</li>
            <li>多维度语义相似匹配</li>
            <li>可视化匹配结果分析</li>
          </ul>
        </div>
        
        <div class="brand-footer">
          <p>毕业设计项目 · 2024</p>
        </div>
      </div>
    </div>
    
    <!-- 右侧注册表单区域 -->
    <div class="register-form-section">
      <div class="register-form-container">
        <div class="form-header">
          <h2 class="form-title">创建账户</h2>
          <p class="form-subtitle">填写以下信息完成注册</p>
        </div>
        
        <el-form
          ref="registerFormRef"
          :model="registerForm"
          :rules="registerRules"
          class="register-form"
          size="large"
          @submit.prevent="handleRegister"
        >
          <el-form-item prop="username">
            <label class="form-label">用户名 <span class="required">*</span></label>
            <el-input
              v-model="registerForm.username"
              placeholder="3-20位字母、数字或下划线"
              :prefix-icon="User"
            />
          </el-form-item>
          
          <el-form-item prop="nickname">
            <label class="form-label">昵称</label>
            <el-input
              v-model="registerForm.nickname"
              placeholder="显示名称（可选）"
              :prefix-icon="UserFilled"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <label class="form-label">密码 <span class="required">*</span></label>
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="6-20位密码"
              :prefix-icon="Lock"
              show-password
            />
          </el-form-item>
          
          <el-form-item prop="confirmPassword">
            <label class="form-label">确认密码 <span class="required">*</span></label>
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="再次输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleRegister"
            />
          </el-form-item>
          
          <el-form-item class="form-agreement">
            <el-checkbox v-model="agreeTerms">
              我已阅读并同意
              <a href="javascript:;" class="terms-link">服务条款</a>
            </el-checkbox>
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              class="register-btn"
              :loading="loading"
              :disabled="!agreeTerms"
              @click="handleRegister"
            >
              {{ loading ? '注册中...' : '创建账户' }}
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="form-footer">
          <span>已有账号？</span>
          <router-link to="/login" class="login-link">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, UserFilled, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const registerFormRef = ref(null)
const loading = ref(false)
const agreeTerms = ref(false)

const registerForm = reactive({
  username: '',
  nickname: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]+$/, message: '用户名只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  nickname: [
    { max: 20, message: '昵称最多20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  if (!agreeTerms.value) {
    ElMessage.warning('请先阅读并同意服务条款')
    return
  }
  
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const result = await userStore.register({
        username: registerForm.username,
        password: registerForm.password,
        nickname: registerForm.nickname || registerForm.username
      })
      
      if (result.success) {
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } else {
        ElMessage.error(result.message || '注册失败')
      }
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.register-page {
  display: flex;
  min-height: 100vh;
  background-color: var(--color-bg-primary);
}

// 左侧品牌区域
.register-brand {
  flex: 1;
  background: linear-gradient(135deg, var(--color-primary-light) 0%, var(--color-primary) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: 
      radial-gradient(circle at 80% 20%, rgba(255,255,255,0.05) 0%, transparent 50%),
      radial-gradient(circle at 20% 80%, rgba(255,255,255,0.05) 0%, transparent 50%);
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: 
      linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px),
      linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
    background-size: 60px 60px;
    pointer-events: none;
  }
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 480px;
  color: #fff;
}

.brand-header {
  margin-bottom: var(--space-8);
}

.brand-logo {
  margin-bottom: var(--space-6);
}

.brand-title {
  font-family: var(--font-heading);
  font-size: var(--text-4xl);
  font-weight: var(--font-bold);
  margin-bottom: var(--space-2);
  letter-spacing: -0.02em;
}

.brand-subtitle {
  font-size: var(--text-lg);
  opacity: 0.8;
  font-weight: var(--font-normal);
}

.brand-desc {
  p {
    font-size: var(--text-base);
    opacity: 0.9;
    line-height: var(--leading-relaxed);
    margin-bottom: var(--space-6);
  }

  .feature-list {
    list-style: none;
    padding: 0;
    margin: 0;

    li {
      display: flex;
      align-items: center;
      gap: var(--space-3);
      padding: var(--space-2) 0;
      font-size: var(--text-sm);
      opacity: 0.85;

      &::before {
        content: '';
        width: 6px;
        height: 6px;
        background-color: rgba(255, 255, 255, 0.6);
        border-radius: 50%;
        flex-shrink: 0;
      }
    }
  }
}

.brand-footer {
  margin-top: var(--space-12);
  padding-top: var(--space-6);
  border-top: 1px solid rgba(255, 255, 255, 0.1);

  p {
    font-size: var(--text-sm);
    opacity: 0.5;
    margin: 0;
  }
}

// 右侧注册表单区域
.register-form-section {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8);
  background-color: var(--color-bg-primary);
  overflow-y: auto;
}

.register-form-container {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: var(--space-6);
}

.form-title {
  font-family: var(--font-heading);
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);
}

.form-subtitle {
  font-size: var(--text-base);
  color: var(--color-text-muted);
  margin: 0;
}

.register-form {
  .form-label {
    display: block;
    font-size: var(--text-sm);
    font-weight: var(--font-medium);
    color: var(--color-text-secondary);
    margin-bottom: var(--space-2);

    .required {
      color: var(--color-danger);
    }
  }

  .el-form-item {
    margin-bottom: var(--space-4);
  }

  .el-input {
    --el-input-height: 44px;

    :deep(.el-input__wrapper) {
      padding: 0 var(--space-4);
      border-radius: var(--radius-md);
      border: 1px solid var(--color-border);
      box-shadow: none;
      transition: all var(--duration-normal) var(--ease-default);

      &:hover {
        border-color: var(--color-border-dark);
      }

      &.is-focus {
        border-color: var(--color-accent);
        box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.1);
      }
    }

    :deep(.el-input__prefix) {
      color: var(--color-text-muted);
    }
  }
}

.form-agreement {
  :deep(.el-checkbox__label) {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }

  .terms-link {
    color: var(--color-accent);
    text-decoration: none;

    &:hover {
      text-decoration: underline;
    }
  }
}

.register-btn {
  width: 100%;
  height: 48px;
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  border-radius: var(--radius-md);
  background-color: var(--color-accent);
  border-color: var(--color-accent);
  transition: all var(--duration-normal) var(--ease-default);

  &:hover,
  &:focus {
    background-color: var(--color-accent-dark);
    border-color: var(--color-accent-dark);
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(29, 78, 216, 0.3);
  }

  &:active {
    transform: translateY(0);
  }

  &:disabled {
    opacity: 0.6;
    transform: none;
    box-shadow: none;
  }
}

.form-footer {
  margin-top: var(--space-6);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-muted);

  .login-link {
    color: var(--color-accent);
    font-weight: var(--font-medium);
    margin-left: var(--space-1);
    text-decoration: none;
    transition: color var(--duration-fast) var(--ease-default);

    &:hover {
      color: var(--color-accent-dark);
      text-decoration: underline;
    }
  }
}

// 响应式布局
@media (max-width: 1024px) {
  .register-page {
    flex-direction: column;
  }

  .register-brand {
    padding: var(--space-6);
    
    .brand-desc {
      display: none;
    }

    .brand-footer {
      display: none;
    }
  }

  .register-form-section {
    width: 100%;
    flex: 1;
    padding: var(--space-6);
  }
}

@media (max-width: 480px) {
  .register-brand {
    padding: var(--space-4);
  }

  .brand-title {
    font-size: var(--text-2xl);
  }

  .register-form-section {
    padding: var(--space-4);
  }

  .form-title {
    font-size: var(--text-2xl);
  }
}
</style>
