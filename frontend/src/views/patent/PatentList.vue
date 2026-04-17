<template>
  <div class="page-container patent-list-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">专利文献库</h1>
        <p class="page-desc">管理和浏览已录入系统的专利文献，支持全文检索和状态筛选</p>
      </div>
      <div class="header-actions">
        <el-button @click="$router.push('/patent/import')">
          <el-icon>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
          </el-icon>
          CSV导入
        </el-button>
        <el-button type="primary" class="upload-btn" @click="$router.push('/patent/upload')">
          <el-icon>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </el-icon>
          上传专利
        </el-button>
      </div>
    </div>
    
    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon total">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ total }}</span>
          <span class="stat-label">专利总数</span>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon success">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ successCount }}</span>
          <span class="stat-label">本页已处理</span>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon processing">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="2" x2="12" y2="6"/>
            <line x1="12" y1="18" x2="12" y2="22"/>
            <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"/>
            <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"/>
            <line x1="2" y1="12" x2="6" y2="12"/>
            <line x1="18" y1="12" x2="22" y2="12"/>
            <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"/>
            <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ processingCount }}</span>
          <span class="stat-label">本页处理中</span>
        </div>
      </div>
      
      <div class="stat-card">
        <div class="stat-icon pending">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pendingCount }}</span>
          <span class="stat-label">本页待处理</span>
        </div>
      </div>
    </div>
    
    <!-- 主内容卡片 -->
    <div class="card main-card">
      <!-- 批量操作工具栏 -->
      <transition name="batch-bar">
        <div v-if="selectedRows.length > 0" class="batch-toolbar">
          <div class="batch-info">
            <el-icon class="batch-icon">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 11 12 14 22 4"/>
                <path d="M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11"/>
              </svg>
            </el-icon>
            <span>已选择 <strong>{{ selectedRows.length }}</strong> 条专利</span>
          </div>
          <div class="batch-actions">
            <el-button
              type="success"
              size="small"
              :loading="batchProcessLoading"
              :disabled="batchProcessableCount === 0"
              @click="handleBatchProcess"
            >
              <el-icon>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polygon points="5 3 19 12 5 21 5 3"/>
                </svg>
              </el-icon>
              批量处理
              <span v-if="batchProcessableCount > 0" class="count-badge">{{ batchProcessableCount }}</span>
            </el-button>
            <el-button
              type="danger"
              size="small"
              :loading="batchDeleteLoading"
              @click="handleBatchDelete"
            >
              <el-icon>
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="3 6 5 6 21 6"/>
                  <path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6"/>
                  <path d="M10 11v6M14 11v6"/>
                  <path d="M9 6V4a1 1 0 0 1 1-1h4a1 1 0 0 1 1 1v2"/>
                </svg>
              </el-icon>
              批量删除
            </el-button>
            <el-button size="small" @click="clearSelection">取消选择</el-button>
          </div>
        </div>
      </transition>

      <!-- 搜索和筛选 -->
      <div class="filter-section">
        <div class="search-box">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索专利标题、公开号..."
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon>
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
              </el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="filter-group">
          <el-select
            v-model="searchForm.parseStatus"
            placeholder="解析状态"
            clearable
            class="status-select"
          >
            <el-option label="待处理" value="PENDING" />
            <el-option label="解析中" value="PARSING" />
            <el-option label="提取中" value="EXTRACTING" />
            <el-option label="向量化中" value="VECTORIZING" />
            <el-option label="已完成" value="SUCCESS" />
            <el-option label="失败" value="FAILED" />
          </el-select>
          
          <el-button type="primary" @click="handleSearch">
            <el-icon>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
            </el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>
      
      <!-- 专利表格 -->
      <el-table
        ref="tableRef"
        v-loading="loading"
        :data="patentList"
        class="patent-table"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" align="center" />
        <el-table-column prop="id" label="ID" width="96" align="center">
          <template #default="{ row }">
            <span class="id-badge">{{ row.id }}</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="publicationNo" label="公开号" width="195" align="center">
          <template #default="{ row }">
            <span v-if="row.publicationNo" class="patent-number">{{ row.publicationNo }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        
        <el-table-column prop="title" label="专利标题" min-width="200" align="center">
          <template #default="{ row }">
            <div class="title-cell">
              <el-link 
                type="primary" 
                :underline="false"
                class="patent-link"
                @click="viewDetail(row.id)"
              >
                {{ row.title || '(处理中...)' }}
              </el-link>
              <div class="title-meta">
                <span v-if="row.applicant" class="applicant">
                  <el-icon>
                    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                      <circle cx="12" cy="7" r="4"/>
                    </svg>
                  </el-icon>
                  {{ row.applicant }}
                </span>
              </div>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="sourceType" label="来源" width="100" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="row.sourceType === 'FILE' ? '' : 'success'" 
              size="small"
              class="source-tag"
            >
              {{ row.sourceType === 'FILE' ? 'PDF' : '文本' }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="parseStatus" label="状态" width="120" align="center">
          <template #default="{ row }">
            <div class="status-cell">
              <span 
                class="status-dot"
                :class="getStatusClass(row.parseStatus)"
              ></span>
              <span class="status-text">{{ getStatusText(row.parseStatus) }}</span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="createdAt" label="创建时间" width="185" align="center">
          <template #default="{ row }">
            <span class="date-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button size="small" type="primary" link @click="viewDetail(row.id)">
                详情
              </el-button>
              <el-button
                v-if="row.parseStatus === 'PENDING' || row.parseStatus === 'FAILED'"
                size="small"
                type="success"
                link
                @click="handleProcess(row)"
              >
                处理
              </el-button>
              <el-button
                v-if="isAdmin && (row.parseStatus === 'SUCCESS' || row.parseStatus === 'FAILED' || row.parseStatus === 'PENDING')"
                size="small"
                type="warning"
                link
                @click="handleReprocess(row)"
              >
                重处理
              </el-button>
              <el-button
                v-if="row.parseStatus === 'SUCCESS'"
                size="small"
                type="primary"
                link
                @click="handleMatch(row)"
              >
                匹配
              </el-button>
              <el-button size="small" type="danger" link @click="handleDelete(row)">
                删除
              </el-button>
            </div>
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, onActivated, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { patentApi } from '@/api/patent'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

const loading = ref(false)
const patentList = ref([])
const total = ref(0)
const pollingTimer = ref(null)
const POLLING_INTERVAL = 2000
const tableRef = ref(null)
const selectedRows = ref([])
const batchProcessLoading = ref(false)
const batchDeleteLoading = ref(false)

const searchForm = reactive({
  keyword: '',
  parseStatus: ''
})

const pagination = reactive({
  page: 1,
  size: 10
})

const forcePolling = ref(false)
const forcePollingCount = ref(0)
// 强制轮询最多等待 15 次（30秒），等待处理任务出现
const MAX_FORCE_POLLING_COUNT = 15
// 如果 N 次内从未出现处理中状态，说明任务未启动，提前终止（避免无效轮询）
const MAX_IDLE_POLLING_COUNT = 5
const hadProcessingPatents = ref(false)

// 统计数据
const successCount = computed(() => 
  patentList.value.filter(p => p.parseStatus === 'SUCCESS').length
)
const processingCount = computed(() => 
  patentList.value.filter(p => ['PARSING', 'EXTRACTING', 'VECTORIZING'].includes(p.parseStatus)).length
)
const pendingCount = computed(() => 
  patentList.value.filter(p => p.parseStatus === 'PENDING').length
)

// 批量操作相关
const batchProcessableCount = computed(() =>
  selectedRows.value.filter(r => r.parseStatus === 'PENDING' || r.parseStatus === 'FAILED').length
)

const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

const clearSelection = () => {
  tableRef.value?.clearSelection()
  selectedRows.value = []
}

const handleBatchProcess = async () => {
  if (batchProcessableCount.value === 0) {
    ElMessage.warning('所选专利中没有可处理的条目（仅待处理/失败状态可触发）')
    return
  }
  const processableIds = selectedRows.value
    .filter(r => r.parseStatus === 'PENDING' || r.parseStatus === 'FAILED')
    .map(r => r.id)
  try {
    await ElMessageBox.confirm(
      `将对 ${processableIds.length} 条专利触发处理流程（AI提取实体、向量化），是否继续？`,
      '批量处理确认',
      { type: 'info', confirmButtonText: '开始处理', cancelButtonText: '取消' }
    )
    batchProcessLoading.value = true
    const res = await patentApi.batchProcess(processableIds)
    if (res.code === 200) {
      ElMessage.success(`已成功触发 ${res.data} 条专利的处理任务，可在列表实时查看进度`)
      clearSelection()
      await fetchList()
      startPolling()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量处理失败:', error)
    }
  } finally {
    batchProcessLoading.value = false
  }
}

const handleBatchDelete = async () => {
  const ids = selectedRows.value.map(r => r.id)
  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${ids.length} 条专利吗？此操作不可恢复，将同时清除相关实体、向量和索引数据。`,
      '批量删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
    batchDeleteLoading.value = true
    const res = await patentApi.batchDelete(ids)
    if (res.code === 200) {
      ElMessage.success(`已成功删除 ${ids.length} 条专利`)
      clearSelection()
      fetchList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('批量删除失败:', error)
    }
  } finally {
    batchDeleteLoading.value = false
  }
}

const hasProcessingPatents = computed(() => {
  const processingStatuses = ['PARSING', 'EXTRACTING', 'VECTORIZING']
  return patentList.value.some(p => processingStatuses.includes(p.parseStatus))
})

const shouldContinuePolling = computed(() => {
  if (hasProcessingPatents.value) {
    return true
  }
  
  if (forcePolling.value) {
    // 曾经出现过处理中状态，现在全部处理完了，停止
    if (hadProcessingPatents.value) {
      return false
    }
    // 从未出现过处理中状态：若已达空闲轮询上限，提前停止（避免无效轮询）
    if (forcePollingCount.value >= MAX_IDLE_POLLING_COUNT) {
      return false
    }
    // 总轮询次数保护
    return forcePollingCount.value < MAX_FORCE_POLLING_COUNT
  }
  
  return false
})

// 状态配置
const statusConfig = {
  PENDING: { class: 'pending', text: '待处理' },
  PARSING: { class: 'processing', text: '解析中' },
  EXTRACTING: { class: 'processing', text: '提取中' },
  VECTORIZING: { class: 'processing', text: '向量化' },
  SUCCESS: { class: 'success', text: '已完成' },
  FAILED: { class: 'failed', text: '失败' }
}

const getStatusClass = (status) => statusConfig[status]?.class || 'pending'
const getStatusText = (status) => statusConfig[status]?.text || status

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

const fetchList = async (showLoading = true) => {
  if (showLoading) {
    loading.value = true
  }
  try {
    const res = await patentApi.getList({
      pageNum: pagination.page,
      pageSize: pagination.size,
      keyword: searchForm.keyword || undefined,
      parseStatus: searchForm.parseStatus || undefined
    })
    if (res.code === 200) {
      patentList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取列表失败:', error)
  } finally {
    loading.value = false
  }
}

const silentRefresh = async () => {
  await fetchList(false)
}

const startPolling = (force = false) => {
  stopPolling()
  
  if (force) {
    forcePolling.value = true
    forcePollingCount.value = 0
    hadProcessingPatents.value = false
  }
  
  pollingTimer.value = setInterval(async () => {
    if (forcePolling.value) {
      forcePollingCount.value++
    }
    
    await silentRefresh()
    
    if (hasProcessingPatents.value) {
      hadProcessingPatents.value = true
    }
    
    if (!shouldContinuePolling.value) {
      stopPolling()
    }
  }, POLLING_INTERVAL)
}

const stopPolling = () => {
  if (pollingTimer.value) {
    clearInterval(pollingTimer.value)
    pollingTimer.value = null
  }
  forcePolling.value = false
  forcePollingCount.value = 0
  hadProcessingPatents.value = false
}

watch(hasProcessingPatents, (hasProcessing) => {
  if (hasProcessing && !pollingTimer.value) {
    startPolling()
  }
})

const handleSearch = () => {
  pagination.page = 1
  fetchList()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.parseStatus = ''
  pagination.page = 1
  fetchList()
}

const handleSizeChange = () => {
  pagination.page = 1
  fetchList()
}

const handlePageChange = () => {
  fetchList()
}

const viewDetail = (id) => {
  router.push(`/patent/detail/${id}`)
}

const handleProcess = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要处理专利"${row.title || '未命名'}"吗？`,
      '提示',
      { type: 'info' }
    )
    
    const res = await patentApi.process(row.id)
    if (res.code === 200) {
      ElMessage.success('已开始处理')
      await fetchList()
      startPolling()
    }
    // 非200的业务错误已由 request.js 统一弹出错误消息
  } catch (error) {
    if (error !== 'cancel') {
      // HTTP 错误（403/400/500等）已由 request.js 拦截器统一弹出消息
      // 此处仅记录日志，避免重复弹窗
      console.error('处理失败:', error)
    }
  }
}

