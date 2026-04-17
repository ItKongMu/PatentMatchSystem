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

        <!-- 主体区域：表单 + 预览 分栏 -->
        <div class="upload-workspace" :class="{ 'has-preview': pdfPreviewUrl }">
          <!-- 左侧表单 -->
          <div class="form-panel">
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
                <!-- 未选文件时显示拖拽区域 -->
                <div v-if="!fileForm.file" class="drop-zone" :class="{ 'drag-over': isDragOver }"
                  @dragover.prevent="isDragOver = true"
                  @dragleave.prevent="isDragOver = false"
                  @drop.prevent="handleDrop"
                  @click="triggerFileInput"
                >
                  <input
                    ref="fileInputRef"
                    type="file"
                    accept=".pdf"
                    style="display:none"
                    @change="handleInputChange"
                  />
                  <div class="drop-zone-content">
                    <div class="drop-icon" :class="{ bouncing: isDragOver }">
                      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.2">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                        <line x1="12" y1="18" x2="12" y2="12"/>
                        <polyline points="9 15 12 12 15 15"/>
                      </svg>
                    </div>
                    <div class="drop-text">
                      <p class="primary-text">{{ isDragOver ? '松开鼠标上传文件' : '将 PDF 文件拖放到此处' }}</p>
                      <p class="secondary-text">或 <em>点击选择文件</em></p>
                    </div>
                    <div class="drop-tips">
                      <span class="tip-item">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                        仅支持 PDF 格式
                      </span>
                      <span class="tip-item">
                        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polyline points="20 6 9 17 4 12"/></svg>
                        最大 50 MB
                      </span>
                    </div>
                  </div>
                </div>

                <!-- 已选文件时显示文件信息卡片 -->
                <transition name="file-card">
                  <div v-if="fileForm.file" class="file-info-card">
                    <div class="file-icon-wrap">
                      <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                        <polyline points="14 2 14 8 20 8"/>
                      </svg>
                    </div>
                    <div class="file-meta">
                      <div class="file-name" :title="fileForm.file.name">{{ fileForm.file.name }}</div>
                      <div class="file-size-row">
                        <span class="file-size">{{ formatFileSize(fileForm.file.size) }}</span>
                        <span class="file-valid-badge">
                          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3"><polyline points="20 6 9 17 4 12"/></svg>
                          格式有效
                        </span>
                      </div>
                    </div>
                    <div class="file-actions">
                      <button class="btn-preview-toggle" @click="previewVisible = !previewVisible" :title="previewVisible ? '收起预览' : '展开预览'">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path v-if="previewVisible" d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                          <line v-if="previewVisible" x1="1" y1="1" x2="23" y2="23"/>
                          <path v-if="!previewVisible" d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                          <circle v-if="!previewVisible" cx="12" cy="12" r="3"/>
                        </svg>
                        {{ previewVisible ? '收起预览' : '预览文件' }}
                      </button>
                      <button class="btn-remove-file" @click="removeFile" title="移除文件">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"/>
                          <path d="M19 6l-1 14H6L5 6"/>
                          <path d="M10 11v6M14 11v6"/>
                          <path d="M9 6V4h6v2"/>
                        </svg>
                        重新选择
                      </button>
                    </div>
                  </div>
                </transition>
              </el-form-item>

              <!-- 上传进度 -->
              <transition name="fade">
                <div v-if="uploading" class="upload-progress-wrap">
                  <div class="upload-progress-label">
                    <span>{{ progressText }}</span>
                    <span class="progress-pct">{{ uploadProgress }}%</span>
                  </div>
                  <div class="upload-progress-bar">
                    <div class="upload-progress-fill" :style="{ width: uploadProgress + '%' }"></div>
                  </div>
                  <div class="upload-steps">
                    <div class="upload-step" :class="getStepClass(1)">
                      <span class="step-dot"></span>上传文件
                    </div>
                    <div class="upload-step-arrow">→</div>
                    <div class="upload-step" :class="getStepClass(2)">
                      <span class="step-dot"></span>解析内容
                    </div>
                    <div class="upload-step-arrow">→</div>
                    <div class="upload-step" :class="getStepClass(3)">
                      <span class="step-dot"></span>提取实体
                    </div>
                    <div class="upload-step-arrow">→</div>
                    <div class="upload-step" :class="getStepClass(4)">
                      <span class="step-dot"></span>向量化
                    </div>
                  </div>
                </div>
              </transition>

              <el-form-item class="form-actions">
                <el-button
                  type="primary"
                  size="large"
                  :loading="uploading"
                  :disabled="!fileForm.file"
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
                <el-button size="large" :disabled="uploading" @click="resetFileForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>

          <!-- 右侧 PDF 预览面板 -->
          <transition name="preview-slide">
            <div v-if="pdfPreviewUrl && previewVisible" class="preview-panel">
              <div class="preview-header">
                <div class="preview-title">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                    <circle cx="12" cy="12" r="3"/>
                  </svg>
                  文档预览
                </div>
                <div class="preview-controls">
                  <span class="preview-filename">{{ fileForm.file?.name }}</span>
                  <button class="preview-close-btn" @click="previewVisible = false" title="关闭预览">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                      <line x1="18" y1="6" x2="6" y2="18"/>
                      <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                  </button>
                </div>
              </div>
              <div class="preview-body">
                <iframe
                  :src="pdfPreviewUrl"
                  class="pdf-iframe"
                  title="PDF预览"
                  frameborder="0"
                ></iframe>
              </div>
              <div class="preview-footer">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/><path d="M12 8v4l3 3"/>
                </svg>
                预览仅供参考，上传后系统将自动解析全文
              </div>
            </div>
          </transition>
        </div>
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
              <span class="form-counter" :class="fullTextCounterClass">
                {{ textForm.fullText.length }} / {{ FILE_CONSTANTS.FULL_TEXT_MAX_LENGTH }} 字符
              </span>
            </div>
            <!-- 正文过长时的使用说明提示 -->
            <div v-if="fullTextStatus !== 'normal'" class="fulltext-hint" :class="`fulltext-hint--${fullTextStatus}`">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/>
              </svg>
              <span v-if="fullTextStatus === 'warning'">
                正文已超过 5 万字符，LLM 分析时将采用「首70%+尾30%」策略自动截取，权利要求等尾部内容仍会被覆盖。
              </span>
              <span v-else-if="fullTextStatus === 'danger'">
                正文已超过 8 万字符（接近上限 10 万），LLM 可能仅覆盖部分内容。建议精简正文，或拆分为多条专利录入。
              </span>
            </div>
            <!-- 正文长度影响说明（始终显示的轻量提示） -->
            <div class="fulltext-length-tip">
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><line x1="12" y1="17" x2="12.01" y2="17"/>
              </svg>
              正文长度影响分析质量：LLM 单次最多处理约 4000 字符，系统会智能截取首尾内容（建议控制在 5 万字以内以获得最佳效果）
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
import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { patentApi } from '@/api/patent'

