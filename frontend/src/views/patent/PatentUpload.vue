<template>
  <div class="page-container patent-upload-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">专利录入</h1>
        <p class="page-desc">上传专利PDF文档或手动录入专利信息，系统将自动解析并提取技术实体</p>
      </div>
    </div>
    
    <!-- 上传方式选择 -->
    <div class="upload-methods">
      <div 
        class="method-card" 
        :class="{ active: activeTab === 'file' }"
        @click="activeTab = 'file'"
      >
        <div class="method-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <path d="M12 18v-6"/>
            <path d="M9 15l3-3 3 3"/>
          </svg>
        </div>
        <div class="method-info">
          <h3>PDF文件上传</h3>
          <p>上传专利PDF文档，系统自动解析全文内容</p>
        </div>
        <div class="method-badge">推荐</div>
      </div>
      
      <div 
        class="method-card" 
        :class="{ active: activeTab === 'text' }"
        @click="activeTab = 'text'"
      >
        <div class="method-icon">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
          </svg>
        </div>
        <div class="method-info">
          <h3>文本手动录入</h3>
          <p>手动输入专利标题、摘要等关键信息</p>
        </div>
      </div>
    </div>
    
    <!-- 上传表单区域 -->
    <div class="card form-card">
      <!-- PDF 上传表单 -->
      <div v-show="activeTab === 'file'" class="form-section">
        <div class="section-header">
          <h2 class="section-title">PDF文件上传</h2>
          <p class="section-desc">支持中国专利、美国专利等标准格式PDF文档</p>
        </div>
        
        <el-form
          ref="fileFormRef"
          :model="fileForm"
          :rules="fileRules"
          label-position="top"
          class="upload-form"
        >
          <el-form-item label="公开号（可选）" prop="publicationNo">
            <el-input
              v-model="fileForm.publicationNo"
              placeholder="如：CN123456789A、US1234567B1"
              class="form-input"
            >
              <template #prefix>
                <el-icon>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M20 14.66V20a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h5.34"/>
                    <polygon points="18 2 22 6 12 16 8 16 8 12 18 2"/>
                  </svg>
                </el-icon>
              </template>
            </el-input>
            <div class="form-hint">如果留空，系统将尝试从PDF中自动提取</div>
          </el-form-item>
          
          <el-form-item label="专利文件" prop="file" required>
            <el-upload
              ref="uploadRef"
              class="upload-dragger"
              drag
              :auto-upload="false"
              :limit="1"
              accept=".pdf"
              :on-change="handleFileChange"
              :on-exceed="handleExceed"
              :on-remove="handleFileRemove"
            >
              <div class="upload-content">
                <div class="upload-icon">
                  <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                    <polyline points="17 8 12 3 7 8"/>
                    <line x1="12" y1="3" x2="12" y2="15"/>
                  </svg>
                </div>
                <div class="upload-text">
                  <p class="primary-text">将PDF文件拖放到此处</p>
                  <p class="secondary-text">或 <em>点击浏览</em> 选择文件</p>
                </div>
                <div class="upload-tips">
                  <span class="tip-item">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                      <polyline points="22 4 12 14.01 9 11.01"/>
                    </svg>
                    仅支持PDF格式
                  </span>
                  <span class="tip-item">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                      <polyline points="22 4 12 14.01 9 11.01"/>
                    </svg>
                    最大50MB
                  </span>
                </div>
              </div>
            </el-upload>
          </el-form-item>
          
          <el-form-item class="form-actions">
            <el-button
              type="primary"
              size="large"
              :loading="uploading"
              @click="handleUpload"
            >
              <el-icon v-if="!uploading">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="17 8 12 3 7 8"/>
                  <line x1="12" y1="3" x2="12" y2="15"/>
                </svg>
              </el-icon>
              {{ uploading ? '上传处理中...' : '上传并处理' }}
            </el-button>
            <el-button size="large" @click="resetFileForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
      
      <!-- 文本录入表单 -->
      <div v-show="activeTab === 'text'" class="form-section">
        <div class="section-header">
          <h2 class="section-title">文本手动录入</h2>
          <p class="section-desc">直接输入专利的核心信息，适合已有结构化数据的场景</p>
        </div>
        
        <el-form
          ref="textFormRef"
          :model="textForm"
          :rules="textRules"
          label-position="top"
          class="upload-form"
        >
          <div class="form-row">
            <el-form-item label="公开号" prop="publicationNo" class="form-col">
              <el-input
                v-model="textForm.publicationNo"
                placeholder="如：CN123456789A"
                class="form-input"
              />
            </el-form-item>
            
            <el-form-item label="公开日期" prop="publicationDate" class="form-col">
              <el-date-picker
                v-model="textForm.publicationDate"
                type="date"
                placeholder="选择日期"
                value-format="YYYY-MM-DD"
                class="form-input"
                style="width: 100%"
              />
            </el-form-item>
          </div>
          
          <el-form-item label="专利标题" prop="title">
            <el-input
              v-model="textForm.title"
              placeholder="请输入完整的专利标题"
              class="form-input"
            />
            <div class="form-hint">标题应准确反映专利的核心技术内容</div>
          </el-form-item>
          
          <el-form-item label="申请人 / 专利权人" prop="applicant">
            <el-input
              v-model="textForm.applicant"
              placeholder="如：华为技术有限公司"
              class="form-input"
            />
          </el-form-item>

          <el-form-item label="IPC分类号" prop="ipcClassification">
            <el-input
              v-model="textForm.ipcClassification"
              placeholder="如：G06F 16/30, G06N 3/08（多个用逗号分隔）"
              class="form-input"
            />
            <div class="form-hint">IPC国际专利分类号，用于领域归类</div>
          </el-form-item>
          
          <el-form-item label="专利摘要" prop="patentAbstract">
            <el-input
              v-model="textForm.patentAbstract"
              type="textarea"
              :rows="6"
              placeholder="请输入专利摘要内容，建议50字以上以保证实体提取效果..."
              class="form-textarea"
            />
            <div class="form-counter">
              {{ textForm.patentAbstract.length }} / 5000 字符
            </div>
          </el-form-item>

          <el-form-item label="专利正文（可选）" prop="fullText">
            <el-input
              v-model="textForm.fullText"
              type="textarea"
              :rows="10"
              placeholder="请输入专利正文内容，包括技术领域、背景技术、发明内容等（如有则用于深度分析）..."
              class="form-textarea"
            />
            <div class="form-counter-group">
              <span class="form-hint">如提供正文，系统将存入MinIO用于向量化和深度分析</span>
              <span class="form-counter">{{ textForm.fullText.length }} 字符</span>
            </div>
          </el-form-item>
          
          <el-form-item class="form-actions">
            <el-button
              type="primary"
              size="large"
              :loading="submitting"
              @click="handleTextSubmit"
            >
              <el-icon v-if="!submitting">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                  <polyline points="22 4 12 14.01 9 11.01"/>
                </svg>
              </el-icon>
              {{ submitting ? '提交处理中...' : '提交并处理' }}
            </el-button>
            <el-button size="large" @click="resetTextForm">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
    
    <!-- 帮助信息 -->
    <div class="help-section">
      <div class="help-card">
        <div class="help-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
            <line x1="12" y1="17" x2="12.01" y2="17"/>
          </svg>
        </div>
        <div class="help-content">
          <h4>处理流程说明</h4>
          <ol>
            <li><strong>文档解析</strong> — 提取专利的标题、摘要、权利要求等结构化信息</li>
            <li><strong>实体提取</strong> — 使用LLM识别技术实体（产品、方法、材料等）</li>
            <li><strong>领域分类</strong> — 自动归类到IPC国际专利分类体系</li>
            <li><strong>向量化</strong> — 生成语义向量用于相似度匹配</li>
          </ol>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { patentApi } from '@/api/patent'