const handleReprocess = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要重新处理专利"${row.title || '未命名'}"吗？这将清除已有的实体、领域和向量数据，重新从头处理。`,
      '重新处理确认',
      { type: 'warning' }
    )
    
    const res = await patentApi.reprocess(row.id)
    if (res.code === 200) {
      ElMessage.success('已开始重新处理')
      await fetchList()
      startPolling()
    }
    // 非200的业务错误已由 request.js 统一弹出错误消息
  } catch (error) {
    if (error !== 'cancel') {
      // HTTP 错误已由 request.js 拦截器统一弹出消息
      console.error('重新处理失败:', error)
    }
  }
}

const handleMatch = (row) => {
  router.push({
    path: '/match',
    query: { patentId: row.id }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除专利"${row.title || row.publicationNo || '未命名'}"吗？此操作不可恢复。`,
      '警告',
      { type: 'warning' }
    )
    
    const res = await patentApi.delete(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      fetchList()
    }
    // 非200的业务错误已由 request.js 统一弹出错误消息
  } catch (error) {
    if (error !== 'cancel') {
      // HTTP 错误（403等）已由 request.js 拦截器统一弹出消息
      console.error('删除失败:', error)
    }
  }
}

const initLoad = async () => {
  await fetchList()
  
  if (route.query.polling === 'true') {
    startPolling(true)
  } else if (hasProcessingPatents.value) {
    startPolling()
  }
}

