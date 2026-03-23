<template>
  <div class="page-container patent-detail-page">
    <!-- 加载遮罩 -->
    <div v-if="loading" class="loading-mask">
      <div class="loading-spinner"></div>
      <p>加载专利信息...</p>
    </div>
    
    <template v-if="patent && !loading">
      <!-- 页面头部 -->
      <div class="detail-header">
        <div class="header-nav">
          <el-button class="back-btn" @click="$router.back()">
            <el-icon>
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="19" y1="12" x2="5" y2="12"/>
                <polyline points="12 19 5 12 12 5"/>
              </svg>
            </el-icon>
            返回
          </el-button>
          
          <div class="header-actions">
            <FavoriteButton v-if="patent.id" :patent-id="patent.id" />
            <el-button
              v-if="patent.publicationNo"
              @click="handleViewGraph"
            >
              <el-icon>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="18" cy="5" r="3"/>
                  <circle cx="6" cy="12" r="3"/>
                  <circle cx="18" cy="19" r="3"/>
                  <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
                  <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
                </svg>
              </el-icon>
              知识图谱
            </el-button>
            <el-button
              v-if="patent.parseStatus === 'SUCCESS'"
              type="primary"
              @click="handleMatch"
            >
              <el-icon>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="18" cy="5" r="3"/>
                  <circle cx="6" cy="12" r="3"/>
                  <circle cx="18" cy="19" r="3"/>
                  <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
                  <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
                </svg>
              </el-icon>
              查找相似专利
            </el-button>
            <el-button
              v-if="patent.parseStatus === 'PENDING' || patent.parseStatus === 'FAILED'"
              type="success"
              @click="handleProcess"
            >
              <el-icon>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="23 4 23 10 17 10"/>
                  <path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/>
                </svg>
              </el-icon>
              触发处理
            </el-button>
            <el-button
              v-if="isAdmin"
              type="warning"
              @click="handleReprocess"
            >
              <el-icon>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="1 4 1 10 7 10"/>
                  <path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"/>
                </svg>
              </el-icon>
              重新处理
            </el-button>
          </div>
        </div>
        
        <div class="patent-title-section">
          <div class="title-meta">
            <span v-if="patent.publicationNo" class="publication-no">
              {{ patent.publicationNo }}
            </span>
            <span 
              class="status-badge"
              :class="getStatusClass(patent.parseStatus)"
            >
              {{ getStatusText(patent.parseStatus) }}
            </span>
          </div>
          <h1 class="patent-title">{{ patent.title || '(专利处理中...)' }}</h1>
          <div class="patent-meta">
            <span v-if="patent.applicant" class="meta-item">
              <el-icon>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                  <circle cx="12" cy="7" r="4"/>
                </svg>
              </el-icon>
              {{ patent.applicant }}
            </span>
            <span v-if="patent.publicationDate" class="meta-item">
              <el-icon>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                  <line x1="16" y1="2" x2="16" y2="6"/>
                  <line x1="8" y1="2" x2="8" y2="6"/>
                  <line x1="3" y1="10" x2="21" y2="10"/>
                </svg>
              </el-icon>
              {{ patent.publicationDate }}
            </span>
            <span class="meta-item">
              <el-icon>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
              </el-icon>
              {{ patent.sourceType === 'FILE' ? 'PDF上传' : '文本录入' }}
            </span>
          </div>
        </div>
      </div>
      
      <!-- 主要内容区 -->
      <div class="detail-content">
        <!-- 左侧主内容 -->
        <div class="content-main">
          <!-- 摘要 -->
          <section class="content-section">
            <h2 class="section-heading">
              <span class="heading-number">01</span>
              专利摘要
            </h2>
            <div class="abstract-content">
              <p>{{ patent.patentAbstract || '暂无摘要信息' }}</p>
            </div>
          </section>
          
          <!-- 技术实体 -->
          <section v-if="patent.entities?.length" class="content-section">
            <h2 class="section-heading">
              <span class="heading-number">02</span>
              技术实体
              <span class="heading-badge">{{ patent.entities.length }} 项</span>
            </h2>
            <p class="section-desc">由大语言模型自动提取的关键技术概念</p>
            
            <div class="entity-grid">
              <div 
                v-for="entity in patent.entities" 
                :key="entity.id"
                class="entity-card"
                :class="[`entity-${entity.entityType.toLowerCase()}`]"
              >
                <div class="entity-name">{{ entity.entityName }}</div>
                <div class="entity-type">{{ getEntityTypeText(entity.entityType) }}</div>
              </div>
            </div>
          </section>
          
          <!-- 技术领域 -->
          <section v-if="patent.domains?.length" class="content-section">
            <h2 class="section-heading">
              <span class="heading-number">03</span>
              技术领域
              <span class="heading-badge">IPC分类</span>
            </h2>
            <p class="section-desc">基于国际专利分类体系的领域归属</p>
            
            <div class="domain-tree">
              <div
                v-for="domain in sortedDomains"
                :key="domain.id"
                class="domain-item"
                :class="`level-${domain.domainLevel}`"
              >
                <span class="domain-code">{{ domain.domainCode }}</span>
                <span class="domain-desc">{{ domain.domainDesc || '—' }}</span>
                <span class="domain-level">L{{ domain.domainLevel }}</span>
              </div>
            </div>
          </section>
        </div>
        
        <!-- 右侧信息栏 -->
        <aside class="content-aside">
          <!-- 基本信息卡片 -->
          <div class="info-card">
            <h3 class="card-title">基本信息</h3>
            <div class="info-list">
              <div class="info-item">
                <span class="info-label">专利ID</span>
                <span class="info-value code">{{ patent.id }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">公开号</span>
                <span class="info-value code">{{ patent.publicationNo || '—' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">创建时间</span>
                <span class="info-value">{{ formatDate(patent.createdAt) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">更新时间</span>
                <span class="info-value">{{ formatDate(patent.updatedAt) }}</span>
              </div>
            </div>
          </div>
          
          <!-- 向量信息卡片 -->
          <div v-if="patent.vector" class="info-card">
            <h3 class="card-title">向量信息</h3>
            <div class="info-list">
              <div class="info-item">
                <span class="info-label">向量ID</span>
                <el-tooltip :content="patent.vector.vectorId" placement="top">
                  <span class="info-value code truncate">
                    {{ patent.vector.vectorId?.substring(0, 12) }}...
                  </span>
                </el-tooltip>
              </div>
              <div class="info-item">
                <span class="info-label">Embedding模型</span>
                <span class="info-value">{{ patent.vector.embeddingModel }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">向量维度</span>
                <span class="info-value code">{{ patent.vector.vectorDim }}</span>
              </div>
            </div>
          </div>
          
          <!-- 实体类型图例 -->
          <div v-if="patent.entities?.length" class="info-card">
            <h3 class="card-title">实体类型说明</h3>
            <div class="legend-list">
              <div class="legend-item">
                <span class="legend-dot entity-product"></span>
                <span>产品 (PRODUCT)</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot entity-method"></span>
                <span>方法 (METHOD)</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot entity-material"></span>
                <span>材料 (MATERIAL)</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot entity-component"></span>
                <span>组件 (COMPONENT)</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot entity-effect"></span>
                <span>效果 (EFFECT)</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot entity-application"></span>
                <span>应用 (APPLICATION)</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </template>
    
    <!-- 空状态 -->
    <div v-else-if="!loading" class="empty-state">
      <div class="empty-icon">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
          <circle cx="12" cy="12" r="10"/>
          <line x1="12" y1="8" x2="12" y2="12"/>
          <line x1="12" y1="16" x2="12.01" y2="16"/>
        </svg>
      </div>
      <h3>专利信息不存在</h3>
      <p>请返回专利列表重新选择</p>
      <el-button type="primary" @click="$router.push('/patent/list')">返回列表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { patentApi } from '@/api/patent'
import FavoriteButton from '@/components/patent/FavoriteButton.vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

const loading = ref(false)
const patent = ref(null)

// 状态配置
const statusConfig = {
  PENDING: { class: 'pending', text: '待处理' },
  PARSING: { class: 'processing', text: '解析中' },
  EXTRACTING: { class: 'processing', text: '提取中' },
  VECTORIZING: { class: 'processing', text: '向量化' },
  SUCCESS: { class: 'success', text: '已完成' },
  FAILED: { class: 'failed', text: '失败' }
}

const entityTypeMap = {
  PRODUCT: '产品',
  METHOD: '方法',
  MATERIAL: '材料',
  COMPONENT: '组件',
  EFFECT: '效果',
  APPLICATION: '应用'
}

const getStatusClass = (status) => statusConfig[status]?.class || 'pending'
const getStatusText = (status) => statusConfig[status]?.text || status
const getEntityTypeText = (type) => entityTypeMap[type] || type

const sortedDomains = computed(() => {
  if (!patent.value?.domains) return []
  return [...patent.value.domains].sort((a, b) => a.domainLevel - b.domainLevel)
})

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString('zh-CN')
}

const fetchDetail = async () => {
  const id = route.params.id
  if (!id) {
    ElMessage.error('专利ID无效')
    router.back()
    return
  }
  
  loading.value = true
  try {
    const res = await patentApi.getDetail(id)
    if (res.code === 200) {
      patent.value = res.data
    } else {
      ElMessage.error(res.message || '获取专利详情失败')
    }
  } catch (error) {
    console.error('获取详情失败:', error)
  } finally {
    loading.value = false
  }
}

const handleProcess = async () => {
  try {
    const res = await patentApi.process(patent.value.id)
    if (res.code === 200) {
      ElMessage.success('已开始处理')
      fetchDetail()
    }
    // 非200的业务错误已由 request.js 统一弹出错误消息
  } catch (error) {
    // HTTP 错误（403/400/500等）已由 request.js 拦截器统一弹出消息
    console.error('处理失败:', error)
  }
}

const handleReprocess = async () => {
  try {
    await ElMessageBox.confirm(
      `确定要重新处理该专利吗？这将清除已有的实体、领域和向量数据，重新从头处理。`,
      '重新处理确认',
      { type: 'warning' }
    )
    const res = await patentApi.reprocess(patent.value.id)
    if (res.code === 200) {
      ElMessage.success('已开始重新处理')
      fetchDetail()
    }
    // 非200的业务错误已由 request.js 统一弹出错误消息
  } catch (error) {
    if (error !== 'cancel') {
      // HTTP 错误已由 request.js 拦截器统一弹出消息
      console.error('重新处理失败:', error)
    }
  }
}

const handleMatch = () => {
  router.push({
    path: '/match',
    query: { patentId: patent.value.id }
  })
}

const handleViewGraph = () => {
  router.push({
    path: '/graph',
    query: { mode: 'patent', q: patent.value.publicationNo }
  })
}

onMounted(() => {
  fetchDetail()
})
</script>

<style lang="scss" scoped>
.patent-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  min-height: calc(100vh - var(--header-height) - var(--space-12));
}

// 加载状态
.loading-mask {
  position: absolute;
  inset: 0;
  background-color: var(--color-bg-secondary);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-4);
  z-index: 10;

  p {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

// 详情头部
.detail-header {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
  margin-bottom: var(--space-6);
}

.header-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border-light);
}

.back-btn {
  color: var(--color-text-muted);

  &:hover {
    color: var(--color-accent);
  }
}

.header-actions {
  display: flex;
  gap: var(--space-3);
}

.patent-title-section {
  .title-meta {
    display: flex;
    align-items: center;
    gap: var(--space-3);
    margin-bottom: var(--space-3);
  }

  .publication-no {
    font-family: var(--font-mono);
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--color-accent);
    background-color: #EFF6FF;
    padding: var(--space-1) var(--space-3);
    border-radius: var(--radius-sm);
  }

  .status-badge {
    font-size: var(--text-xs);
    font-weight: var(--font-medium);
    padding: var(--space-1) var(--space-2);
    border-radius: var(--radius-sm);

    &.pending {
      background-color: #F3F4F6;
      color: var(--color-text-muted);
    }

    &.processing {
      background-color: #FEF3C7;
      color: var(--color-warning);
    }

    &.success {
      background-color: #ECFDF5;
      color: var(--color-success);
    }

    &.failed {
      background-color: #FEE2E2;
      color: var(--color-danger);
    }
  }
}

.patent-title {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  line-height: var(--leading-snug);
  margin-bottom: var(--space-4);
}

.patent-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-5);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

// 主要内容区
.detail-content {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: var(--space-6);
}

.content-main {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

// 内容区块
.content-section {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-6);
}

.section-heading {
  font-family: var(--font-heading);
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.heading-number {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: var(--font-bold);
  color: var(--color-accent);
  background-color: #EFF6FF;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
}

.heading-badge {
  font-family: var(--font-body);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-muted);
  background-color: var(--color-bg-tertiary);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-full);
  margin-left: auto;
}