const router = useRouter()

const activeTab = ref('file')
const uploading = ref(false)
const submitting = ref(false)
const isDragOver = ref(false)
const previewVisible = ref(true)
const uploadProgress = ref(0)
const uploadStep = ref(0) // 1-4 for each processing step
let progressTimer = null

// 常量定义 - 与后端保持一致
const FILE_CONSTANTS = {
  MAX_FILE_SIZE: 50 * 1024 * 1024,
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

// PDF预览
const pdfPreviewUrl = ref(null)
let objectUrl = null

// PDF上传表单
const fileFormRef = ref(null)
const fileInputRef = ref(null)
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

// 正文字符计数状态（normal / warning / danger）
const fullTextStatus = computed(() => {
  const len = textForm.fullText.length
  if (len >= 80000) return 'danger'
  if (len >= 50000) return 'warning'
  return 'normal'
})

// 正文计数器的 CSS class
const fullTextCounterClass = computed(() => {
  if (fullTextStatus.value === 'danger') return 'counter-danger'
  if (fullTextStatus.value === 'warning') return 'counter-warning'
  return ''
})

// 进度文案
const progressText = computed(() => {
  const texts = ['', '正在上传文件...', '正在解析文档内容...', '正在提取技术实体...', '正在生成语义向量...']
  return texts[uploadStep.value] || '处理中...'
})

const getStepClass = (step) => {
  if (uploadStep.value > step) return 'done'
  if (uploadStep.value === step) return 'active'
  return ''
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(2) + ' MB'
}

// 验证文件
const validateFile = (file) => {
  if (file.type !== FILE_CONSTANTS.ALLOWED_FILE_TYPE && !file.name.toLowerCase().endsWith('.pdf')) {
    ElMessage.error('只能上传PDF文件')
    return false
  }
  if (file.size > FILE_CONSTANTS.MAX_FILE_SIZE) {
    ElMessage.error(`文件大小不能超过${FILE_CONSTANTS.MAX_FILE_SIZE / 1024 / 1024}MB`)
    return false
  }
  if (file.size === 0) {
    ElMessage.error('文件内容为空，请选择有效的PDF文件')
    return false
  }
  return true
}

// 设置文件并生成预览
const setFile = (file) => {
  if (!validateFile(file)) return
  fileForm.file = file

  // 释放旧 objectUrl
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl)
  }
  objectUrl = URL.createObjectURL(file)
  pdfPreviewUrl.value = objectUrl
  previewVisible.value = true
}