onMounted(() => {
  initLoad()
})

watch(
  () => route.query.polling,
  (newPolling, oldPolling) => {
    if (newPolling === 'true' && oldPolling !== 'true') {
      initLoad()
    }
  }
)

onActivated(() => {
  fetchList().then(() => {
    if (hasProcessingPatents.value && !pollingTimer.value) {
      startPolling()
    }
  })
})

onUnmounted(() => {
  stopPolling()
})
</script>

<style lang="scss" scoped>
.patent-list-page {
  max-width: 1400px;
  margin: 0 auto;
}

// 页面头部
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: var(--space-6);
}

.header-content {
  .page-title {
    margin-bottom: var(--space-2);
  }
  
  .page-desc {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
    margin: 0;
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.upload-btn {
  height: 44px;
  padding: 0 var(--space-5);
  font-weight: var(--font-medium);
}

// 批量操作工具栏
.batch-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  margin-bottom: var(--space-4);
  background: linear-gradient(135deg, #eff6ff 0%, #e0f2fe 100%);
  border: 1px solid #bfdbfe;
  border-radius: var(--radius-lg);

  .batch-info {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    color: var(--color-accent);
    font-size: var(--text-sm);

    .batch-icon {
      font-size: 16px;
    }

    strong {
      font-weight: var(--font-bold);
      font-size: var(--text-base);
    }
  }

  .batch-actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);
  }
}

