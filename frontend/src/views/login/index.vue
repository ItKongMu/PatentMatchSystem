<template>
  <div class="login-page">
    <!-- 左侧品牌区域 -->
    <div class="login-brand">
      <div class="brand-content">
        <!-- Logo 和标题 -->
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
        
        <!-- 特性列表 -->
        <div class="brand-features">
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M9 12l2 2 4-4"/>
                <circle cx="12" cy="12" r="10"/>
              </svg>
            </div>
            <div class="feature-text">
              <h3>智能实体提取</h3>
              <p>基于大语言模型的专利技术实体自动识别与分类</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48l2.83-2.83"/>
              </svg>
            </div>
            <div class="feature-text">
              <h3>多维度语义匹配</h3>
              <p>结合实体、领域和向量的三维匹配算法</p>
            </div>
          </div>
          
          <div class="feature-item">
            <div class="feature-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2"/>
                <path d="M3 9h18M9 21V9"/>
              </svg>
            </div>
            <div class="feature-text">
              <h3>IPC领域分析</h3>
              <p>自动解析国际专利分类，构建技术知识图谱</p>
            </div>
          </div>
        </div>
        
        <!-- 底部信息 -->
        <div class="brand-footer">
          <p>毕业设计项目 · 2024</p>
        </div>
      </div>
    </div>
    
    <!-- 右侧登录表单区域 -->
    <div class="login-form-section">
      <div class="login-form-container">
        <div class="form-header">
          <h2 class="form-title">欢迎回来</h2>
          <p class="form-subtitle">请登录您的账户以继续使用系统</p>
        </div>
        
        <el-form
          ref="loginFormRef"
          :model="loginForm"
          :rules="loginRules"
          class="login-form"
          size="large"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="username">
            <label class="form-label">用户名</label>
            <el-input
              v-model="loginForm.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <label class="form-label">密码</label>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          
          <el-form-item class="form-options">
            <el-checkbox v-model="rememberMe">记住登录状态</el-checkbox>
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>
        
        <div class="form-footer">
          <span>还没有账号？</span>
          <router-link to="/register" class="register-link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)
const rememberMe = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度在3到20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度在6到20个字符', trigger: 'blur' }
  ]
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  
  await loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    loading.value = true
    try {
      const result = await userStore.login(loginForm)
      if (result.success) {
        ElMessage.success('登录成功')
        const redirect = route.query.redirect || '/'
        router.push(redirect)
      } else {
        ElMessage.error(result.message || '登录失败')
      }
    } finally {
      loading.value = false
    }
  })
}
</script>

<style lang="scss" scoped>
.login-page {
  display: flex;
  min-height: 100vh;
  background-color: var(--color-bg-primary);
}

// 左侧品牌区域
.login-brand {
  flex: 1;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  position: relative;
  overflow: hidden;

  // 装饰性背景图案
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: 
      radial-gradient(circle at 20% 30%, rgba(255,255,255,0.05) 0%, transparent 50%),
      radial-gradient(circle at 80% 70%, rgba(255,255,255,0.05) 0%, transparent 50%);
    pointer-events: none;
  }

  // 网格线装饰
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
  margin-bottom: var(--space-12);
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

.brand-features {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
}

.feature-icon {
  flex-shrink: 0;
  width: 48px;
  height: 48px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.9);
}

.feature-text {
  h3 {
    font-family: var(--font-heading);
    font-size: var(--text-lg);
    font-weight: var(--font-semibold);
    margin-bottom: var(--space-1);
    color: #fff;
  }

  p {
    font-size: var(--text-sm);
    opacity: 0.7;
    line-height: var(--leading-relaxed);
    margin: 0;
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

// 右侧登录表单区域
.login-form-section {
  width: 520px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-12);
  background-color: var(--color-bg-primary);
}

.login-form-container {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: var(--space-8);
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

.login-form {
  .form-label {
    display: block;
    font-size: var(--text-sm);
    font-weight: var(--font-medium);
    color: var(--color-text-secondary);
    margin-bottom: var(--space-2);
  }

  .el-form-item {
    margin-bottom: var(--space-5);
  }

  .el-input {
    --el-input-height: 48px;

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

.form-options {
  :deep(.el-checkbox__label) {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }
}

.login-btn {
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
}

.form-footer {
  margin-top: var(--space-6);
  text-align: center;
  font-size: var(--text-sm);
  color: var(--color-text-muted);

  .register-link {
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
  .login-page {
    flex-direction: column;
  }

  .login-brand {
    padding: var(--space-8);
    
    .brand-content {
      max-width: 100%;
    }

    .brand-features {
      display: none;
    }

    .brand-footer {
      display: none;
    }
  }

  .login-form-section {
    width: 100%;
    flex: 1;
    padding: var(--space-6);
  }
}

@media (max-width: 480px) {
  .login-brand {
    padding: var(--space-6);
  }

  .brand-title {
    font-size: var(--text-2xl);
  }

  .brand-subtitle {
    font-size: var(--text-base);
  }

  .login-form-section {
    padding: var(--space-4);
  }

  .form-title {
    font-size: var(--text-2xl);
  }
}
</style>
