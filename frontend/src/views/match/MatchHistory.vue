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
      
      <!-- 历史记录表格 -->
      <el-table
        v-loading="loading"
        :data="historyList"
        class="history-table"
        stripe
      >
        <el-table-column prop="id" label="ID" width="70" align="center">
          <template #default="{ row }">
            <span class="id-badge">{{ row.id }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="matchMode" label="模式" width="100" align="center">
          <template #default="{ row }">
            <span class="mode-badge" :class="row.matchMode === 'TEXT' ? 'text' : 'patent'">
              {{ row.matchMode === 'TEXT' ? '文本' : '专利' }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column label="查询来源" min-width="240">
          <template #default="{ row }">
            <template v-if="row.matchMode === 'TEXT'">
              <span class="query-text">{{ truncateText(row.queryText, 80) }}</span>
            </template>
            <template v-else>
              <el-link type="primary" :underline="false" @click="viewPatent(row.sourcePatentId)">
                专利 #{{ row.sourcePatentId }}
              </el-link>
            </template>
          </template>
        </el-table-column>
        
        <el-table-column label="匹配专利" width="180">
          <template #default="{ row }">
            <el-link 
              type="primary" 
              :underline="false"
              @click="viewPatent(row.targetPatentId)"
            >
              {{ row.targetPatent?.title ? truncateText(row.targetPatent.title, 20) : `#${row.targetPatentId}` }}
            </el-link>
          </template>
        </el-table-column>
        
        <el-table-column prop="similarityScore" label="相似度" width="100" align="center">
          <template #default="{ row }">
            <span class="score-badge" :class="getScoreClass(row.similarityScore)">
              {{ (row.similarityScore * 100).toFixed(1) }}%
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="domainMatch" label="领域" width="80" align="center">
          <template #default="{ row }">
            <span class="domain-badge" :class="(row.domainMatch === 1 || row.domainMatch === true) ? 'match' : 'cross'">
              {{ (row.domainMatch === 1 || row.domainMatch === true) ? '匹配' : '跨域' }}
            </span>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="时间" width="160" align="center">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="100" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">
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
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>
    
    <!-- 详情弹窗 -->
    <el-dialog 
      v-model="showDetailDialog" 
      title="匹配详情" 
      width="700"
      class="detail-dialog"
    >
      <template v-if="currentRecord">
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">匹配模式</span>
            <span class="mode-badge" :class="currentRecord.matchMode === 'TEXT' ? 'text' : 'patent'">
              {{ currentRecord.matchMode === 'TEXT' ? '文本查询' : '专利匹配' }}
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">相似度</span>
            <span class="score-badge large" :class="getScoreClass(currentRecord.similarityScore)">
              {{ (currentRecord.similarityScore * 100).toFixed(1) }}%
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">匹配类型</span>
            <span class="detail-value">{{ currentRecord.matchType }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">领域匹配</span>
            <span class="domain-badge" :class="(currentRecord.domainMatch === 1 || currentRecord.domainMatch === true) ? 'match' : 'cross'">
              {{ (currentRecord.domainMatch === 1 || currentRecord.domainMatch === true) ? '是' : '否' }}
            </span>
          </div>
          <div class="detail-item">
            <span class="detail-label">实体匹配数</span>
            <span class="detail-value">{{ currentRecord.entityMatchCount || 0 }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">匹配时间</span>
            <span class="detail-value">{{ formatDate(currentRecord.createdAt) }}</span>
          </div>
        </div>
        
        <div v-if="currentRecord.queryText" class="detail-section">
          <h4 class="section-title">查询文本</h4>
          <p class="section-content">{{ currentRecord.queryText }}</p>
        </div>
        
        <div v-if="currentRecord.queryEntities" class="detail-section">
          <h4 class="section-title">查询实体</h4>
          <div class="entity-tags">
            <span
              v-for="(entity, index) in parseEntities(currentRecord.queryEntities)"
              :key="index"
              class="entity-tag"
            >
              {{ entity.text || entity.name }}
              <small>{{ entity.type }}</small>
            </span>
          </div>
        </div>
        
        <div v-if="currentRecord.matchReason" class="detail-section">
          <h4 class="section-title">匹配原因</h4>
          <p class="section-content">{{ currentRecord.matchReason }}</p>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { matchApi } from '@/api/match'

const router = useRouter()

const loading = ref(false)
const historyList = ref([])
const total = ref(0)
const showDetailDialog = ref(false)
const currentRecord = ref(null)

const filterForm = reactive({
  matchMode: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

const getScoreClass = (score) => {
  if (score >= 0.8) return 'high'
  if (score >= 0.6) return 'medium'
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

const parseEntities = (entitiesJson) => {
  if (!entitiesJson) return []
  try {
    return JSON.parse(entitiesJson)
  } catch {
    return []
  }
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
  router.push(`/patent/detail/${id}`)
}

const viewDetail = (row) => {
  currentRecord.value = row
  showDetailDialog.value = true
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

// 页面头部
.page-header {
  margin-bottom: var(--space-6);
  
  .page-desc {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
    margin: var(--space-2) 0 0 0;
  }
}

// 筛选区域
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

.mode-select {
  width: 150px;
}

// 表格
.history-table {
  margin-bottom: var(--space-5);
}

.id-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 24px;
  padding: 0 var(--space-2);
  background-color: var(--color-bg-tertiary);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
}

.mode-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);

  &.text {
    background-color: #EFF6FF;
    color: var(--color-accent);
  }

  &.patent {
    background-color: #ECFDF5;
    color: var(--color-success);
  }
}

.query-text {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.score-badge {
  display: inline-flex;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);

  &.high { color: var(--color-success); }
  &.medium { color: var(--color-warning); }
  &.low { color: var(--color-danger); }

  &.large {
    font-size: var(--text-lg);
  }
}

.domain-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);

  &.match {
    background-color: #ECFDF5;
    color: var(--color-success);
  }

  &.cross {
    background-color: #F3F4F6;
    color: var(--color-text-muted);
  }
}

.date-text {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

// 分页
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

// 详情弹窗
:deep(.detail-dialog) {
  .el-dialog__header {
    padding: var(--space-5) var(--space-6);
    border-bottom: 1px solid var(--color-border-light);
  }

  .el-dialog__body {
    padding: var(--space-6);
  }
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-4);
  padding: var(--space-4);
  background-color: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-5);
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.detail-label {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.detail-value {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
}

.detail-section {
  margin-top: var(--space-5);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border-light);
}

.section-title {
  font-family: var(--font-heading);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-3);
}

.section-content {
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
  color: var(--color-text-secondary);
  margin: 0;
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
  padding: var(--space-2) var(--space-3);
  background-color: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);

  small {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }
}
</style>