.section-desc {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin-bottom: var(--space-4);
}

// 摘要内容
.abstract-content {
  p {
    font-size: var(--text-base);
    line-height: var(--leading-loose);
    color: var(--color-text-secondary);
    text-align: justify;
    margin: 0;
  }
}

// 实体网格
.entity-grid {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
}

.entity-card {
  display: inline-flex;
  flex-direction: column;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  border: 1px solid;
  transition: all var(--duration-fast) var(--ease-default);

  &:hover {
    transform: translateY(-2px);
    box-shadow: var(--shadow-md);
  }

  &.entity-product {
    background-color: #EFF6FF;
    border-color: #BFDBFE;
  }

  &.entity-method {
    background-color: #ECFDF5;
    border-color: #A7F3D0;
  }

  &.entity-material {
    background-color: #FEF3C7;
    border-color: #FDE68A;
  }

  &.entity-component {
    background-color: #F3F4F6;
    border-color: #D1D5DB;
  }

  &.entity-effect {
    background-color: #FEE2E2;
    border-color: #FECACA;
  }

  &.entity-application {
    background-color: #F3E8FF;
    border-color: #DDD6FE;
  }
}

.entity-name {
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  margin-bottom: var(--space-1);
}

.entity-type {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

// 领域树
.domain-tree {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.domain-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-3) var(--space-4);
  background-color: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--color-accent);
  transition: all var(--duration-fast) var(--ease-default);

  &.level-1 {
    border-left-color: var(--color-accent);
  }

  &.level-2 {
    margin-left: var(--space-6);
    border-left-color: var(--color-accent-light);
  }

  &.level-3 {
    margin-left: var(--space-12);
    border-left-color: #93C5FD;
  }

  &.level-4 {
    margin-left: calc(var(--space-12) + var(--space-6));
    border-left-color: #BFDBFE;
  }
}

