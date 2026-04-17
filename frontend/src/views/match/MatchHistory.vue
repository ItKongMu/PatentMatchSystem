<template>
  <div class="page-container match-history-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">匹配历史记录</h1>
        <p class="page-desc">查看和管理过往的专利匹配分析记录</p>
      </div>
    </div>
    
    <!-- 主内容卡片 -->
    <div class="card main-card">
      <!-- 筛选栏 -->
      <div class="filter-section">
        <div class="filter-group">
          <el-select 
            v-model="filterForm.matchMode" 
            placeholder="匹配模式" 
            clearable
            class="mode-select"
          >
            <el-option label="文本查询" value="TEXT" />
            <el-option label="专利匹配" value="PATENT" />
          </el-select>
          
          <el-button type="primary" @click="handleFilter">
            <el-icon>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
              </svg>
            </el-icon>
            筛选
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
      
      <!-- 历史记录表格（按session聚合，一次匹配=一行） -->
      <el-table
        v-loading="loading"
        :data="historyList"
        class="history-table"
        stripe
        row-key="sessionId"
      >
        <el-table-column prop="matchMode" label="模式" width="100" align="center">
          <template #default="{ row }">
            <span class="mode-badge" :class="row.matchMode === 'TEXT' ? 'text' : 'patent'">
              {{ row.matchMode === 'TEXT' ? '文本' : '专利' }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column label="查询来源" min-width="280">
          <template #default="{ row }">
            <template v-if="row.matchMode === 'TEXT'">
              <span class="query-text">{{ truncateText(row.querySource, 80) }}</span>
            </template>
            <template v-else>
              <el-link type="primary" :underline="false" @click="viewPatent(row.sourcePatentId)">
                {{ truncateText(row.querySource, 50) || `专利 #${row.sourcePatentId}` }}
              </el-link>
            </template>
          </template>
        </el-table-column>
        
        <el-table-column label="匹配专利" min-width="200">
          <template #default="{ row }">
            <div class="match-preview">
              <span class="match-count-badge">{{ row.matchCount }} 条</span>
              <div v-if="row.topMatches?.length" class="top-matches">
                <span
                  v-for="m in row.topMatches"
                  :key="m.targetPatentId"
                  class="top-match-item"
                  @click="viewPatent(m.targetPatentId)"
                >
                  {{ truncateText(m.targetPatentTitle, 15) }}
                  <span class="match-score">{{ (m.similarityScore * 100).toFixed(0) }}%</span>
                </span>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="要求数量" width="90" align="center">
          <template #default="{ row }">
            <span class="topk-badge">Top {{ row.topK }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="时间" width="160" align="center">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewSessionDetail(row)">
              查看详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <!-- 分页 -->
      <div class="pagination-wrapper">
        <div class="pagination-info">
          显示 {{ (pagination.page - 1) * pagination.size + 1 }} - 
          {{ Math.min(pagination.page * pagination.size, total) }} 条，
          共 {{ total }} 条记录
        </div>
        <el-pagination
          :current-page="pagination.page"
          :page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          @size-change="(val) => { pagination.size = val; handleSizeChange() }"
          @current-change="(val) => { pagination.page = val; handlePageChange() }"
        />
      </div>
    </div>
    
    <!-- Session 详情抽屉 -->
    <el-drawer
      v-model="showDetailDrawer"
      :title="drawerTitle"
      size="680px"
      direction="rtl"
      class="detail-drawer"
    >
      <div v-if="currentSession" class="drawer-content">
        <!-- 基本信息 -->
        <div class="session-info">
          <div class="info-row">
            <span class="info-label">匹配模式</span>
            <span class="mode-badge" :class="currentSession.matchMode === 'TEXT' ? 'text' : 'patent'">
              {{ currentSession.matchMode === 'TEXT' ? '文本查询' : '专利匹配' }}
            </span>
          </div>
          <div class="info-row">
            <span class="info-label">查询来源</span>
            <span class="info-value query-source-text">{{ currentSession.querySource }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">要求返回</span>
            <span class="info-value">Top {{ currentSession.topK }}</span>
          </div>
          <div class="info-row">
            <span class="info-label">实际匹配</span>
            <span class="info-value"><strong>{{ currentSession.matchCount }}</strong> 条专利</span>
          </div>
          <div class="info-row">
            <span class="info-label">匹配时间</span>
            <span class="info-value">{{ formatDate(currentSession.createdAt) }}</span>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="detailLoading" class="detail-loading">
          <el-icon class="spin-icon">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 12a9 9 0 1 1-6.219-8.56"/>
            </svg>
          </el-icon>
          加载详细匹配信息...
        </div>

        <template v-else-if="sessionDetail">
          <!-- 查询实体分析 -->
          <div v-if="sessionDetail.queryEntities?.length" class="analysis-card">
            <div class="analysis-header">
              <h3>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="16" x2="12" y2="12"/>
                  <line x1="12" y1="8" x2="12.01" y2="8"/>
                </svg>
                查询实体分析
              </h3>
              <span class="entity-count">识别到 {{ sessionDetail.queryEntities.length }} 个技术实体</span>
            </div>
            <div class="entity-tags">
              <span
                v-for="(entity, idx) in sessionDetail.queryEntities"
                :key="idx"
                class="entity-tag"
                :class="`entity-${entity.type?.toLowerCase()}`"
              >
                <span class="entity-name">{{ entity.name }}</span>
                <small>{{ entity.type }}</small>
              </span>
            </div>
          </div>

          <!-- 匹配结果列表 -->
          <div class="detail-list-header">
            <h4>匹配专利列表（共 {{ sessionDetail.matches?.length || 0 }} 条，按相似度排序）</h4>
          </div>

          <div v-if="sessionDetail.matches?.length" class="detail-list">
            <div
              v-for="(item, index) in sessionDetail.matches"
              :key="item.targetPatentId"
              class="result-card"
            >
              <!-- 排名 -->
              <div class="result-rank">
                <span class="rank-number">{{ index + 1 }}</span>
              </div>

              <!-- 内容 -->
              <div class="result-content">
                <!-- 标题行 -->
                <div class="result-title-row">
                  <h3 class="result-title" @click="viewPatent(item.targetPatentId)">
                    {{ item.targetPatentTitle }}
                  </h3>
                  <div class="score-badge" :class="getScoreClass(item.similarityScore)">
                    <span class="score-value">{{ (item.similarityScore * 100).toFixed(1) }}</span>
                    <span class="score-unit">%</span>
                  </div>
                </div>

                <!-- 元信息 -->
                <div class="result-meta">
                  <span v-if="item.publicationNo" class="meta-item">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                      <polyline points="14 2 14 8 20 8"/>
                    </svg>
                    {{ item.publicationNo }}
                  </span>
                  <span v-if="item.applicant" class="meta-item">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                      <circle cx="12" cy="7" r="4"/>
                    </svg>
                    {{ item.applicant }}
                  </span>
                  <span v-if="item.domainCodes?.length" class="meta-item">
                    <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                      <path d="M3 9h18M9 21V9"/>
                    </svg>
                    {{ item.domainCodes.slice(0, 3).join(', ') }}
                  </span>
                </div>

                <!-- 摘要 -->
                <p v-if="item.patentAbstract" class="result-abstract">
                  {{ truncateText(item.patentAbstract, 180) }}
                </p>

                <!-- 标签行 -->
                <div class="result-tags">
                  <span
                    class="match-tag"
                    :class="item.domainMatch ? 'domain-match' : 'cross-domain'"
                  >
                    {{ item.domainMatch ? '领域匹配' : '跨领域' }}
                  </span>
                  <span v-if="item.entityMatchCount" class="match-tag entity-match">
                    实体匹配: {{ item.entityMatchCount }}
                  </span>
                  <span v-if="item.matchReason" class="match-reason-brief">
                    {{ truncateText(item.matchReason, 60) }}
                  </span>
                </div>

                <!-- 详细匹配原因（可展开） -->
                <div v-if="item.matchReason" class="match-reason-section">
                  <el-collapse v-model="expandedItems[item.targetPatentId]">
                    <el-collapse-item :name="item.targetPatentId">
                      <template #title>
                        <div class="reason-toggle">
                          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"/>
                            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                            <line x1="12" y1="17" x2="12.01" y2="17"/>
                          </svg>
                          <span>查看匹配分析原因</span>
                        </div>
                      </template>
                      <div class="reason-content">
                        <p>{{ item.matchReason }}</p>
                      </div>
                    </el-collapse-item>
                  </el-collapse>
                </div>

                <!-- 操作按钮 -->
                <div class="result-footer">
                  <el-button
                    type="primary"
                    size="small"
                    @click="viewPatent(item.targetPatentId)"
                  >
                    查看专利详情
                  </el-button>
                </div>
              </div>
            </div>
          </div>

          <div v-else class="detail-empty">
            暂无匹配记录
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { matchApi } from '@/api/match'

const router = useRouter()

const loading = ref(false)
const historyList = ref([])
const total = ref(0)
const showDetailDrawer = ref(false)
const currentSession = ref(null)
const sessionDetail = ref(null)
const detailLoading = ref(false)
const expandedItems = ref({})

const filterForm = reactive({
  matchMode: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

const drawerTitle = computed(() => {
  if (!currentSession.value) return '匹配详情'
  const mode = currentSession.value.matchMode === 'TEXT' ? '文本查询' : '专利匹配'
  return `${mode} - 匹配详情`
})

const getScoreClass = (score) => {
  const s = parseFloat(score)
  if (s >= 0.8) return 'high'
  if (s >= 0.6) return 'medium'
  return 'low'
}

const truncateText = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const fetchHistory = async () => {
  loading.value = true
  try {
    const res = await matchApi.getHistory({
      page: pagination.page,
      size: pagination.size,
      matchMode: filterForm.matchMode || undefined
    })
    if (res.code === 200) {
      historyList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取历史记录失败:', error)
  } finally {
    loading.value = false
  }
}

const handleFilter = () => {
  pagination.page = 1
  fetchHistory()
}

const handleReset = () => {
  filterForm.matchMode = ''
  pagination.page = 1
  fetchHistory()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchHistory()
}

const handlePageChange = () => {
  fetchHistory()
}

const viewPatent = (id) => {
  if (!id) return
  router.push(`/patent/detail/${id}`)
}

const viewSessionDetail = async (session) => {
  currentSession.value = session
  showDetailDrawer.value = true
  sessionDetail.value = null
  expandedItems.value = {}
  detailLoading.value = true

  try {
    const res = await matchApi.getSessionDetails(session.sessionId)
    if (res.code === 200) {
      sessionDetail.value = res.data || { queryEntities: [], matches: [] }
    }
  } catch (error) {
    console.error('获取session详情失败:', error)
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  fetchHistory()
})
</script>

<style lang="scss" scoped>
.match-history-page {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: var(--space-6);
  .page-desc {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
    margin: var(--space-2) 0 0 0;
  }
}

.filter-section {
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border-light);
}

.filter-group {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.mode-select { width: 150px; }

.history-table { margin-bottom: var(--space-5); }

.mode-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);

  &.text { background-color: #EFF6FF; color: var(--color-accent); }
  &.patent { background-color: #ECFDF5; color: var(--color-success); }
}

.query-text {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

// 匹配专利预览
.match-preview {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.match-count-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px var(--space-2);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-xs);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  color: var(--color-text-secondary);
  font-family: var(--font-mono);
  width: fit-content;
}

.top-matches {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.top-match-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--text-xs);
  color: var(--color-accent);
  cursor: pointer;
  padding: 2px var(--space-1);
  border-radius: var(--radius-xs);
  transition: background 0.15s;

  &:hover { background: var(--color-bg-tertiary); }
}

.match-score {
  font-family: var(--font-mono);
  font-size: 10px;
  color: var(--color-text-muted);
  margin-left: var(--space-2);
  flex-shrink: 0;
}

.topk-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-1) var(--space-2);
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-xs);
  font-size: var(--text-xs);
  font-family: var(--font-mono);
  color: var(--color-text-muted);
}

.date-text {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.pagination-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-light);
}

.pagination-info {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

// ==================== 抽屉样式 ====================
:deep(.detail-drawer) {
  .el-drawer__header {
    padding: var(--space-5) var(--space-6);
    border-bottom: 1px solid var(--color-border-light);
    margin-bottom: 0;
  }
  .el-drawer__body {
    padding: 0;
    overflow-y: auto;
    display: flex;
    flex-direction: column;
  }
}

.drawer-content {
  display: flex;
  flex-direction: column;
  min-height: 100%;
}

// 基本信息块
.session-info {
  padding: var(--space-5) var(--space-6);
  background: var(--color-bg-secondary);
  border-bottom: 1px solid var(--color-border-light);
  flex-shrink: 0;
}

.info-row {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-2) 0;

  &:not(:last-child) {
    border-bottom: 1px solid var(--color-border-light);
  }
}

.info-label {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  width: 72px;
  flex-shrink: 0;
  padding-top: 2px;
}

.info-value {
  font-size: var(--text-sm);
  color: var(--color-text-primary);
  flex: 1;
  word-break: break-all;

  strong { color: var(--color-accent); }
}

.query-source-text {
  line-height: var(--leading-relaxed);
  max-height: 80px;
  overflow-y: auto;
}

// 加载中
.detail-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  padding: var(--space-10) 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.spin-icon {
  animation: spin 1.5s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

// 查询实体分析
.analysis-card {
  margin: var(--space-5) var(--space-6);
  padding: var(--space-4);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-light);
}

.analysis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-3);

  h3 {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0;
    svg { color: var(--color-accent); }
  }

  .entity-count {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }
}

