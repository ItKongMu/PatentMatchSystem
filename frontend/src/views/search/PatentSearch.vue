<template>
  <div class="page-container patent-search-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">专利检索</h1>
        <p class="page-desc">基于Elasticsearch的全文检索，支持关键词、实体、领域等多维度查询</p>
      </div>
    </div>
    
    <!-- 检索方式切换 -->
    <div class="search-tabs">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'quick' }"
        @click="activeTab = 'quick'"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        快速检索
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'advanced' }"
        @click="activeTab = 'advanced'"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/>
        </svg>
        高级检索
      </button>
    </div>
    
    <!-- 检索表单区域 -->
    <div class="card search-card">
      <!-- 快速检索 -->
      <div v-show="activeTab === 'quick'" class="search-section">
        <div class="quick-search">
          <el-input
            v-model="quickKeyword"
            placeholder="输入关键词检索专利标题、摘要、实体..."
            size="large"
            clearable
            class="search-input"
            @keyup.enter="handleQuickSearch"
          >
            <template #prefix>
              <el-icon>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
              </el-icon>
            </template>
            <template #append>
              <el-button type="primary" @click="handleQuickSearch">
                检索
              </el-button>
            </template>
          </el-input>
          <p class="search-tip">支持专利标题、摘要、实体关键词的全文检索</p>
        </div>
      </div>
      
      <!-- 高级检索 -->
      <div v-show="activeTab === 'advanced'" class="search-section">
        <el-form
          ref="advancedFormRef"
          :model="advancedForm"
          label-position="top"
          class="advanced-form"
        >
          <div class="form-grid">
            <el-form-item label="专利标题">
              <el-input v-model="advancedForm.title" placeholder="标题关键词" clearable />
            </el-form-item>
            
            <el-form-item label="申请人">
              <el-input v-model="advancedForm.applicant" placeholder="申请人名称" clearable />
            </el-form-item>
            
            <el-form-item label="IPC分类号">
              <el-input v-model="advancedForm.domainCode" placeholder="如：G06F, H04L" clearable />
            </el-form-item>
            
            <el-form-item label="实体类型">
              <el-select v-model="advancedForm.entityType" placeholder="选择类型" clearable style="width: 100%">
                <el-option label="产品" value="PRODUCT" />
                <el-option label="方法" value="METHOD" />
                <el-option label="材料" value="MATERIAL" />
                <el-option label="组件" value="COMPONENT" />
                <el-option label="效果" value="EFFECT" />
                <el-option label="应用" value="APPLICATION" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="专利号">
              <el-input v-model="advancedForm.publicationNo" placeholder="如：CN123456789A" clearable />
            </el-form-item>
            
            <el-form-item label="实体关键词">
              <el-input v-model="advancedForm.entityKeyword" placeholder="如：深度学习" clearable />
            </el-form-item>
            
            <el-form-item label="公开日期">
              <el-date-picker
                v-model="advancedForm.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始"
                end-placeholder="结束"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>
            
            <el-form-item label="排序方式">
              <el-select v-model="advancedForm.sortField" style="width: 100%">
                <el-option label="相关性评分" value="_score" />
                <el-option label="公开日期" value="publication_date" />
                <el-option label="创建时间" value="created_at" />
              </el-select>
            </el-form-item>
          </div>
          
          <el-form-item label="专利摘要">
            <el-input
              v-model="advancedForm.abstractKeyword"
              type="textarea"
              :rows="3"
              placeholder="摘要关键词"
            />
          </el-form-item>
          
          <div class="form-actions">
            <el-button type="primary" size="large" @click="handleAdvancedSearch">
              <el-icon>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
              </el-icon>
              检索
            </el-button>
            <el-button size="large" @click="resetAdvancedForm">重置</el-button>
          </div>
        </el-form>
      </div>
    </div>
    
    <!-- 检索结果 -->
    <div v-if="showResult" class="results-section">
      <div class="results-header">
        <div class="results-title">
          <h2>检索结果</h2>
          <div class="results-meta">
            <span v-if="maxScore" class="max-score">最高评分: {{ maxScore.toFixed(2) }}</span>
            <span class="results-count">共 {{ total }} 条结果</span>
          </div>
        </div>
      </div>
      
      <div v-loading="loading" class="results-content">
        <!-- 结果列表 -->
        <div class="results-list">
          <div
            v-for="item in searchResults"
            :key="item.id"
            class="result-card card"
          >
            <div class="result-header">
              <div class="result-title-row">
                <h3 class="result-title" @click="viewPatent(item.id)">
                  <span v-html="getHighlightedTitle(item)"></span>
                </h3>
                <div class="result-badges">
                  <span v-if="item.score" class="score-badge">{{ item.score.toFixed(2) }}</span>
                  <span class="status-badge" :class="item.parseStatus === 'SUCCESS' ? 'success' : 'pending'">
                    {{ item.parseStatus === 'SUCCESS' ? '已处理' : '待处理' }}
                  </span>
                </div>
              </div>
              
              <div class="result-meta">
                <span v-if="item.publicationNo" class="meta-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                    <polyline points="14 2 14 8 20 8"/>
                  </svg>
                  {{ item.publicationNo }}
                </span>
                <span v-if="item.applicant" class="meta-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                  {{ item.applicant }}
                </span>
                <span v-if="item.publicationDate" class="meta-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                    <line x1="16" y1="2" x2="16" y2="6"/>
                    <line x1="8" y1="2" x2="8" y2="6"/>
                    <line x1="3" y1="10" x2="21" y2="10"/>
                  </svg>
                  {{ item.publicationDate }}
                </span>
              </div>
            </div>
            
            <p class="result-abstract" v-if="item.patentAbstract">
              <span v-html="getHighlightedAbstract(item)"></span>
            </p>
            
            <!-- 实体标签 -->
            <div class="result-tags" v-if="item.entities?.length">
              <span class="tag-label">实体:</span>
              <span
                v-for="entity in item.entities.slice(0, 5)"
                :key="entity.id"
                class="entity-tag"
                :class="`entity-${entity.entityType?.toLowerCase()}`"
              >
                {{ entity.entityName }}
              </span>
              <span v-if="item.entities.length > 5" class="more-tag">
                +{{ item.entities.length - 5 }}
              </span>
            </div>
            
            <!-- 领域标签 -->
            <div class="result-tags" v-if="item.domains?.length">
              <span class="tag-label">领域:</span>
              <span
                v-for="domain in item.domains.filter(d => d.domainLevel <= 3)"
                :key="domain.id"
                class="domain-tag"
              >
                {{ domain.domainCode }}
              </span>
            </div>
            
            <div class="result-footer">
              <span class="result-time">{{ formatDate(item.createdAt) }}</span>
              <div class="result-actions">
                <el-button size="small" type="primary" link @click="viewPatent(item.id)">
                  查看详情
                </el-button>
                <el-button
                  v-if="item.parseStatus === 'SUCCESS'"
                  size="small"
                  type="success"
                  link
                  @click="handleMatch(item)"
                >
                  相似匹配
                </el-button>
              </div>
            </div>
          </div>
        </div>
        
        <div v-if="!loading && !searchResults.length" class="empty-results">
          <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            <line x1="8" y1="11" x2="14" y2="11"/>
          </svg>
          <h3>未找到匹配的专利</h3>
          <p>尝试修改检索条件或使用其他关键词</p>
        </div>
        
        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="total > 0">
          <div class="pagination-info">
            显示 {{ (pagination.page - 1) * pagination.size + 1 }} - 
            {{ Math.min(pagination.page * pagination.size, total) }} 条
          </div>
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="sizes, prev, pager, next"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { searchApi } from '@/api/search'