const router = useRouter()

const activeTab = ref('file')
const uploading = ref(false)
const submitting = ref(false)

// 常量定义 - 与后端保持一致
const FILE_CONSTANTS = {
  MAX_FILE_SIZE: 50 * 1024 * 1024, // 50MB
  ALLOWED_FILE_TYPE: 'application/pdf',
  ALLOWED_EXTENSIONS: ['.pdf'],
  TITLE_MIN_LENGTH: 5,
  TITLE_MAX_LENGTH: 200,
  ABSTRACT_MIN_LENGTH: 50,
  ABSTRACT_MAX_LENGTH: 5000,
  FULL_TEXT_MAX_LENGTH: 100000,
  APPLICANT_MAX_LENGTH: 200,
  IPC_MAX_LENGTH: 500,
  PUBLICATION_NO_PATTERN: /^[A-Z]{2}\d+[A-Z]?\d*$/
}

// PDF上传表单
const fileFormRef = ref(null)
const uploadRef = ref(null)
const fileForm = reactive({
  publicationNo: '',
  file: null
})

const fileRules = {
  publicationNo: [
    { 
      pattern: FILE_CONSTANTS.PUBLICATION_NO_PATTERN, 
      message: '公开号格式不正确，示例：CN123456789A', 
      trigger: 'blur' 
    }
  ]
}