.entity-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.entity-tag {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  border: 1px solid;

  small { font-size: 10px; opacity: 0.7; }

  &.entity-product { background-color: #EFF6FF; border-color: #BFDBFE; color: #1E40AF; }
  &.entity-method { background-color: #ECFDF5; border-color: #A7F3D0; color: #047857; }
  &.entity-material { background-color: #FEF3C7; border-color: #FDE68A; color: #B45309; }
  &.entity-component { background-color: #F3F4F6; border-color: #D1D5DB; color: #4B5563; }
  &.entity-effect { background-color: #FEE2E2; border-color: #FECACA; color: #B91C1C; }
  &.entity-application { background-color: #F3E8FF; border-color: #DDD6FE; color: #7C3AED; }
}

// 列表头
.detail-list-header {
  padding: var(--space-4) var(--space-6) var(--space-2);
  flex-shrink: 0;

  h4 {
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }
}

// 结果列表
.detail-list {
  padding: 0 var(--space-6) var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

// 单条结果卡片
.result-card {
  display: flex;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border-light);
  border-radius: var(--radius-md);
  transition: box-shadow 0.2s;

  &:hover { box-shadow: var(--shadow-sm); }
}

.result-rank {
  flex-shrink: 0;
}

.rank-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #fff;
  border-radius: 50%;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: var(--font-bold);
}

.result-content {
  flex: 1;
  min-width: 0;
}

.result-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.result-title {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-accent);
  margin: 0;
  cursor: pointer;
  line-height: var(--leading-snug);
  transition: color 0.15s;
  &:hover { color: var(--color-accent-dark); }
}

.score-badge {
  display: flex;
  align-items: baseline;
  flex-shrink: 0;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);

  .score-value { font-size: var(--text-base); font-weight: var(--font-bold); }
  .score-unit { font-size: var(--text-xs); margin-left: 1px; }

  &.high { background-color: #ECFDF5; color: var(--color-success); }
  &.medium { background-color: #FEF3C7; color: var(--color-warning); }
  &.low { background-color: #FEE2E2; color: var(--color-danger); }
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-bottom: var(--space-2);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  svg { flex-shrink: 0; }
}

.result-abstract {
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
  color: var(--color-text-secondary);
  margin: var(--space-2) 0;
}

.result-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: var(--space-2) 0;
}

.match-tag {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  padding: 2px var(--space-2);
  border-radius: var(--radius-sm);

  &.domain-match { background-color: #ECFDF5; color: var(--color-success); }
  &.cross-domain { background-color: #F3F4F6; color: var(--color-text-muted); }
  &.entity-match { background-color: #EFF6FF; color: var(--color-accent); }
}

.match-reason-brief {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  flex: 1;
}

// 匹配原因展开区
.match-reason-section {
  margin-top: var(--space-3);
  border-top: 1px solid var(--color-border-light);
  padding-top: var(--space-2);

  :deep(.el-collapse) { border: none; }
  :deep(.el-collapse-item__header) {
    border: none;
    background: transparent;
    height: auto;
    padding: var(--space-2) 0;
    font-size: var(--text-sm);
    color: var(--color-accent);
  }
  :deep(.el-collapse-item__wrap) { border: none; background: transparent; }
  :deep(.el-collapse-item__content) { padding-bottom: 0; }
}

.reason-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-accent);
  font-weight: var(--font-medium);
  font-size: var(--text-sm);
  svg { flex-shrink: 0; color: var(--color-accent); }
}

.reason-content {
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
  padding: var(--space-3) var(--space-4);
  margin-top: var(--space-2);

  p {
    font-size: var(--text-sm);
    line-height: var(--leading-relaxed);
    color: var(--color-text-secondary);
    margin: 0;
  }
}

.result-footer {
  margin-top: var(--space-3);
  padding-top: var(--space-3);
  border-top: 1px solid var(--color-border-light);
}

.detail-empty {
  text-align: center;
  padding: var(--space-10) 0;
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}
</style>