const router = useRouter()

const activeTab = ref('quick')
const loading = ref(false)

const quickSearchResults = ref([])
const quickTotal = ref(0)
const advancedSearchResults = ref([])
const advancedTotal = ref(0)

const quickHasSearched = ref(false)
const advancedHasSearched = ref(false)
const hasSearched = computed(() => {
  return activeTab.value === 'quick' ? quickHasSearched.value : advancedHasSearched.value
})

const searchResults = computed(() => {
  return activeTab.value === 'quick' ? quickSearchResults.value : advancedSearchResults.value
})
const total = computed(() => {
  return activeTab.value === 'quick' ? quickTotal.value : advancedTotal.value
})
const showResult = computed(() => {
  return hasSearched.value
})

const maxScore = computed(() => {
  const results = searchResults.value
  if (!results || results.length === 0) return null
  const scores = results.filter(r => r.score).map(r => r.score)
  return scores.length > 0 ? Math.max(...scores) : null
})

const currentKeyword = ref('')
const quickKeyword = ref('')

const advancedFormRef = ref(null)
const advancedForm = reactive({
  title: '',
  abstractKeyword: '',
  domainCode: '',
  applicant: '',
  entityType: '',
  publicationNo: '',
  entityKeyword: '',
  dateRange: null,
  sortField: '_score'
})

const pagination = reactive({
  page: 1,
  size: 10
})

const truncateText = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