// 文本录入表单
const textFormRef = ref(null)
const textForm = reactive({
  publicationNo: '',
  title: '',
  applicant: '',
  publicationDate: '',
  ipcClassification: '',
  patentAbstract: '',
  fullText: ''
})

const textRules = {
  publicationNo: [
    { 
      pattern: FILE_CONSTANTS.PUBLICATION_NO_PATTERN, 
      message: '公开号格式不正确，示例：CN123456789A', 
      trigger: 'blur' 
    }
  ],
  title: [
    { required: true, message: '请输入专利标题', trigger: 'blur' },
    { 
      min: FILE_CONSTANTS.TITLE_MIN_LENGTH, 
      max: FILE_CONSTANTS.TITLE_MAX_LENGTH, 
      message: `标题长度应在${FILE_CONSTANTS.TITLE_MIN_LENGTH}-${FILE_CONSTANTS.TITLE_MAX_LENGTH}个字符之间`, 
      trigger: 'blur' 
    }
  ],
  applicant: [
    { 
      max: FILE_CONSTANTS.APPLICANT_MAX_LENGTH, 
      message: `申请人长度不能超过${FILE_CONSTANTS.APPLICANT_MAX_LENGTH}个字符`, 
      trigger: 'blur' 
    }
  ],
  ipcClassification: [
    { 
      max: FILE_CONSTANTS.IPC_MAX_LENGTH, 
      message: `IPC分类号长度不能超过${FILE_CONSTANTS.IPC_MAX_LENGTH}个字符`, 
      trigger: 'blur' 
    }
  ],
  patentAbstract: [
    { required: true, message: '请输入专利摘要', trigger: 'blur' },
    { 
      min: FILE_CONSTANTS.ABSTRACT_MIN_LENGTH, 
      max: FILE_CONSTANTS.ABSTRACT_MAX_LENGTH, 
      message: `摘要长度应在${FILE_CONSTANTS.ABSTRACT_MIN_LENGTH}-${FILE_CONSTANTS.ABSTRACT_MAX_LENGTH}个字符之间`, 
      trigger: 'blur' 
    }
  ],
  fullText: [
    { 
      max: FILE_CONSTANTS.FULL_TEXT_MAX_LENGTH, 
      message: `正文长度不能超过${FILE_CONSTANTS.FULL_TEXT_MAX_LENGTH / 10000}万个字符`, 
      trigger: 'blur' 
    }
  ]
}

/**
 * 验证上传文件
 * @param {File} file - 文件对象
 * @returns {boolean} - 是否有效
 */
const validateFile = (file) => {
  // 验证文件类型
  if (file.type !== FILE_CONSTANTS.ALLOWED_FILE_TYPE) {
    ElMessage.error('只能上传PDF文件')
    return false
  }
  
  // 验证文件扩展名
  const fileName = file.name.toLowerCase()
  const hasValidExtension = FILE_CONSTANTS.ALLOWED_EXTENSIONS.some(ext => fileName.endsWith(ext))
  if (!hasValidExtension) {
    ElMessage.error('只支持.pdf格式的文件')
    return false
  }
  
  // 验证文件大小
  if (file.size > FILE_CONSTANTS.MAX_FILE_SIZE) {
    ElMessage.error(`文件大小不能超过${FILE_CONSTANTS.MAX_FILE_SIZE / 1024 / 1024}MB`)
    return false
  }
  
  // 验证文件是否为空
  if (file.size === 0) {
    ElMessage.error('文件内容为空，请选择有效的PDF文件')
    return false
  }
  
  return true
}

