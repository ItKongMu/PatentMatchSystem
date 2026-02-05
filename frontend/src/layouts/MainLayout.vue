<template>
  <el-container class="layout-container">
    <!-- 侧边栏 -->
    <el-aside :width="isCollapse ? '72px' : '240px'" class="layout-aside">
      <!-- Logo 区域 -->
      <div class="logo-container">
        <div class="logo-icon">
          <svg width="32" height="32" viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="10" fill="currentColor"/>
            <path d="M14 16h20v2H14v-2zm0 7h20v2H14v-2zm0 7h14v2H14v-2z" fill="#fff" opacity="0.9"/>
            <circle cx="34" cy="32" r="5" stroke="#fff" stroke-width="1.5" fill="none"/>
            <path d="M37.5 35.5l3 3" stroke="#fff" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
        </div>
        <transition name="fade">
          <div v-show="!isCollapse" class="logo-text">
            <span class="logo-title">PatentMatch</span>
            <span class="logo-subtitle">专利匹配系统</span>
          </div>
        </transition>
      </div>
      
      <!-- 导航菜单 -->
      <el-menu
        :default-active="activeMenu"
        :collapse="isCollapse"
        :collapse-transition="false"
        class="sidebar-menu"
        router
      >
        <div class="menu-section">
          <div v-show="!isCollapse" class="menu-section-title">专利管理</div>
          
          <el-menu-item index="/patent/list">
            <el-icon>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <line x1="16" y1="13" x2="8" y2="13"/>
                <line x1="16" y1="17" x2="8" y2="17"/>
                <polyline points="10 9 9 9 8 9"/>
              </svg>
            </el-icon>
            <template #title>专利列表</template>
          </el-menu-item>
          
          <el-menu-item index="/patent/upload">
            <el-icon>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="17 8 12 3 7 8"/>
                <line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
            </el-icon>
            <template #title>专利上传</template>
          </el-menu-item>
          
          <el-menu-item index="/patent/import">
            <el-icon>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                <polyline points="14 2 14 8 20 8"/>
                <path d="M8 13h8"/>
                <path d="M8 17h8"/>
                <path d="M8 9h2"/>
              </svg>
            </el-icon>
            <template #title>批量导入</template>
          </el-menu-item>
          
          <el-menu-item index="/patent/favorites">
            <el-icon>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
              </svg>
            </el-icon>
            <template #title>我的收藏</template>
          </el-menu-item>
        </div>
        
        <div class="menu-section">
          <div v-show="!isCollapse" class="menu-section-title">技术分析</div>
          
          <el-menu-item index="/match">
            <el-icon>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="18" cy="5" r="3"/>
                <circle cx="6" cy="12" r="3"/>
                <circle cx="18" cy="19" r="3"/>
                <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
                <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
              </svg>
            </el-icon>
            <template #title>技术匹配</template>
          </el-menu-item>
          
          <el-menu-item index="/match/history">
            <el-icon>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/>
                <polyline points="12 6 12 12 16 14"/>
              </svg>
            </el-icon>
            <template #title>匹配历史</template>
          </el-menu-item>
          
          <el-menu-item index="/search">
            <el-icon>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
            </el-icon>
            <template #title>专利检索</template>
          </el-menu-item>
          
          <el-menu-item index="/chat">
            <el-icon>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
              </svg>
            </el-icon>
            <template #title>智能对话</template>
          </el-menu-item>
          
          <el-menu-item index="/analysis">
            <el-icon>
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="20" x2="18" y2="10"/>
                <line x1="12" y1="20" x2="12" y2="4"/>
                <line x1="6" y1="20" x2="6" y2="14"/>
              </svg>
            </el-icon>
            <template #title>数据分析</template>
          </el-menu-item>
        </div>
      </el-menu>
      
      <!-- 折叠按钮 -->
      <div class="collapse-trigger" @click="toggleCollapse">
        <el-icon :class="{ 'is-rotated': isCollapse }">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 18 9 12 15 6"/>
          </svg>
        </el-icon>
      </div>
    </el-aside>
    
    <el-container class="layout-main">
      <!-- 顶部导航 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">
              <el-icon style="vertical-align: middle;">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                  <polyline points="9 22 9 12 15 12 15 22"/>
                </svg>
              </el-icon>
              首页
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute.meta?.title">
              {{ currentRoute.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="header-right">
          <!-- 搜索框 -->
          <div class="header-search">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索专利..."
              :prefix-icon="SearchIcon"
              clearable
              @keyup.enter="handleGlobalSearch"
            />
          </div>
          
          <!-- 用户信息 -->
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="36" class="user-avatar">
                {{ userStore.nickname?.charAt(0) || 'U' }}
              </el-avatar>
              <div class="user-detail">
                <span class="username">{{ userStore.nickname }}</span>
                <span class="user-role">研究员</span>
              </div>
              <el-icon class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                      <circle cx="12" cy="7" r="4"/>
                    </svg>
                  </el-icon>
                  个人信息
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon>
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
                      <polyline points="16 17 21 12 16 7"/>
                      <line x1="21" y1="12" x2="9" y2="12"/>
                    </svg>
                  </el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <!-- 主内容区 -->
      <el-main class="layout-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <keep-alive :include="cachedViews">
              <component :is="Component" />
            </keep-alive>
          </transition>
        </router-view>
      </el-main>
      
      <!-- 底部信息 -->
      <el-footer class="layout-footer">
        <span>PatentMatch - 基于大语言模型的专利技术匹配系统</span>
        <span class="footer-divider">|</span>
        <span>毕业设计项目 2024</span>
      </el-footer>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, markRaw } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { ArrowDown, Search as SearchIcon } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const isCollapse = ref(false)
const searchKeyword = ref('')
const cachedViews = ref(['PatentList', 'PatentSearch', 'PatentMatch', 'PatentChat'])

const activeMenu = computed(() => route.path)
const currentRoute = computed(() => route)

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleGlobalSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({
      path: '/search',
      query: { keyword: searchKeyword.value.trim() }
    })
    searchKeyword.value = ''
  }
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/user/profile')
      break
    case 'logout':
      ElMessageBox.confirm(
        '确定要退出登录吗？',
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      ).then(() => {
        userStore.logout()
      })
      break
  }
}
</script>