// 点击触发文件输入
const triggerFileInput = () => {
  fileInputRef.value?.click()
}

// 文件 input change
const handleInputChange = (e) => {
  const file = e.target.files?.[0]
  if (file) setFile(file)
  e.target.value = ''
}

// 拖拽放入
const handleDrop = (e) => {
  isDragOver.value = false
  const file = e.dataTransfer.files?.[0]
  if (file) setFile(file)
}

// 移除文件
const removeFile = () => {
  fileForm.file = null
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl)
    objectUrl = null
  }
  pdfPreviewUrl.value = null
  previewVisible.value = false
}

// 模拟上传进度
const startProgressSimulation = () => {
  uploadProgress.value = 0
  uploadStep.value = 1
  let pct = 0

  progressTimer = setInterval(() => {
    if (pct < 30) {
      pct += Math.random() * 4
      uploadStep.value = 1
    } else if (pct < 60) {
      pct += Math.random() * 3
      uploadStep.value = 2
    } else if (pct < 85) {
      pct += Math.random() * 2
      uploadStep.value = 3
    } else if (pct < 95) {
      pct += Math.random() * 0.5
      uploadStep.value = 4
    }
    uploadProgress.value = Math.min(Math.round(pct), 95)
  }, 300)
}

const finishProgress = () => {
  clearInterval(progressTimer)
  uploadProgress.value = 100
  uploadStep.value = 4
}

const handleUpload = async () => {
  if (!fileForm.file) {
    ElMessage.error('请选择要上传的PDF文件')
    return
  }

  if (fileForm.publicationNo && !FILE_CONSTANTS.PUBLICATION_NO_PATTERN.test(fileForm.publicationNo)) {
    ElMessage.error('公开号格式不正确，示例：CN123456789A')
    return
  }

  uploading.value = true
  startProgressSimulation()

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
      finishProgress()
      if (processRes.code === 200) {
        ElMessage.success('上传成功，正在后台处理中...')
        resetFileForm()
        router.push({ path: '/patent/list', query: { polling: 'true' } })
      } else {
        resetFileForm()
        router.push('/patent/list')
      }
    } catch (processError) {
      console.warn('处理触发失败（专利已上传）:', processError)
      finishProgress()
      resetFileForm()
      router.push('/patent/list')
    }
  } catch (error) {
    console.error('上传处理失败:', error)
    const errorMsg = error.response?.data?.message || error.message || '上传处理失败，请检查网络连接'
    ElMessage.error(errorMsg)
  } finally {
    uploading.value = false
    clearInterval(progressTimer)
  }
}

const handleTextSubmit = async () => {
  if (!textFormRef.value) return

  await textFormRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
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
          resetTextForm()
          router.push('/patent/list')
        }
      } catch (processError) {
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
  removeFile()
  uploadProgress.value = 0
  uploadStep.value = 0
}

const resetTextForm = () => {
  textFormRef.value?.resetFields()
}

// 组件卸载时释放 objectUrl
onUnmounted(() => {
  if (objectUrl) URL.revokeObjectURL(objectUrl)
  clearInterval(progressTimer)
})
</script>