.domain-code {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  min-width: 80px;
}

.domain-desc {
  flex: 1;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.domain-level {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  background-color: var(--color-bg-primary);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
}

// 侧边栏
.content-aside {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.info-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
}

.card-title {
  font-family: var(--font-heading);
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border-light);
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.info-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.info-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.info-value {
  font-size: var(--text-sm);
  color: var(--color-text-primary);
  font-weight: var(--font-medium);

  &.code {
    font-family: var(--font-mono);
  }

  &.truncate {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

// 图例
.legend-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: var(--radius-sm);
  flex-shrink: 0;

  &.entity-product { background-color: #3B82F6; }
  &.entity-method { background-color: #10B981; }
  &.entity-material { background-color: #F59E0B; }
  &.entity-component { background-color: #6B7280; }
  &.entity-effect { background-color: #EF4444; }
  &.entity-application { background-color: #8B5CF6; }
}

// 空状态
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: var(--space-16) 0;
  text-align: center;

  .empty-icon {
    color: var(--color-text-disabled);
    margin-bottom: var(--space-4);
  }

  h3 {
    font-family: var(--font-heading);
    font-size: var(--text-xl);
    color: var(--color-text-primary);
    margin-bottom: var(--space-2);
  }

  p {
    color: var(--color-text-muted);
    margin-bottom: var(--space-6);
  }
}

// 响应式
@media (max-width: 1024px) {
  .detail-content {
    grid-template-columns: 1fr;
  }

  .content-aside {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--space-4);
  }
}

@media (max-width: 768px) {
  .header-nav {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-4);
  }

  .header-actions {
    width: 100%;
    
    .el-button {
      flex: 1;
    }
  }

  .content-aside {
    grid-template-columns: 1fr;
  }

  .domain-item {
    &.level-2 { margin-left: var(--space-4); }
    &.level-3 { margin-left: var(--space-8); }
    &.level-4 { margin-left: var(--space-12); }
  }
}
</style>
