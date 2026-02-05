<template>
  <div class="error-page">
    <div class="error-content">
      <div class="error-illustration">
        <svg width="200" height="200" viewBox="0 0 200 200" fill="none" xmlns="http://www.w3.org/2000/svg">
          <!-- 搜索框 -->
          <rect x="30" y="60" width="140" height="80" rx="12" fill="#F1F5F9" stroke="#E2E8F0" stroke-width="2"/>
          <!-- 搜索图标 -->
          <circle cx="100" cy="90" r="20" stroke="#CBD5E1" stroke-width="3" fill="none"/>
          <line x1="115" y1="105" x2="130" y2="120" stroke="#CBD5E1" stroke-width="3" stroke-linecap="round"/>
          <!-- 问号 -->
          <text x="100" y="98" text-anchor="middle" fill="#94A3B8" font-family="serif" font-size="24" font-weight="600">?</text>
          <!-- 文档线条 -->
          <line x1="50" y1="120" x2="80" y2="120" stroke="#E2E8F0" stroke-width="2" stroke-linecap="round"/>
          <line x1="50" y1="128" x2="70" y2="128" stroke="#E2E8F0" stroke-width="2" stroke-linecap="round"/>
        </svg>
      </div>
      
      <h1 class="error-code">404</h1>
      <h2 class="error-title">页面未找到</h2>
      <p class="error-message">抱歉，您访问的页面不存在或已被移除</p>
      
      <div class="error-actions">
        <el-button type="primary" size="large" @click="goHome">
          <el-icon>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
              <polyline points="9 22 9 12 15 12 15 22"/>
            </svg>
          </el-icon>
          返回首页
        </el-button>
        <el-button size="large" @click="goBack">
          <el-icon>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
          </el-icon>
          返回上页
        </el-button>
      </div>
      
      <div class="error-footer">
        <p>如果问题持续存在，请联系系统管理员</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<style lang="scss" scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-bg-secondary);
  padding: var(--space-6);
}

.error-content {
  text-align: center;
  max-width: 480px;
}

.error-illustration {
  margin-bottom: var(--space-6);
}

.error-code {
  font-family: var(--font-heading);
  font-size: 96px;
  font-weight: var(--font-bold);
  color: var(--color-accent);
  line-height: 1;
  margin: 0 0 var(--space-4) 0;
  letter-spacing: -0.04em;
}

.error-title {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-3) 0;
}

.error-message {
  font-size: var(--text-base);
  color: var(--color-text-muted);
  margin: 0 0 var(--space-8) 0;
  line-height: var(--leading-relaxed);
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-4);

  .el-button {
    min-width: 140px;
  }
}

.error-footer {
  margin-top: var(--space-12);
  padding-top: var(--space-6);
  border-top: 1px solid var(--color-border-light);

  p {
    font-size: var(--text-sm);
    color: var(--color-text-disabled);
    margin: 0;
  }
}
</style>