.count-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  margin-left: 4px;
  background-color: rgba(255, 255, 255, 0.8);
  border-radius: 9px;
  font-size: 11px;
  font-weight: var(--font-bold);
  line-height: 1;
}

// 批量工具栏过渡动画
.batch-bar-enter-active,
.batch-bar-leave-active {
  transition: all 0.25s ease;
}

.batch-bar-enter-from,
.batch-bar-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

// 统计卡片
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.stat-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  transition: all var(--duration-normal) var(--ease-default);

  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.total {
    background-color: #EFF6FF;
    color: var(--color-accent);
  }

  &.success {
    background-color: #ECFDF5;
    color: var(--color-success);
  }

  &.processing {
    background-color: #FEF3C7;
    color: var(--color-warning);
  }

  &.pending {
    background-color: #F3F4F6;
    color: var(--color-text-muted);
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-family: var(--font-heading);
  font-size: var(--text-2xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  line-height: 1;
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin-top: var(--space-1);
}

// 主卡片
.main-card {
  overflow: hidden;
}

// 筛选区域
.filter-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-5);
  padding-bottom: var(--space-5);
  border-bottom: 1px solid var(--color-border-light);
}

.search-box {
  width: 320px;

  :deep(.el-input__wrapper) {
    border-radius: var(--radius-md);
  }
}

.filter-group {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.status-select {
  width: 140px;
}

// 表格样式
.patent-table {
  margin-bottom: var(--space-5);
  
  :deep(.el-table__header) {
    th {
      font-weight: var(--font-semibold);
    }
  }
}

.id-badge {
  display: inline-block;
  padding: 2px 8px;
  background-color: var(--color-bg-tertiary);
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  color: var(--color-text-secondary);
  line-height: 20px;
}

.patent-number {
  display: inline-block;
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  background-color: var(--color-bg-tertiary);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  white-space: nowrap;
}

.title-cell {
  display: flex;
  flex-direction: column;
  align-items: center;

  .patent-link {
    font-weight: var(--font-medium);
    transition: color var(--duration-fast) var(--ease-default);
    text-align: center;
    
    &:hover {
      color: var(--color-accent-dark);
    }
  }
}

.title-meta {
  margin-top: var(--space-1);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
}

.applicant {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.source-tag {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
}

.status-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;

  &.pending {
    background-color: var(--color-text-disabled);
  }

  &.processing {
    background-color: var(--color-warning);
    animation: pulse 1.5s ease-in-out infinite;
  }

  &.success {
    background-color: var(--color-success);
  }

  &.failed {
    background-color: var(--color-danger);
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.status-text {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.date-text {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  white-space: nowrap;
}

.text-muted {
  color: var(--color-text-disabled);
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
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

// 响应式
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: var(--space-4);
  }

  .stats-row {
    grid-template-columns: 1fr;
  }

  .filter-section {
    flex-direction: column;
    align-items: stretch;
    gap: var(--space-4);
  }

  .search-box {
    width: 100%;
  }

  .filter-group {
    flex-wrap: wrap;
  }

  .pagination-wrapper {
    flex-direction: column;
    gap: var(--space-4);
  }
}
</style>
