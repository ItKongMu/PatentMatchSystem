<template>
  <div class="page-container favorites-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">我的收藏</h1>
        <p class="page-desc">管理您收藏的专利文献</p>
      </div>
      <div class="header-stats">
        <div class="stat-badge">
          <el-icon><Star /></el-icon>
          <span>共收藏 {{ total }} 篇专利</span>
        </div>
      </div>
    </div>

    <!-- 主内容卡片 -->
    <div class="card main-card">
      <!-- 搜索和筛选 -->
      <div class="filter-section">
        <div class="search-box">
          <el-input
            v-model="searchForm.keyword"
            placeholder="搜索收藏的专利..."
            clearable
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="filter-group">
          <el-select
            v-model="searchForm.groupName"
            placeholder="收藏分组"
            clearable
            class="group-select"
            @change="handleSearch"
          >
            <el-option
              v-for="group in favoriteGroups"
              :key="group"
              :label="group"
              :value="group"
            />
          </el-select>
          
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </div>
      </div>

      <!-- 收藏列表 -->
      <div v-if="favoriteList.length > 0" class="favorites-grid">
        <div 
          v-for="item in favoriteList" 
          :key="item.id" 
          class="favorite-card"
        >
          <div class="card-header">
            <div class="patent-info">
              <span v-if="item.publicationNo" class="patent-number">{{ item.publicationNo }}</span>
              <el-tag v-if="item.favoriteGroup" type="info" size="small" class="group-tag">
                {{ item.favoriteGroup }}
              </el-tag>
            </div>
            <div class="card-actions">
              <el-dropdown trigger="click">
                <el-button type="default" link>
                  <el-icon><MoreFilled /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="viewDetail(item.id)">
                      <el-icon><View /></el-icon>
                      查看详情
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleMatch(item)">
                      <el-icon><Connection /></el-icon>
                      技术匹配
                    </el-dropdown-item>
                    <el-dropdown-item @click="editFavorite(item)">
                      <el-icon><Edit /></el-icon>
                      编辑备注
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="removeFavorite(item)">
                      <el-icon><Delete /></el-icon>
                      取消收藏
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </div>
          
          <div class="card-body" @click="viewDetail(item.id)">
            <h4 class="patent-title">{{ item.title || '(处理中...)' }}</h4>
            <p v-if="item.applicant" class="patent-applicant">
              <el-icon><User /></el-icon>
              {{ item.applicant }}
            </p>
            <p v-if="item.patentAbstract" class="patent-abstract">
              {{ truncateText(item.patentAbstract, 120) }}
            </p>
          </div>
          
          <div class="card-footer">
            <div class="status-info">
              <span 
                class="status-dot"
                :class="getStatusClass(item.parseStatus)"
              ></span>
              <span class="status-text">{{ getStatusText(item.parseStatus) }}</span>
            </div>
            <div class="time-info">
              <el-icon><Clock /></el-icon>
              <span>{{ formatDate(item.favoriteTime || item.createdAt) }}</span>
            </div>
          </div>
          
          <div v-if="item.favoriteRemark" class="card-remark">
            <el-icon><Memo /></el-icon>
            <span>{{ item.favoriteRemark }}</span>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-else-if="!loading" class="empty-state">
        <el-empty description="暂无收藏的专利">
          <el-button type="primary" @click="$router.push('/patent/list')">
            浏览专利库
          </el-button>
        </el-empty>
      </div>

      <!-- 加载状态 -->
      <div v-if="loading" class="loading-state">
        <el-skeleton :rows="3" animated />
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination-wrapper">
        <div class="pagination-info">
          显示 {{ (pagination.page - 1) * pagination.size + 1 }} - 
          {{ Math.min(pagination.page * pagination.size, total) }} 条，
          共 {{ total }} 条记录
        </div>
        <el-pagination
          v-model:current-page="pagination.page"
          v-model:page-size="pagination.size"
          :page-sizes="[12, 24, 48]"
          :total="total"
          layout="sizes, prev, pager, next"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 编辑收藏对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑收藏"
      width="500px"
    >
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="专利标题">
          <el-input :value="currentItem?.title" disabled />
        </el-form-item>
        <el-form-item label="收藏分组">
          <el-select
            v-model="editForm.groupName"
            placeholder="选择或输入分组"
            filterable
            allow-create
            clearable
            style="width: 100%"
          >
            <el-option
              v-for="group in favoriteGroups"
              :key="group"
              :label="group"
              :value="group"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="editForm.remark"
            type="textarea"
            :rows="3"
            placeholder="添加备注信息..."
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editLoading" @click="saveEdit">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Star, Search, MoreFilled, View, Connection, Edit, Delete,
  User, Clock, Memo
} from '@element-plus/icons-vue'
import { favoriteApi } from '@/api/favorite'