<style lang="scss" scoped>
.patent-upload-page {
  max-width: 1200px;
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
    transform: translateY(-1px);
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

// 工作区：分栏布局
.upload-workspace {
  display: flex;
  gap: var(--space-6);
  align-items: flex-start;
  transition: all var(--duration-slow) var(--ease-default);
}

.form-panel {
  flex: 0 0 auto;
  width: 100%;
  transition: width var(--duration-slow) var(--ease-default);

  .has-preview & {
    width: 420px;
    min-width: 380px;
  }
}

// 表单样式
.upload-form {
  width: 100%;
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

// ============================================
// 拖拽上传区域
// ============================================
.drop-zone {
  width: 100%;
  border: 2px dashed var(--color-border);
  border-radius: var(--radius-lg);
  background-color: var(--color-bg-secondary);
  padding: var(--space-10) var(--space-6);
  cursor: pointer;
  transition: all var(--duration-normal) var(--ease-default);
  user-select: none;

  &:hover, &.drag-over {
    border-color: var(--color-accent);
    background-color: #F0F5FF;
  }

  &.drag-over {
    transform: scale(1.01);
    box-shadow: 0 0 0 4px rgba(29, 78, 216, 0.15);
  }
}

.drop-zone-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.drop-icon {
  color: var(--color-text-muted);
  transition: all var(--duration-normal) var(--ease-default);

  .drop-zone:hover & {
    color: var(--color-accent);
    transform: translateY(-4px);
  }

  &.bouncing {
    color: var(--color-accent);
    animation: bounce 0.6s ease infinite alternate;
  }
}

@keyframes bounce {
  from { transform: translateY(0); }
  to   { transform: translateY(-8px); }
}

.drop-text {
  text-align: center;

  .primary-text {
    font-size: var(--text-base);
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
      text-decoration: underline;
      text-underline-offset: 2px;
    }
  }
}

.drop-tips {
  display: flex;
  gap: var(--space-4);
  margin-top: var(--space-1);
}

.tip-item {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-muted);

  svg {
    color: var(--color-success);
    flex-shrink: 0;
  }
}

// ============================================
// 文件信息卡片
// ============================================
.file-info-card {
  width: 100%;
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  background: linear-gradient(135deg, #EFF6FF 0%, #F8FAFF 100%);
  border: 1.5px solid #BFDBFE;
  border-radius: var(--radius-lg);
  box-shadow: 0 2px 8px rgba(29, 78, 216, 0.08);
}

.file-icon-wrap {
  width: 52px;
  height: 52px;
  background: var(--color-accent);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.file-meta {
  flex: 1;
  min-width: 0;

  .file-name {
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    margin-bottom: var(--space-1);
  }

  .file-size-row {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .file-size {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    font-family: var(--font-mono);
  }

  .file-valid-badge {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: var(--text-xs);
    color: var(--color-success);
    font-weight: var(--font-medium);

    svg {
      flex-shrink: 0;
    }
  }
}

.file-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  flex-shrink: 0;
}

.btn-preview-toggle,
.btn-remove-file {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  padding: var(--space-1) var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  cursor: pointer;
  border: 1.5px solid;
  transition: all var(--duration-fast) var(--ease-default);
  white-space: nowrap;
  line-height: 1.6;
  background: transparent;
}

.btn-preview-toggle {
  color: var(--color-accent);
  border-color: var(--color-accent);

  &:hover {
    background: var(--color-accent);
    color: #fff;
  }
}

.btn-remove-file {
  color: var(--color-text-muted);
  border-color: var(--color-border-dark);

  &:hover {
    color: var(--color-danger);
    border-color: var(--color-danger);
    background: #FEE2E2;
  }
}

// ============================================
// 上传进度
// ============================================
.upload-progress-wrap {
  margin: var(--space-4) 0;
  padding: var(--space-4) var(--space-5);
  background: var(--color-bg-secondary);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

.upload-progress-label {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);

  .progress-pct {
    font-family: var(--font-mono);
    font-weight: var(--font-semibold);
    color: var(--color-accent);
  }
}

.upload-progress-bar {
  width: 100%;
  height: 6px;
  background: var(--color-border);
  border-radius: var(--radius-full);
  overflow: hidden;
  margin-bottom: var(--space-3);
}

.upload-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--color-accent) 0%, var(--color-accent-light) 100%);
  border-radius: var(--radius-full);
  transition: width 0.4s var(--ease-out);
  position: relative;

  &::after {
    content: '';
    position: absolute;
    right: 0;
    top: 0;
    bottom: 0;
    width: 40px;
    background: linear-gradient(90deg, transparent, rgba(255,255,255,0.4));
    animation: shimmer 1.2s ease infinite;
  }
}

@keyframes shimmer {
  0% { opacity: 0; }
  50% { opacity: 1; }
  100% { opacity: 0; }
}

.upload-steps {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.upload-step {
  display: inline-flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-text-disabled);
  transition: all var(--duration-normal) var(--ease-default);

  .step-dot {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--color-border-dark);
    flex-shrink: 0;
    transition: all var(--duration-normal) var(--ease-default);
  }

  &.active {
    color: var(--color-accent);

    .step-dot {
      background: var(--color-accent);
      box-shadow: 0 0 0 3px rgba(29, 78, 216, 0.25);
      animation: pulse-dot 1s ease infinite;
    }
  }

  &.done {
    color: var(--color-success);

    .step-dot {
      background: var(--color-success);
    }
  }
}