const highlightText = (text) => {
  if (!text || !currentKeyword.value) return text
  const keywords = currentKeyword.value.split(/\s+/).filter(k => k)
  let result = text
  keywords.forEach(keyword => {
    const escaped = keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
    const regex = new RegExp(`(${escaped})`, 'gi')
    result = result.replace(regex, '<em class="highlight">$1</em>')
  })
  return result
}

const getHighlightedTitle = (item) => {
  if (item.highlights?.title?.length) {
    return item.highlights.title[0]
  }
  if (item.title && item.title.includes('<em class="highlight">')) {
    return item.title
  }
  return highlightText(item.title)
}

const getHighlightedAbstract = (item) => {
  if (item.highlights?.abstract_text?.length) {
    return item.highlights.abstract_text.join('...')
  }
  if (item.patentAbstract && item.patentAbstract.includes('<em class="highlight">')) {
    return item.patentAbstract
  }
  return highlightText(truncateText(item.patentAbstract, 300))
}

const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const handleQuickSearch = async () => {
  if (!quickKeyword.value.trim()) return
  
  currentKeyword.value = quickKeyword.value
  pagination.page = 1
  await executeSearch()
}

const handleAdvancedSearch = async () => {
  const hasCondition = advancedForm.title || 
                       advancedForm.abstractKeyword || 
                       advancedForm.domainCode ||
                       advancedForm.applicant ||
                       advancedForm.entityType ||
                       advancedForm.publicationNo ||
                       advancedForm.entityKeyword ||
                       advancedForm.dateRange
  if (!hasCondition) return
  
  currentKeyword.value = [
    advancedForm.title,
    advancedForm.abstractKeyword,
    advancedForm.entityKeyword
  ].filter(v => v).join(' ')
  
  pagination.page = 1
  await executeAdvancedSearch()
}

const executeSearch = async () => {
  loading.value = true
  quickHasSearched.value = true
  
  try {
    const res = await searchApi.quickSearch({
      keyword: currentKeyword.value,
      page: pagination.page,
      size: pagination.size
    })
    
    if (res.code === 200) {
      quickSearchResults.value = res.data.list || []
      quickTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('检索失败:', error)
  } finally {
    loading.value = false
  }
}

const executeAdvancedSearch = async () => {
  loading.value = true
  advancedHasSearched.value = true
  
  try {
    const searchParams = {
      title: advancedForm.title,
      abstractKeyword: advancedForm.abstractKeyword,
      domainCode: advancedForm.domainCode,
      applicant: advancedForm.applicant,
      entityType: advancedForm.entityType,
      publicationNo: advancedForm.publicationNo,
      entityKeyword: advancedForm.entityKeyword,
      sortField: advancedForm.sortField,
      sortOrder: 'desc',
      enableHighlight: true,
      page: pagination.page,
      size: pagination.size
    }
    
    if (advancedForm.dateRange && advancedForm.dateRange.length === 2) {
      searchParams.publicationDateFrom = advancedForm.dateRange[0]
      searchParams.publicationDateTo = advancedForm.dateRange[1]
    }
    
    const res = await searchApi.advancedSearchV2(searchParams)
    
    if (res.code === 200) {
      advancedSearchResults.value = res.data.list || []
      advancedTotal.value = res.data.total || 0
    }
  } catch (error) {
    console.error('检索失败:', error)
  } finally {
    loading.value = false
  }
}

const handleSizeChange = () => {
  pagination.page = 1
  if (activeTab.value === 'quick') {
    executeSearch()
  } else {
    executeAdvancedSearch()
  }
}

const handlePageChange = () => {
  if (activeTab.value === 'quick') {
    executeSearch()
  } else {
    executeAdvancedSearch()
  }
}

const viewPatent = (id) => {
  router.push(`/patent/detail/${id}`)
}

const handleMatch = (item) => {
  router.push({
    path: '/match',
    query: { patentId: item.id }
  })
}

const resetAdvancedForm = () => {
  advancedForm.title = ''
  advancedForm.abstractKeyword = ''
  advancedForm.domainCode = ''
  advancedForm.applicant = ''
  advancedForm.entityType = ''
  advancedForm.publicationNo = ''
  advancedForm.entityKeyword = ''
  advancedForm.dateRange = null
  advancedForm.sortField = '_score'
  advancedSearchResults.value = []
  advancedTotal.value = 0
  advancedHasSearched.value = false
  currentKeyword.value = ''
}
</script>

<style lang="scss" scoped>
.patent-search-page {
  max-width: 1100px;
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

// 检索方式切换
.search-tabs {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

.tab-btn {
  flex: 1;
  max-width: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  height: 48px;
  background: var(--color-bg-primary);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-family: var(--font-body);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);

  svg {
    color: var(--color-text-muted);
    transition: color var(--duration-normal) var(--ease-default);
  }

  &:hover {
    border-color: var(--color-border-dark);
  }

  &.active {
    border-color: var(--color-accent);
    background-color: #F8FAFF;
    color: var(--color-accent);

    svg {
      color: var(--color-accent);
    }
  }
}

// 检索表单
.search-card {
  margin-bottom: var(--space-6);
}

.quick-search {
  max-width: 700px;
  margin: var(--space-8) auto;
  text-align: center;
}

.search-input {
  :deep(.el-input__wrapper) {
    border-radius: var(--radius-lg);
    padding-left: var(--space-4);
  }

  :deep(.el-input-group__append) {
    border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  }
}

.search-tip {
  margin-top: var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.advanced-form {
  max-width: 100%;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
}

.form-actions {
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  gap: var(--space-3);

  .el-button {
    min-width: 120px;
  }
}

// 结果区域
.results-section {
  margin-top: var(--space-6);
}

.results-header {
  margin-bottom: var(--space-5);
}

.results-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;

  h2 {
    font-family: var(--font-heading);
    font-size: var(--text-xl);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }
}

.results-meta {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  font-size: var(--text-sm);
}

.max-score {
  color: var(--color-warning);
  font-weight: var(--font-medium);
}

.results-count {
  color: var(--color-text-muted);
}

// 结果列表
.results-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.result-card {
  padding: var(--space-5);
  transition: all var(--duration-normal) var(--ease-default);

  &:hover {
    box-shadow: var(--shadow-md);
  }
}

.result-header {
  margin-bottom: var(--space-3);
}

.result-title-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-2);
}

