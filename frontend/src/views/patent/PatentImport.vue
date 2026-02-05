<template>
  <div class="page-container import-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">CSV批量导入</h1>
        <p class="page-desc">支持批量导入专利元数据，支持CSV格式文件</p>
      </div>
      <el-button @click="downloadTemplate">
        <el-icon><Download /></el-icon>
        下载模板
      </el-button>
    </div>

    <!-- 上传区域 -->
    <div class="card upload-card">
      <h3 class="card-title">上传CSV文件</h3>
      
      <el-upload
        ref="uploadRef"
        class="upload-area"
        drag
        accept=".csv"
        :auto-upload="false"
        :limit="1"
        :on-change="handleFileChange"
        :on-exceed="handleExceed"
        :show-file-list="false"
      >
        <div class="upload-content">
          <el-icon class="upload-icon"><UploadFilled /></el-icon>
          <div class="upload-text">
            <span class="main-text">拖拽CSV文件到此处，或</span>
            <em>点击上传</em>
          </div>
          <div class="upload-tip">
            支持 .csv 格式，文件大小不超过 10MB
          </div>
        </div>
      </el-upload>

      <div v-if="selectedFile" class="file-info">
        <el-icon><Document /></el-icon>
        <span class="file-name">{{ selectedFile.name }}</span>
        <span class="file-size">({{ formatFileSize(selectedFile.size) }})</span>
        <el-button type="danger" link size="small" @click="clearFile">
          移除
        </el-button>
      </div>

      <div class="action-buttons">
        <el-button 
          type="primary" 
          :loading="previewLoading"
          :disabled="!selectedFile"
          @click="handlePreview"
        >
          <el-icon><View /></el-icon>
          预览数据
        </el-button>
      </div>
    </div>

    <!-- 预览数据 -->
    <div v-if="previewData" class="card preview-card">
      <div class="preview-header">
        <h3 class="card-title">数据预览</h3>
        <div class="preview-stats">
          <span class="stat-item">
            <span class="stat-label">总计:</span>
            <span class="stat-value">{{ previewData.totalRows }}</span>
          </span>
          <span class="stat-item success">
            <span class="stat-label">有效:</span>
            <span class="stat-value">{{ previewData.validRows }}</span>
          </span>
          <span v-if="previewData.invalidRows > 0" class="stat-item error">
            <span class="stat-label">无效:</span>
            <span class="stat-value">{{ previewData.invalidRows }}</span>
          </span>
        </div>
      </div>

      <el-table 
        :data="previewData.previewData" 
        class="preview-table"
        max-height="400"
        stripe
      >
        <el-table-column prop="rowNum" label="行号" width="70" align="center">
          <template #default="{ row }">
            <span class="row-num">{{ row.rowNum }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="publicationNo" label="公开号" width="140">
          <template #default="{ row }">
            <span v-if="row.publicationNo" class="mono-text">{{ row.publicationNo }}</span>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="180">
          <template #default="{ row }">
            <div class="title-cell">
              {{ row.title || '—' }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="applicant" label="申请人" width="120">
          <template #default="{ row }">
            {{ row.applicant || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="publicationDate" label="公开日期" width="110">
          <template #default="{ row }">
            {{ row.publicationDate || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="ipcClassification" label="IPC分类" width="160">
          <template #default="{ row }">
            <el-tooltip 
              :content="row.ipcClassification" 
              placement="top" 
              :show-after="500"
              :disabled="!row.ipcClassification || row.ipcClassification.length < 20"
            >
              <span v-if="row.ipcClassification" class="mono-text ipc-cell">
                {{ truncateText(row.ipcClassification, 20) }}
              </span>
              <span v-else class="text-muted">—</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="patentAbstract" label="摘要" min-width="180">
          <template #default="{ row }">
            <el-tooltip 
              :content="row.patentAbstract" 
              placement="top" 
              :show-after="500"
              :disabled="!row.patentAbstract || row.patentAbstract.length < 50"
            >
              <div class="abstract-cell">
                {{ truncateText(row.patentAbstract, 50) }}
              </div>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="fullText" label="正文" width="120" align="center">
          <template #default="{ row }">
            <el-tooltip 
              v-if="row.fullText && row.fullText.length > 0"
              :content="truncateText(row.fullText, 200)" 
              placement="top" 
              :show-after="500"
            >
              <el-tag type="success" size="small">
                {{ row.fullText.length > 1000 ? (Math.round(row.fullText.length / 1000) + 'K字') : (row.fullText.length + '字') }}
              </el-tag>
            </el-tooltip>
            <span v-else class="text-muted">—</span>
          </template>
        </el-table-column>
        <el-table-column prop="valid" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.valid" type="success" size="small">有效</el-tag>
            <el-tooltip v-else :content="row.errorMessage" placement="top">
              <el-tag type="danger" size="small">无效</el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>

      <div class="import-options">
        <el-checkbox v-model="autoProcess">
          导入后自动处理（提取实体和向量化）
        </el-checkbox>
        <div v-if="autoProcess && previewData.validRows > 5" class="batch-hint">
          <el-icon><InfoFilled /></el-icon>
          <span>系统将使用批处理模式提高处理效率（批量向量化和ES索引同步）</span>
        </div>
      </div>

      <div class="action-buttons">
        <el-button @click="clearPreview">取消</el-button>
        <el-button 
          type="primary" 
          :loading="importLoading"
          :disabled="!previewData.canImport"
          @click="handleImport"
        >
          <el-icon><Check /></el-icon>
          确认导入 {{ previewData.validRows }} 条
        </el-button>
      </div>
    </div>

    <!-- 导入结果 -->
    <el-dialog
      v-model="showResultDialog"
      title="导入结果"
      width="600px"
      :close-on-click-modal="false"
    >
      <div v-if="importResult" class="import-result">
        <div class="result-summary">
          <div class="result-item success">
            <el-icon><CircleCheckFilled /></el-icon>
            <span class="count">{{ importResult.successCount }}</span>
            <span class="label">成功导入</span>
          </div>
          <div v-if="importResult.failedCount > 0" class="result-item error">
            <el-icon><CircleCloseFilled /></el-icon>
            <span class="count">{{ importResult.failedCount }}</span>
            <span class="label">导入失败</span>
          </div>
          <div v-if="importResult.skippedCount > 0" class="result-item warning">
            <el-icon><WarningFilled /></el-icon>
            <span class="count">{{ importResult.skippedCount }}</span>
            <span class="label">已跳过</span>
          </div>
        </div>

        <div v-if="importResult.failedRows?.length > 0" class="failed-list">
          <h4>失败详情</h4>
          <el-table :data="importResult.failedRows" max-height="200" size="small">
            <el-table-column prop="rowNum" label="行号" width="60" />
            <el-table-column prop="title" label="标题" min-width="150" />
            <el-table-column prop="errorMessage" label="错误原因" min-width="150">
              <template #default="{ row }">
                <span class="error-text">{{ row.errorMessage }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <template #footer>
        <el-button @click="showResultDialog = false">关闭</el-button>
        <el-button type="primary" @click="goToList">
          查看专利列表
        </el-button>
      </template>
    </el-dialog>

    <!-- CSV格式说明 -->
    <div class="card format-card">
      <h3 class="card-title">CSV格式说明</h3>
      <div class="format-content">
        <p>CSV文件应包含以下列（按顺序）：</p>
        <table class="format-table">
          <thead>
            <tr>
              <th>列名</th>
              <th>说明</th>
              <th>是否必填</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>公开号</td>
              <td>专利公开号/专利号</td>
              <td>可选</td>
            </tr>
            <tr>
              <td>标题</td>
              <td>专利名称</td>
              <td><strong>必填</strong></td>
            </tr>
            <tr>
              <td>申请人</td>
              <td>专利申请人</td>
              <td>可选</td>
            </tr>
            <tr>
              <td>公开日期</td>
              <td>格式：yyyy-MM-dd 或 yyyy/MM/dd</td>
              <td>可选</td>
            </tr>
            <tr>
              <td>IPC分类</td>
              <td>IPC分类号，多个用逗号分隔（如 G06F 16/30, H04L 9/32）</td>
              <td>可选</td>
            </tr>
            <tr>
              <td>摘要</td>
              <td>专利摘要/技术方案描述</td>
              <td><strong>必填</strong></td>
            </tr>
            <tr>
              <td>正文</td>
              <td>专利正文内容（技术领域、背景技术、发明内容等）</td>
              <td>可选（如有则存入系统用于深度分析）</td>
            </tr>
          </tbody>
        </table>
        <div class="format-tips">
          <h4>说明：</h4>
          <ul>
            <li>如果提供了正文字段，系统会将完整内容存储到MinIO，用于后续的实体提取和向量化分析</li>
            <li>如果正文为空，系统仅使用摘要进行分析处理</li>
            <li>包含逗号的字段请用英文双引号包围</li>
          </ul>
        </div>
        <div class="format-example">
          <h4>示例：</h4>
          <code>
公开号,标题,申请人,公开日期,IPC分类,摘要,正文
CN123456A,一种智能检测方法,某某公司,2024-01-15,"G06F 16/30, G06N 3/08",本发明提供一种基于深度学习的...,"技术领域 本发明涉及..."
CN789012B,数据处理装置,研究院,2024-02-20,G06F 17/30,本实用新型涉及一种数据处理装置...,
          </code>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, genFileId } from 'element-plus'
import { 
  Download, UploadFilled, Document, View, Check,
  CircleCheckFilled, CircleCloseFilled, WarningFilled, InfoFilled 
} from '@element-plus/icons-vue'
import { patentApi } from '@/api/patent'

const router = useRouter()

// 常量定义 - 与后端保持一致
const CSV_CONSTANTS = {
  MAX_FILE_SIZE: 10 * 1024 * 1024, // 10MB
  ALLOWED_MIME_TYPES: ['text/csv', 'application/vnd.ms-excel', 'text/plain'],
  ALLOWED_EXTENSIONS: ['.csv'],
  TITLE_MIN_LENGTH: 5,
  TITLE_MAX_LENGTH: 200,
  ABSTRACT_MIN_LENGTH: 10,
  ABSTRACT_MAX_LENGTH: 5000
}

const uploadRef = ref(null)
const selectedFile = ref(null)
const previewLoading = ref(false)
const importLoading = ref(false)
const previewData = ref(null)
const autoProcess = ref(false)
const showResultDialog = ref(false)
const importResult = ref(null)

/**
 * 验证CSV文件
 * @param {File} file - 文件对象
 * @returns {boolean} - 是否有效
 */
const validateCsvFile = (file) => {
  // 验证文件扩展名
  const fileName = file.name.toLowerCase()
  const hasValidExtension = CSV_CONSTANTS.ALLOWED_EXTENSIONS.some(ext => fileName.endsWith(ext))
  if (!hasValidExtension) {
    ElMessage.error('只支持.csv格式的文件')
    return false
  }
  
  // 验证MIME类型（某些系统可能识别为不同类型）
  const isValidMimeType = CSV_CONSTANTS.ALLOWED_MIME_TYPES.includes(file.type) || file.type === ''
  if (!isValidMimeType) {
    ElMessage.warning('文件类型可能不正确，请确保是有效的CSV文件')
  }
  
  // 验证文件大小
  if (file.size > CSV_CONSTANTS.MAX_FILE_SIZE) {
    ElMessage.error(`文件大小不能超过${CSV_CONSTANTS.MAX_FILE_SIZE / 1024 / 1024}MB`)
    return false
  }
  
  // 验证文件是否为空
  if (file.size === 0) {
    ElMessage.error('文件内容为空，请选择有效的CSV文件')
    return false
  }
  
  return true
}

// 处理文件选择
const handleFileChange = (file) => {
  if (!validateCsvFile(file.raw)) {
    uploadRef.value?.clearFiles()
    selectedFile.value = null
    return
  }
  selectedFile.value = file.raw
  previewData.value = null
}

// 处理文件超出限制
const handleExceed = (files) => {
  uploadRef.value.clearFiles()
  const file = files[0]
  
  // 先验证新文件
  if (!validateCsvFile(file)) {
    return
  }
  
  file.uid = genFileId()
  uploadRef.value.handleStart(file)
  selectedFile.value = file
}

// 清除选中的文件
const clearFile = () => {
  selectedFile.value = null
  previewData.value = null
  uploadRef.value?.clearFiles()
}

// 清除预览
const clearPreview = () => {
  previewData.value = null
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

// 截断文本
const truncateText = (text, maxLength) => {
  if (!text) return '—'
  return text.length > maxLength ? text.substring(0, maxLength) + '...' : text
}

// 预览CSV
const handlePreview = async () => {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择CSV文件')
    return
  }

  previewLoading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)

    const res = await patentApi.previewCsv(formData)
    if (res.code === 200) {
      previewData.value = res.data
      // 设置默认可导入状态（如果后端没有返回）
      if (previewData.value.canImport === undefined) {
        previewData.value.canImport = previewData.value.validRows > 0
      }
      
      if (res.data.invalidRows > 0) {
        ElMessage.warning(`解析完成，有 ${res.data.invalidRows} 条数据无效`)
      } else {
        ElMessage.success('解析完成')
      }
    } else {
      ElMessage.error(res.message || '文件解析失败')
    }
  } catch (error) {
    console.error('预览失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '文件解析失败，请检查CSV格式'
    ElMessage.error(errorMsg)
  } finally {
    previewLoading.value = false
  }
}

// 执行导入
const handleImport = async () => {
  if (!previewData.value?.canImport) {
    ElMessage.warning('没有可导入的有效数据')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要导入 ${previewData.value.validRows} 条专利数据吗？${autoProcess.value ? '导入后将自动开始处理。' : ''}`,
      '确认导入',
      { type: 'info' }
    )

    importLoading.value = true
    
    // 只导入有效数据
    const validData = previewData.value.allData.filter(item => item.valid)
    
    if (validData.length === 0) {
      ElMessage.warning('没有可导入的有效数据')
      return
    }
    
    const res = await patentApi.importCsvPreviewData(validData, autoProcess.value)
    
    if (res.code === 200) {
      importResult.value = res.data
      showResultDialog.value = true
      
      // 清空预览
      clearFile()
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('导入失败:', error)
      const errorMsg = error.response?.data?.message || error.message || '导入失败，请重试'
      ElMessage.error(errorMsg)
    }
  } finally {
    importLoading.value = false
  }
}

// 跳转到专利列表
const goToList = () => {
  showResultDialog.value = false
  router.push({ 
    path: '/patent/list',
    query: autoProcess.value ? { polling: 'true' } : undefined
  })
}

// 下载CSV模板
const downloadTemplate = () => {
  const template = `公开号,标题,申请人,公开日期,IPC分类,摘要,正文
CN123456A,一种智能检测方法,某某公司,2024-01-15,"G06F 16/30, G06N 3/08",本发明提供一种基于深度学习的智能检测方法，包括数据采集、特征提取和模型推理等步骤。该方法能够实现高精度的目标检测。,"技术领域 本发明涉及人工智能技术领域，尤其涉及一种智能检测方法。背景技术 目前，智能检测方法越来越受到重视..."
CN789012B,数据处理装置,研究院,2024-02-20,G06F 17/30,本实用新型涉及一种数据处理装置，包括输入模块、处理模块和输出模块。该装置能够高效处理大规模数据。,`

  const blob = new Blob(['\uFEFF' + template], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = '专利导入模板.csv'
  link.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('模板下载成功')
}
</script>

<style lang="scss" scoped>
.import-page {
  max-width: 1200px;
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

.card-title {
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  margin: 0 0 var(--space-5);
  color: var(--color-text-primary);
}

// 上传区域
.upload-card {
  margin-bottom: var(--space-6);
}

.upload-area {
  :deep(.el-upload-dragger) {
    width: 100%;
    border-radius: var(--radius-lg);
    border: 2px dashed var(--color-border);
    background-color: var(--color-bg-secondary);
    transition: all 0.2s ease;
    
    &:hover {
      border-color: var(--color-accent);
      background-color: var(--color-bg-tertiary);
    }
  }
}

.upload-content {
  padding: var(--space-8) var(--space-4);
  text-align: center;
}

.upload-icon {
  font-size: 48px;
  color: var(--color-text-disabled);
  margin-bottom: var(--space-4);
}

.upload-text {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  margin-bottom: var(--space-2);
  
  em {
    color: var(--color-accent);
    font-style: normal;
    cursor: pointer;
  }
}

.upload-tip {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
}

.file-info {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-3) var(--space-4);
  background-color: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  margin-top: var(--space-4);
  
  .el-icon {
    color: var(--color-accent);
  }
  
  .file-name {
    font-weight: var(--font-medium);
  }
  
  .file-size {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
  }
}

.action-buttons {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  margin-top: var(--space-5);
}

// 预览区域
.preview-card {
  margin-bottom: var(--space-6);
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
  
  .card-title {
    margin: 0;
  }
}

.preview-stats {
  display: flex;
  gap: var(--space-4);
}

.stat-item {
  font-size: var(--text-sm);
  
  .stat-label {
    color: var(--color-text-muted);
    margin-right: var(--space-1);
  }
  
  .stat-value {
    font-weight: var(--font-semibold);
  }
  
  &.success .stat-value {
    color: var(--color-success);
  }
  
  &.error .stat-value {
    color: var(--color-danger);
  }
}

.preview-table {
  margin-bottom: var(--space-4);
}

.row-num {
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.mono-text {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
}

.text-muted {
  color: var(--color-text-disabled);
}

.title-cell, .abstract-cell, .ipc-cell {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ipc-cell {
  display: inline-block;
  max-width: 100%;
}

.import-options {
  padding: var(--space-4);
  background-color: var(--color-bg-secondary);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-4);

  .batch-hint {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-top: var(--space-3);
    padding: var(--space-2) var(--space-3);
    background-color: #e6f7ff;
    border-radius: var(--radius-sm);
    font-size: var(--text-xs);
    color: var(--color-info);
    
    .el-icon {
      font-size: 14px;
    }
  }
}

// 导入结果
.import-result {
  .result-summary {
    display: flex;
    justify-content: center;
    gap: var(--space-8);
    padding: var(--space-6);
    background-color: var(--color-bg-secondary);
    border-radius: var(--radius-lg);
    margin-bottom: var(--space-4);
  }
  
  .result-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-2);
    
    .el-icon {
      font-size: 32px;
    }
    
    .count {
      font-size: var(--text-2xl);
      font-weight: var(--font-bold);
    }
    
    .label {
      font-size: var(--text-sm);
      color: var(--color-text-muted);
    }
    
    &.success {
      .el-icon, .count { color: var(--color-success); }
    }
    
    &.error {
      .el-icon, .count { color: var(--color-danger); }
    }
    
    &.warning {
      .el-icon, .count { color: var(--color-warning); }
    }
  }
  
  .failed-list {
    h4 {
      font-size: var(--text-sm);
      font-weight: var(--font-medium);
      margin-bottom: var(--space-2);
    }
    
    .error-text {
      color: var(--color-danger);
      font-size: var(--text-xs);
    }
  }
}

// 格式说明
.format-card {
  .format-content {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    
    p {
      margin-bottom: var(--space-3);
    }
  }
  
  .format-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: var(--space-4);
    
    th, td {
      padding: var(--space-2) var(--space-3);
      border: 1px solid var(--color-border);
      text-align: left;
    }
    
    th {
      background-color: var(--color-bg-secondary);
      font-weight: var(--font-medium);
    }
  }
  
  .format-tips {
    margin-bottom: var(--space-4);
    
    h4 {
      font-size: var(--text-sm);
      font-weight: var(--font-medium);
      margin-bottom: var(--space-2);
      color: var(--color-text-primary);
    }
    
    ul {
      margin: 0;
      padding-left: var(--space-4);
      
      li {
        margin-bottom: var(--space-1);
        color: var(--color-text-secondary);
        font-size: var(--text-sm);
      }
    }
  }

  .format-example {
    background-color: var(--color-bg-secondary);
    border-radius: var(--radius-md);
    padding: var(--space-4);
    
    h4 {
      font-size: var(--text-sm);
      font-weight: var(--font-medium);
      margin-bottom: var(--space-2);
    }
    
    code {
      display: block;
      font-family: var(--font-mono);
      font-size: var(--text-xs);
      white-space: pre-wrap;
      word-break: break-all;
    }
  }
}
</style>