<style lang="scss" scoped>
.layout-container {
  height: 100vh;
  width: 100%;
}

// 侧边栏
.layout-aside {
  background-color: var(--color-primary);
  transition: width var(--duration-slow) var(--ease-default);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

// Logo 区域
.logo-container {
  height: 72px;
  display: flex;
  align-items: center;
  padding: 0 var(--space-5);
  gap: var(--space-3);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  flex-shrink: 0;
}

.logo-icon {
  flex-shrink: 0;
  color: var(--color-accent);
}

.logo-text {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  white-space: nowrap;
}

.logo-title {
  font-family: var(--font-heading);
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
  color: #fff;
  letter-spacing: -0.02em;
}

.logo-subtitle {
  font-size: var(--text-xs);
  color: rgba(255, 255, 255, 0.5);
}

// 菜单区域
.sidebar-menu {
  flex: 1;
  border-right: none;
  background-color: transparent;
  padding: var(--space-4) 0;
  overflow-y: auto;

  &:not(.el-menu--collapse) {
    width: 100%;
  }

  // 菜单分组
  .menu-section {
    margin-bottom: var(--space-4);
  }

  .menu-section-title {
    padding: var(--space-2) var(--space-5);
    font-size: var(--text-xs);
    font-weight: var(--font-semibold);
    color: rgba(255, 255, 255, 0.35);
    text-transform: uppercase;
    letter-spacing: 0.1em;
  }

  // 菜单项样式
  :deep(.el-menu-item) {
    height: 48px;
    margin: 2px var(--space-3);
    padding: 0 var(--space-4) !important;
    border-radius: var(--radius-md);
    color: rgba(255, 255, 255, 0.7);
    transition: all var(--duration-normal) var(--ease-default);

    .el-icon {
      color: rgba(255, 255, 255, 0.5);
      font-size: 20px;
      transition: color var(--duration-normal) var(--ease-default);
    }

    &:hover {
      background-color: rgba(255, 255, 255, 0.08);
      color: #fff;

      .el-icon {
        color: rgba(255, 255, 255, 0.9);
      }
    }

    &.is-active {
      background-color: var(--color-accent);
      color: #fff;

      .el-icon {
        color: #fff;
      }
    }
  }

  // 折叠状态下的菜单项
  &.el-menu--collapse {
    :deep(.el-menu-item) {
      padding: 0 !important;
      justify-content: center;
      margin: 2px var(--space-2);

      .el-icon {
        margin-right: 0;
      }
    }
  }
}

// 折叠按钮
.collapse-trigger {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  transition: all var(--duration-normal) var(--ease-default);
  flex-shrink: 0;

  &:hover {
    background-color: rgba(255, 255, 255, 0.05);
    color: rgba(255, 255, 255, 0.9);
  }

  .el-icon {
    transition: transform var(--duration-normal) var(--ease-default);

    &.is-rotated {
      transform: rotate(180deg);
    }
  }
}

// 主内容区
.layout-main {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background-color: var(--color-bg-secondary);
}

// 顶部导航
.layout-header {
  height: var(--header-height);
  background: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-6);
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;

  .el-breadcrumb {
    font-size: var(--text-sm);

    :deep(.el-breadcrumb__inner) {
      display: flex;
      align-items: center;
      gap: var(--space-1);
    }
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-6);
}

.header-search {
  width: 280px;

  :deep(.el-input__wrapper) {
    border-radius: var(--radius-full);
    background-color: var(--color-bg-secondary);
    box-shadow: none;
    border: 1px solid var(--color-border);
    transition: all var(--duration-normal) var(--ease-default);

    &:hover {
      border-color: var(--color-border-dark);
    }

    &.is-focus {
      background-color: var(--color-bg-primary);
      border-color: var(--color-accent);
      box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.1);
    }
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-default);

  &:hover {
    background-color: var(--color-bg-secondary);
  }
}

.user-avatar {
  background: linear-gradient(135deg, var(--color-accent) 0%, var(--color-accent-dark) 100%);
  color: #fff;
  font-family: var(--font-heading);
  font-weight: var(--font-semibold);
}

.user-detail {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.username {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
}

.user-role {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.dropdown-icon {
  color: var(--color-text-muted);
  font-size: 12px;
}

// 主内容
.layout-content {
  background-color: var(--color-bg-secondary);
  padding: var(--space-6);
  overflow: auto;
  flex: 1;
}

// 底部
.layout-footer {
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  background-color: var(--color-bg-primary);
  border-top: 1px solid var(--color-border);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  flex-shrink: 0;
}

.footer-divider {
  color: var(--color-border);
}

// 页面切换动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity var(--duration-normal) var(--ease-default);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
