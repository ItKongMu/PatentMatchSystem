import { createRouter, createWebHistory } from 'vue-router'

// 路由配置
const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', requiresAuth: false }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/login/register.vue'),
    meta: { title: '注册', requiresAuth: false }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/patent/list',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'patent/list',
        name: 'PatentList',
        component: () => import('@/views/patent/PatentList.vue'),
        meta: { title: '专利列表', icon: 'Document' }
      },
      {
        path: 'patent/upload',
        name: 'PatentUpload',
        component: () => import('@/views/patent/PatentUpload.vue'),
        meta: { title: '专利上传', icon: 'Upload' }
      },
      {
        path: 'patent/detail/:id',
        name: 'PatentDetail',
        component: () => import('@/views/patent/PatentDetail.vue'),
        meta: { title: '专利详情', hidden: true }
      },
      {
        path: 'patent/import',
        name: 'PatentImport',
        component: () => import('@/views/patent/PatentImport.vue'),
        meta: { title: 'CSV批量导入', icon: 'Upload' }
      },
      {
        path: 'patent/favorites',
        name: 'PatentFavorites',
        component: () => import('@/views/patent/PatentFavorites.vue'),
        meta: { title: '我的收藏', icon: 'Star' }
      },
      {
        path: 'match',
        name: 'PatentMatch',
        component: () => import('@/views/match/PatentMatch.vue'),
        meta: { title: '技术匹配', icon: 'Connection' }
      },
      {
        path: 'match/history',
        name: 'MatchHistory',
        component: () => import('@/views/match/MatchHistory.vue'),
        meta: { title: '匹配历史', icon: 'Clock' }
      },
      {
        path: 'search',
        name: 'PatentSearch',
        component: () => import('@/views/search/PatentSearch.vue'),
        meta: { title: '专利检索', icon: 'Search' }
      },
      {
        path: 'chat',
        name: 'PatentChat',
        component: () => import('@/views/chat/PatentChat.vue'),
        meta: { title: '智能对话', icon: 'ChatDotRound' }
      },
      {
        path: 'analysis',
        name: 'DataAnalysis',
        component: () => import('@/views/analysis/DataAnalysis.vue'),
        meta: { title: '数据分析', icon: 'DataAnalysis' }
      },
      {
        path: 'graph',
        name: 'KnowledgeGraph',
        component: () => import('@/views/graph/KnowledgeGraph.vue'),
        meta: { title: '知识图谱', icon: 'Share' }
      },
      {
        path: 'user/profile',
        name: 'UserProfile',
        component: () => import('@/views/user/Profile.vue'),
        meta: { title: '个人信息', hidden: true }
      },
      {
        path: 'settings/llm',
        name: 'LlmSettings',
        component: () => import('@/views/settings/LlmSettings.vue'),
        meta: { title: 'LLM配置', icon: 'Setting' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = `${to.meta.title || '专利技术匹配系统'} - 专利技术匹配系统`
  
  // 从localStorage获取token（避免在Pinia初始化前使用store）
  const token = localStorage.getItem('token')
  
  // 检查路由链中是否有任何路由需要认证
  // to.matched 包含了从根路由到当前路由的所有匹配记录
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth === true) || 
                       (to.matched.length > 0 && to.matched.every(record => record.meta.requiresAuth !== false))
  
  // 检查是否明确不需要认证（如登录页、注册页）
  const explicitlyNoAuth = to.meta.requiresAuth === false
  
  if (!explicitlyNoAuth && requiresAuth && !token) {
    // 需要认证但未登录
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else if (token && (to.name === 'Login' || to.name === 'Register')) {
    // 已登录访问登录/注册页面
    next({ path: '/' })
  } else {
    next()
  }
})

export default router