.result-title {
  font-family: var(--font-heading);
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-accent);
  margin: 0;
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-default);

  &:hover {
    color: var(--color-accent-dark);
  }

  :deep(.highlight) {
    background-color: #FEF3C7;
    padding: 0 2px;
    font-style: normal;
    border-radius: 2px;
  }
}

.result-badges {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-shrink: 0;
}

.score-badge {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-warning);
  background-color: #FEF3C7;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
}

.status-badge {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);

  &.success {
    background-color: #ECFDF5;
    color: var(--color-success);
  }

  &.pending {
    background-color: #F3F4F6;
    color: var(--color-text-muted);
  }
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.result-abstract {
  font-size: var(--text-sm);
  line-height: var(--leading-relaxed);
  color: var(--color-text-secondary);
  margin: var(--space-3) 0;

  :deep(.highlight) {
    background-color: #FEF3C7;
    padding: 0 2px;
    font-style: normal;
    border-radius: 2px;
  }
}

.result-tags {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-2);
  margin: var(--space-2) 0;
}

.tag-label {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.entity-tag {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  background-color: var(--color-bg-tertiary);
  color: var(--color-text-secondary);

  &.entity-product { background-color: #EFF6FF; color: #1E40AF; }
  &.entity-method { background-color: #ECFDF5; color: #047857; }
  &.entity-material { background-color: #FEF3C7; color: #B45309; }
  &.entity-component { background-color: #F3F4F6; color: #4B5563; }
  &.entity-effect { background-color: #FEE2E2; color: #B91C1C; }
  &.entity-application { background-color: #F3E8FF; color: #7C3AED; }
}

.domain-tag {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  background-color: var(--color-bg-tertiary);
  color: var(--color-text-secondary);
}

.more-tag {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.result-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-light);
}

.result-time {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.result-actions {
  display: flex;
  gap: var(--space-2);
}

// 空结果
.empty-results {
  text-align: center;
  padding: var(--space-12) 0;
  color: var(--color-text-muted);

  svg {
    margin-bottom: var(--space-4);
  }

  h3 {
    font-family: var(--font-heading);
    font-size: var(--text-lg);
    color: var(--color-text-secondary);
    margin-bottom: var(--space-2);
  }

  p {
    margin: 0;
  }
}

// 分页
.pagination-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-6);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-light);
}

.pagination-info {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

// 响应式
@media (max-width: 1024px) {
  .form-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .search-tabs {
    flex-direction: column;
  }

  .tab-btn {
    max-width: 100%;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .result-title-row {
    flex-direction: column;
    gap: var(--space-2);
  }

  .result-badges {
    align-self: flex-start;
  }

  .pagination-wrapper {
    flex-direction: column;
    gap: var(--space-4);
  }
}
</style>