.upload-step-arrow {
  font-size: var(--text-xs);
  color: var(--color-border-dark);
}

@keyframes pulse-dot {
  0%, 100% { box-shadow: 0 0 0 2px rgba(29, 78, 216, 0.2); }
  50% { box-shadow: 0 0 0 5px rgba(29, 78, 216, 0.1); }
}

// ============================================
// PDF 预览面板
// ============================================
.preview-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  height: 680px;
  background: var(--color-bg-secondary);
  box-shadow: var(--shadow-md);
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  background: var(--color-bg-primary);
  border-bottom: 1px solid var(--color-border);
  flex-shrink: 0;
}

.preview-title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);

  svg {
    color: var(--color-accent);
  }
}

.preview-controls {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.preview-filename {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  font-family: var(--font-mono);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.preview-close-btn {
  width: 24px;
  height: 24px;
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-text-muted);
  transition: all var(--duration-fast) var(--ease-default);

  &:hover {
    background: #FEE2E2;
    border-color: var(--color-danger);
    color: var(--color-danger);
  }
}

.preview-body {
  flex: 1;
  overflow: hidden;
  position: relative;
}

.pdf-iframe {
  width: 100%;
  height: 100%;
  border: none;
  display: block;
  background: #fff;
}

.preview-footer {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  background: var(--color-bg-primary);
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;

  svg {
    flex-shrink: 0;
    color: var(--color-info);
  }
}

// ============================================
// 过渡动画
// ============================================

// 文件卡片进入
.file-card-enter-active {
  transition: all 0.35s var(--ease-out);
}
.file-card-leave-active {
  transition: all 0.2s var(--ease-in);
}
.file-card-enter-from {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}
.file-card-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}

// 预览面板滑入
.preview-slide-enter-active {
  transition: all 0.4s cubic-bezier(0.34, 1.2, 0.64, 1);
}
.preview-slide-leave-active {
  transition: all 0.25s var(--ease-in);
}
.preview-slide-enter-from {
  opacity: 0;
  transform: translateX(30px) scale(0.98);
}
.preview-slide-leave-to {
  opacity: 0;
  transform: translateX(20px) scale(0.98);
}

// fade
.fade-enter-active { transition: opacity 0.3s var(--ease-out); }
.fade-leave-active { transition: opacity 0.2s var(--ease-in); }
.fade-enter-from, .fade-leave-to { opacity: 0; }

// ============================================
// 帮助信息
// ============================================
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

// ============================================
// 正文字符计数器颜色预警 & 提示
// ============================================
.form-counter {
  &.counter-warning {
    color: var(--el-color-warning);
    font-weight: var(--font-medium);
  }
  &.counter-danger {
    color: var(--el-color-danger);
    font-weight: var(--font-medium);
  }
}

.fulltext-hint {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  margin-top: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  line-height: var(--leading-normal);

  svg {
    flex-shrink: 0;
    margin-top: 2px;
  }

  &--warning {
    background: rgba(var(--el-color-warning-rgb), 0.08);
    border: 1px solid rgba(var(--el-color-warning-rgb), 0.3);
    color: var(--el-color-warning-dark-2);
    svg { stroke: var(--el-color-warning); }
  }

  &--danger {
    background: rgba(var(--el-color-danger-rgb), 0.08);
    border: 1px solid rgba(var(--el-color-danger-rgb), 0.3);
    color: var(--el-color-danger-dark-2);
    svg { stroke: var(--el-color-danger); }
  }
}

.fulltext-length-tip {
  display: flex;
  align-items: flex-start;
  gap: var(--space-1);
  margin-top: var(--space-2);
  font-size: 12px;
  color: var(--color-text-tertiary);
  line-height: var(--leading-normal);

  svg {
    flex-shrink: 0;
    margin-top: 1px;
    stroke: var(--color-text-tertiary);
  }
}

// ============================================
// 响应式
// ============================================
@media (max-width: 900px) {
  .upload-workspace {
    flex-direction: column;

    &.has-preview .form-panel {
      width: 100%;
    }
  }

  .preview-panel {
    height: 480px;
  }
}

@media (max-width: 768px) {
  .upload-methods {
    grid-template-columns: 1fr;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .file-info-card {
    flex-wrap: wrap;
  }

  .file-actions {
    flex-direction: row;
    width: 100%;
  }
}
</style>