const router = useRouter()

const loading = ref(false)
const favoriteList = ref([])
const favoriteGroups = ref([])
const total = ref(0)

const searchForm = reactive({
  keyword: '',
  groupName: ''
})

const pagination = reactive({
  page: 1,
  size: 12
})

// 编辑对话框
const editDialogVisible = ref(false)
const editLoading = ref(false)
const currentItem = ref(null)
const editForm = reactive({
  remark: '',
  groupName: ''
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

const truncateText = (text, maxLength) => {
  if (!text) return ''
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

// 获取收藏列表
const fetchList = async () => {
  loading.value = true
  try {
    const res = await favoriteApi.getList({
      pageNum: pagination.page,
      pageSize: pagination.size,
      keyword: searchForm.keyword || undefined,
      groupName: searchForm.groupName || undefined
    })
    if (res.code === 200) {
      favoriteList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (error) {
    console.error('获取收藏列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 获取分组列表
const fetchGroups = async () => {
  try {
    const res = await favoriteApi.getGroups()
    if (res.code === 200) {
      favoriteGroups.value = res.data || []
    }
  } catch (error) {
    console.error('获取分组列表失败:', error)
  }
}

const handleSearch = () => {
  pagination.page = 1
  fetchList()
}

const handleReset = () => {
  searchForm.keyword = ''
  searchForm.groupName = ''
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

const handleMatch = (item) => {
  router.push({
    path: '/match',
    query: { patentId: item.id }
  })
}

// 编辑收藏
const editFavorite = (item) => {
  currentItem.value = item
  editForm.remark = item.favoriteRemark || ''
  editForm.groupName = item.favoriteGroup || ''
  editDialogVisible.value = true
}

const saveEdit = async () => {
  if (!currentItem.value) return
  
  editLoading.value = true
  try {
    const res = await favoriteApi.update(currentItem.value.id, {
      remark: editForm.remark,
      groupName: editForm.groupName
    })
    if (res.code === 200) {
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      fetchList()
      fetchGroups()
    }
  } catch (error) {
    console.error('更新失败:', error)
    ElMessage.error('更新失败，请重试')
  } finally {
    editLoading.value = false
  }
}

// 取消收藏
const removeFavorite = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确定要取消收藏"${item.title || '该专利'}"吗？`,
      '提示',
      { type: 'warning' }
    )
    
    const res = await favoriteApi.remove(item.id)
    if (res.code === 200) {
      ElMessage.success('已取消收藏')
      fetchList()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('取消收藏失败:', error)
    }
  }
}

onMounted(() => {
  fetchList()
  fetchGroups()
})
</script>

<style lang="scss" scoped>
.favorites-page {
  max-width: 1400px;
  margin: 0 auto;
}

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

.stat-badge {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-radius: var(--radius-full);
  color: #d97706;
  font-weight: var(--font-medium);
  font-size: var(--text-sm);
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
  width: 300px;
}

.filter-group {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.group-select {
  width: 150px;
}

// 收藏网格
.favorites-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.favorite-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all 0.2s ease;
  
  &:hover {
    box-shadow: var(--shadow-md);
    transform: translateY(-2px);
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-bottom: 1px solid var(--color-border-light);
  background-color: var(--color-bg-secondary);
}

.patent-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.patent-number {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  background-color: var(--color-bg-primary);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
}

.group-tag {
  font-size: var(--text-xs);
}

.card-body {
  padding: var(--space-4);
  cursor: pointer;
}

.patent-title {
  font-size: var(--text-base);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin: 0 0 var(--space-2);
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  
  &:hover {
    color: var(--color-accent);
  }
}

.patent-applicant {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin: 0 0 var(--space-2);
}

.patent-abstract {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin: 0;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-top: 1px solid var(--color-border-light);
  background-color: var(--color-bg-secondary);
}

.status-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;

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
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.time-info {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.card-remark {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background-color: #fef3c7;
  font-size: var(--text-xs);
  color: #92400e;
  
  .el-icon {
    flex-shrink: 0;
    margin-top: 2px;
  }
}

// 空状态
.empty-state {
  padding: var(--space-12) 0;
}

// 加载状态
.loading-state {
  padding: var(--space-6);
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
@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    gap: var(--space-4);
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

  .favorites-grid {
    grid-template-columns: 1fr;
  }

  .pagination-wrapper {
    flex-direction: column;
    gap: var(--space-4);
  }
}
</style>