const handleFileChange = (file) => {
  if (!validateFile(file.raw)) {
    uploadRef.value?.clearFiles()
    return
  }
  fileForm.file = file.raw
}

const handleFileRemove = () => {
  fileForm.file = null
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件，请先删除已选文件')
}

const handleUpload = async () => {
  if (!fileForm.file) {
    ElMessage.error('请选择要上传的PDF文件')
    return
  }
  
  // 验证公开号格式（如果填写）
  if (fileForm.publicationNo && !FILE_CONSTANTS.PUBLICATION_NO_PATTERN.test(fileForm.publicationNo)) {
    ElMessage.error('公开号格式不正确，示例：CN123456789A')
    return
  }
  
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', fileForm.file)
    if (fileForm.publicationNo) {
      formData.append('publicationNo', fileForm.publicationNo.trim())
    }
    
    const uploadRes = await patentApi.upload(formData)
    if (uploadRes.code !== 200) {
      ElMessage.error(uploadRes.message || '上传失败')
      return
    }
    
    const patentId = uploadRes.data.patentId
    
    try {
      const processRes = await patentApi.process(patentId)
      if (processRes.code === 200) {
        ElMessage.success('上传成功，正在处理中...')
        resetFileForm()
        router.push({ path: '/patent/list', query: { polling: 'true' } })
      } else {
        // 上传成功但触发处理失败（非200业务错误已由 request.js 弹出），跳转列表页
        resetFileForm()
        router.push('/patent/list')
      }
    } catch (processError) {
      // HTTP 错误（403等）已由 request.js 拦截器统一弹出消息
      // 专利已上传成功，只是处理触发失败，仍跳转列表页
      console.warn('处理触发失败（专利已上传）:', processError)
      resetFileForm()
      router.push('/patent/list')
    }
  } catch (error) {
    console.error('上传处理失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '上传处理失败，请检查网络连接'
    ElMessage.error(errorMsg)
  } finally {
    uploading.value = false
  }
}

const handleTextSubmit = async () => {
  if (!textFormRef.value) return
  
  await textFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitting.value = true
    try {
      // 构建请求数据，清理空白字符
      const requestData = {
        ...textForm,
        title: textForm.title?.trim(),
        applicant: textForm.applicant?.trim(),
        publicationNo: textForm.publicationNo?.trim(),
        ipcClassification: textForm.ipcClassification?.trim(),
        patentAbstract: textForm.patentAbstract?.trim(),
        fullText: textForm.fullText?.trim()
      }
      
      const createRes = await patentApi.createByText(requestData)
      if (createRes.code !== 200) {
        ElMessage.error(createRes.message || '录入失败')
        return
      }
      
      const patentId = createRes.data.patentId
      
      try {
        const processRes = await patentApi.process(patentId)
        if (processRes.code === 200) {
          ElMessage.success('提交成功，正在处理中...')
          resetTextForm()
          router.push({ path: '/patent/list', query: { polling: 'true' } })
        } else {
          // 录入成功但触发处理失败（非200业务错误已由 request.js 弹出），跳转列表页
          resetTextForm()
          router.push('/patent/list')
        }
      } catch (processError) {
        // HTTP 错误（403等）已由 request.js 拦截器统一弹出消息
        // 专利已录入成功，只是处理触发失败，仍跳转列表页
        console.warn('处理触发失败（专利已录入）:', processError)
        resetTextForm()
        router.push('/patent/list')
      }
    } catch (error) {
      console.error('提交处理失败:', error)
      const errorMsg = error.response?.data?.message || error.message || '提交处理失败，请检查网络连接'
      ElMessage.error(errorMsg)
    } finally {
      submitting.value = false
    }
  })
}

const resetFileForm = () => {
  fileForm.publicationNo = ''
  fileForm.file = null
  uploadRef.value?.clearFiles()
}

const resetTextForm = () => {
  textFormRef.value?.resetFields()
}
</script>

<style lang="scss" scoped>
.patent-upload-page {
  max-width: 960px;
  margin: 0 auto;
}

// 页面头部
.page-header {
  margin-bottom: var(--space-6);
  
  .page-desc {
    color: var(--color-text-muted);
    font-size: var(--text-sm);
    margin: 0;
    margin-top: var(--space-2);
  }
}

// 上传方式选择
.upload-methods {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.method-card {
  background: var(--color-bg-primary);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  align-items: center;
  gap: var(--space-4);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  position: relative;

  &:hover {
    border-color: var(--color-border-dark);
    box-shadow: var(--shadow-sm);
  }

  &.active {
    border-color: var(--color-accent);
    background-color: #F8FAFF;
    box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.1);
  }
}

.method-icon {
  width: 56px;
  height: 56px;
  background-color: var(--color-bg-tertiary);
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-accent);
  flex-shrink: 0;
  transition: all var(--duration-normal) var(--ease-default);

  .method-card.active & {
    background-color: var(--color-accent);
    color: #fff;
  }
}

.method-info {
  flex: 1;

  h3 {
    font-family: var(--font-heading);
    font-size: var(--text-lg);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin-bottom: var(--space-1);
  }

  p {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
    margin: 0;
  }
}

.method-badge {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  background-color: var(--color-success);
  color: #fff;
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-full);
}

// 表单卡片
.form-card {
  margin-bottom: var(--space-6);
}

.section-header {
  margin-bottom: var(--space-6);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border-light);
}

.section-title {
  font-family: var(--font-heading);
  font-size: var(--text-xl);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-2);

  &::before {
    content: '§';
    color: var(--color-accent);
    margin-right: var(--space-2);
  }
}

.section-desc {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
  margin: 0;
}

// 表单样式
.upload-form {
  max-width: 640px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}

.form-col {
  margin-bottom: 0;
}

.form-input {
  :deep(.el-input__wrapper) {
    border-radius: var(--radius-md);
  }
}

.form-textarea {
  :deep(.el-textarea__inner) {
    border-radius: var(--radius-md);
    font-family: var(--font-body);
    line-height: var(--leading-relaxed);
  }
}

.form-hint {
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.form-counter {
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  text-align: right;
}

.form-counter-group {
  margin-top: var(--space-2);
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .form-hint {
    margin-top: 0;
  }
  
  .form-counter {
    margin-top: 0;
  }
}

.form-actions {
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border-light);

  .el-button {
    min-width: 140px;
  }
}

// 上传拖拽区域
.upload-dragger {
  width: 100%;

  :deep(.el-upload) {
    width: 100%;
  }

  :deep(.el-upload-dragger) {
    width: 100%;
    height: auto;
    padding: var(--space-10) var(--space-6);
    border: 2px dashed var(--color-border);
    border-radius: var(--radius-lg);
    background-color: var(--color-bg-secondary);
    transition: all var(--duration-normal) var(--ease-default);

    &:hover {
      border-color: var(--color-accent);
      background-color: #F8FAFF;
    }
  }
}

.upload-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.upload-icon {
  color: var(--color-text-muted);
  margin-bottom: var(--space-4);
}

.upload-text {
  text-align: center;
  margin-bottom: var(--space-4);

  .primary-text {
    font-size: var(--text-lg);
    font-weight: var(--font-medium);
    color: var(--color-text-primary);
    margin-bottom: var(--space-1);
  }

  .secondary-text {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
    margin: 0;

    em {
      color: var(--color-accent);
      font-style: normal;
      font-weight: var(--font-medium);
    }
  }
}

.upload-tips {
  display: flex;
  gap: var(--space-4);
}

.tip-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

// 帮助信息
.help-section {
  margin-top: var(--space-6);
}

.help-card {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-5);
  display: flex;
  gap: var(--space-4);
}

.help-icon {
  width: 40px;
  height: 40px;
  background-color: #FEF3C7;
  color: var(--color-warning);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.help-content {
  h4 {
    font-family: var(--font-heading);
    font-size: var(--text-base);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin-bottom: var(--space-3);
  }

  ol {
    margin: 0;
    padding-left: var(--space-5);
    color: var(--color-text-secondary);
    font-size: var(--text-sm);
    line-height: var(--leading-loose);

    li {
      margin-bottom: var(--space-1);

      strong {
        color: var(--color-text-primary);
        font-weight: var(--font-medium);
      }
    }
  }
}

// 响应式
@media (max-width: 768px) {
  .upload-methods {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
