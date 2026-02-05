<template>
  <div class="page-container patent-match-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">技术匹配分析</h1>
        <p class="page-desc">基于大语言模型的多维度专利相似度匹配，支持文本查询和专利互检</p>
      </div>
    </div>
    
    <!-- 匹配方式切换 -->
    <div class="match-tabs">
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'text' }"
        @click="activeTab = 'text'"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
        </svg>
        文本查询匹配
      </button>
      <button 
        class="tab-btn" 
        :class="{ active: activeTab === 'patent' }"
        @click="activeTab = 'patent'"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
          <polyline points="14 2 14 8 20 8"/>
          <line x1="16" y1="13" x2="8" y2="13"/>
          <line x1="16" y1="17" x2="8" y2="17"/>
        </svg>
        专利相似匹配
      </button>
    </div>
    
    <!-- 查询表单区域 -->
    <div class="card query-card">
      <!-- 文本匹配表单 -->
      <div v-show="activeTab === 'text'" class="query-section">
        <el-form
          ref="textFormRef"
          :model="textForm"
          :rules="textRules"
          label-position="top"
          class="query-form"
        >
          <el-form-item label="技术描述" prop="query">
            <el-input
              v-model="textForm.query"
              type="textarea"
              :rows="5"
              placeholder="请输入技术描述文本，例如：一种基于深度学习的医学影像诊断方法，采用卷积神经网络对CT图像进行特征提取..."
              class="query-textarea"
            />
            <div class="form-footer">
              <span class="char-count">{{ textForm.query.length }} 字符</span>
              <span class="form-tip">建议输入50字以上以获得更准确的匹配结果</span>
            </div>
          </el-form-item>
          
          <div class="form-row">
            <el-form-item label="领域过滤" class="form-col">
              <el-select
                v-model="textForm.domainFilter"
                placeholder="全部领域"
                clearable
                class="domain-select"
              >
                <el-option label="A - 人类生活必需" value="A" />
                <el-option label="B - 作业；运输" value="B" />
                <el-option label="C - 化学；冶金" value="C" />
                <el-option label="D - 纺织；造纸" value="D" />
                <el-option label="E - 固定建筑物" value="E" />
                <el-option label="F - 机械工程" value="F" />
                <el-option label="G - 物理" value="G" />
                <el-option label="H - 电学" value="H" />
              </el-select>
            </el-form-item>
            
            <el-form-item label="返回数量" class="form-col">
              <div class="slider-wrapper">
                <el-slider
                  v-model="textForm.topK"
                  :min="5"
                  :max="50"
                  :step="5"
                  :marks="{ 5: '5', 25: '25', 50: '50' }"
                />
                <span class="slider-value">{{ textForm.topK }}</span>
              </div>
            </el-form-item>
          </div>
          
          <div class="form-actions">
            <el-button
              type="primary"
              size="large"
              :loading="matching"
              @click="handleTextMatch"
            >
              <el-icon v-if="!matching">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
              </el-icon>
              {{ matching ? '匹配分析中...' : '开始匹配' }}
            </el-button>
            <el-button size="large" @click="resetTextForm">重置</el-button>
          </div>
        </el-form>
      </div>
      
      <!-- 专利匹配表单 -->
      <div v-show="activeTab === 'patent'" class="query-section">
        <el-form
          ref="patentFormRef"
          :model="patentForm"
          :rules="patentRules"
          label-position="top"
          class="query-form"
        >
          <el-form-item label="选择源专利" prop="patentId">
            <el-select
              v-model="patentForm.patentId"
              placeholder="请选择已处理的专利"
              filterable
              remote
              :remote-method="searchPatents"
              :loading="searchLoading"
              class="patent-select"
            >
              <el-option
                v-for="item in patentOptions"
                :key="item.id"
                :label="`${item.publicationNo || 'ID:' + item.id} - ${item.title}`"
                :value="item.id"
              />
            </el-select>
            <div class="form-tip">仅显示已完成处理的专利，可输入关键词搜索</div>
          </el-form-item>
          
          <el-form-item label="返回数量" class="slider-form-item">
            <div class="slider-wrapper full-width">
              <el-slider
                v-model="patentForm.topK"
                :min="5"
                :max="50"
                :step="5"
                :marks="{ 5: '5', 25: '25', 50: '50' }"
              />
              <span class="slider-value">{{ patentForm.topK }}</span>
            </div>
          </el-form-item>
          
          <div class="form-actions">
            <el-button
              type="primary"
              size="large"
              :loading="matching"
              @click="handlePatentMatch"
            >
              <el-icon v-if="!matching">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="18" cy="5" r="3"/>
                  <circle cx="6" cy="12" r="3"/>
                  <circle cx="18" cy="19" r="3"/>
                  <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
                  <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
                </svg>
              </el-icon>
              {{ matching ? '匹配分析中...' : '开始匹配' }}
            </el-button>
            <el-button size="large" @click="resetPatentForm">重置</el-button>
          </div>
        </el-form>
      </div>
    </div>
    
    <!-- 匹配结果 -->
    <div v-if="matchResult" class="results-section">
      <!-- 结果头部 -->
      <div class="results-header">
        <div class="results-title">
          <h2>匹配结果</h2>
          <span class="results-count">找到 {{ matchResult.matches?.length || 0 }} 条相似专利</span>
        </div>
      </div>
      
      <!-- 查询实体分析 - 增强版带匹配高亮 -->
      <div v-if="matchResult.queryEntities?.length" class="analysis-card card">
        <div class="analysis-header">
          <h3>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="16" x2="12" y2="12"/>
              <line x1="12" y1="8" x2="12.01" y2="8"/>
            </svg>
            查询实体分析
          </h3>
          <span class="entity-count">
            识别到 {{ matchResult.queryEntities.length }} 个技术实体
            <span v-if="matchedEntityCount > 0" class="matched-hint">
              （{{ matchedEntityCount }} 个已匹配）
            </span>
          </span>
        </div>
        <div class="entity-tags">
          <el-tooltip
            v-for="(entity, index) in matchResult.queryEntities"
            :key="index"
            :content="getEntityTooltip(entity.name)"
            :disabled="!isEntityMatched(entity.name)"
            placement="top"
          >
            <span
              class="entity-tag"
              :class="[
                `entity-${entity.type?.toLowerCase()}`,
                { 'entity-matched': isEntityMatched(entity.name) }
              ]"
            >
              <span class="entity-name">{{ entity.name }}</span>
              <small>{{ entity.type }}</small>
              <span v-if="isEntityMatched(entity.name)" class="match-indicator">
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3">
                  <polyline points="20 6 9 17 4 12"/>
                </svg>
              </span>
            </span>
          </el-tooltip>
        </div>
        
        <!-- 匹配统计摘要 -->
        <div v-if="matchedEntityCount > 0" class="match-summary">
          <div class="summary-item">
            <span class="summary-icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
            </span>
            <span>已匹配实体：<strong>{{ matchedEntityCount }}</strong> / {{ matchResult.queryEntities.length }}</span>
          </div>
          <div class="summary-item" v-if="avgEntitySimilarity > 0">
            <span class="summary-icon">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="20" x2="18" y2="10"/>
                <line x1="12" y1="20" x2="12" y2="4"/>
                <line x1="6" y1="20" x2="6" y2="14"/>
              </svg>
            </span>
            <span>平均匹配度：<strong>{{ avgEntitySimilarity }}%</strong></span>
          </div>
        </div>
      </div>
      
      <!-- 结果列表 -->
      <div class="results-list">
        <div
          v-for="(item, index) in matchResult.matches"
          :key="item.patentId"
          class="result-card card"
        >
          <div class="result-rank">
            <span class="rank-number">{{ index + 1 }}</span>
          </div>
          
          <div class="result-content">
            <div class="result-header">
              <div class="result-title-row">
                <h3 class="result-title" @click="viewPatent(item.patentId)">
                  {{ item.title }}
                </h3>
                <div class="score-badge" :class="getScoreClass(item.similarityScore)">
                  <span class="score-value">{{ (item.similarityScore * 100).toFixed(1) }}</span>
                  <span class="score-unit">%</span>
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
                <span v-if="item.domainCodes?.length" class="meta-item">
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                    <path d="M3 9h18M9 21V9"/>
                  </svg>
                  {{ item.domainCodes.slice(0, 3).join(', ') }}
                </span>
              </div>
            </div>
            
            <p class="result-abstract">{{ truncateText(item.abstract, 200) }}</p>
            
            <!-- 匹配标签和简要原因 -->
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
                {{ item.matchReason }}
              </span>
            </div>

            <!-- 详细匹配解释展开区域 -->
            <div class="match-explanation-section">
              <el-collapse v-model="expandedItems[item.patentId]">
                <el-collapse-item :name="item.patentId">
                  <template #title>
                    <div class="explanation-toggle">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
                        <line x1="12" y1="17" x2="12.01" y2="17"/>
                      </svg>
                      <span>查看详细匹配分析</span>
                    </div>
                  </template>
                  
                  <div class="explanation-content">
                    <!-- 整体分析 -->
                    <div v-if="item.explanation?.overallAnalysis" class="explanation-block">
                      <h4>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                          <polyline points="14 2 14 8 20 8"/>
                          <line x1="16" y1="13" x2="8" y2="13"/>
                          <line x1="16" y1="17" x2="8" y2="17"/>
                        </svg>
                        整体匹配分析
                      </h4>
                      <p>{{ item.explanation.overallAnalysis }}</p>
                    </div>

                    <!-- 技术相似性分析 -->
                    <div v-if="item.explanation?.technicalSimilarity" class="explanation-block">
                      <h4>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <line x1="18" y1="20" x2="18" y2="10"/>
                          <line x1="12" y1="20" x2="12" y2="4"/>
                          <line x1="6" y1="20" x2="6" y2="14"/>
                        </svg>
                        技术相似性
                      </h4>
                      <div class="similarity-bars">
                        <div class="similarity-item">
                          <span class="similarity-label">方法相似度</span>
                          <el-progress 
                            :percentage="item.explanation.technicalSimilarity.methodSimilarity || 0" 
                            :stroke-width="8"
                            :color="getProgressColor(item.explanation.technicalSimilarity.methodSimilarity)"
                          />
                        </div>
                        <div class="similarity-item">
                          <span class="similarity-label">结构相似度</span>
                          <el-progress 
                            :percentage="item.explanation.technicalSimilarity.structureSimilarity || 0" 
                            :stroke-width="8"
                            :color="getProgressColor(item.explanation.technicalSimilarity.structureSimilarity)"
                          />
                        </div>
                        <div class="similarity-item">
                          <span class="similarity-label">效果相似度</span>
                          <el-progress 
                            :percentage="item.explanation.technicalSimilarity.effectSimilarity || 0" 
                            :stroke-width="8"
                            :color="getProgressColor(item.explanation.technicalSimilarity.effectSimilarity)"
                          />
                        </div>
                      </div>
                      <div v-if="item.explanation.technicalSimilarity.keyDifference" class="key-difference">
                        <strong>关键差异：</strong>{{ item.explanation.technicalSimilarity.keyDifference }}
                      </div>
                    </div>

                    <!-- 实体匹配详情 -->
                    <div v-if="item.matchedEntityDetails?.length" class="explanation-block">
                      <h4>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
                          <line x1="7" y1="7" x2="7.01" y2="7"/>
                        </svg>
                        实体匹配详情
                      </h4>
                      <div class="entity-match-list">
                        <div 
                          v-for="(em, idx) in item.matchedEntityDetails" 
                          :key="idx" 
                          class="entity-match-item"
                        >
                          <div class="entity-match-header">
                            <span class="entity-pair">
                              <span class="query-entity" :class="`entity-${em.entityType?.toLowerCase()}`">
                                {{ em.queryEntity }}
                              </span>
                              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <line x1="5" y1="12" x2="19" y2="12"/>
                                <polyline points="12 5 19 12 12 19"/>
                              </svg>
                              <span class="matched-entity" :class="`entity-${em.entityType?.toLowerCase()}`">
                                {{ em.matchedEntity }}
                              </span>
                            </span>
                            <span class="entity-similarity">{{ em.similarity }}%</span>
                          </div>
                          <div class="entity-match-reason">
                            <span class="entity-type-badge">{{ em.entityType }}</span>
                            {{ em.matchReason }}
                          </div>
                        </div>
                      </div>
                    </div>

                    <!-- 创新点对比 -->
                    <div v-if="item.explanation?.innovationPoint" class="explanation-block">
                      <h4>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                        </svg>
                        创新点分析
                      </h4>
                      <p>{{ item.explanation.innovationPoint }}</p>
                    </div>

                    <!-- 应用场景分析 -->
                    <div v-if="item.explanation?.applicationScenario" class="explanation-block">
                      <h4>
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <rect x="2" y="7" width="20" height="14" rx="2" ry="2"/>
                          <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/>
                        </svg>
                        应用场景
                      </h4>
                      <p>{{ item.explanation.applicationScenario }}</p>
                    </div>
                  </div>
                </el-collapse-item>
              </el-collapse>
            </div>
            
            <div class="result-footer">
              <el-button 
                type="primary" 
                size="small"
                @click="viewPatent(item.patentId)"
              >
                查看专利详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 空结果 -->
      <div v-if="!matchResult.matches?.length" class="empty-results">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1">
          <circle cx="11" cy="11" r="8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          <line x1="8" y1="11" x2="14" y2="11"/>
        </svg>
        <h3>未找到匹配的专利</h3>
        <p>尝试修改查询内容或放宽筛选条件</p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onActivated, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { matchApi } from '@/api/match'
import { patentApi } from '@/api/patent'

const route = useRoute()
const router = useRouter()

const activeTab = ref('text')
const matching = ref(false)
const searchLoading = ref(false)
const textMatchResult = ref(null)
const patentMatchResult = ref(null)
const patentOptions = ref([])
const expandedItems = ref({})  // 控制详细解释展开状态

// 文本匹配表单
const textFormRef = ref(null)
const textForm = reactive({
  query: '',
  domainFilter: '',
  topK: 10
})

const textRules = {
  query: [
    { required: true, message: '请输入查询文本', trigger: 'blur' },
    { min: 20, message: '查询文本至少20个字符', trigger: 'blur' }
  ]
}

// 专利匹配表单
const patentFormRef = ref(null)
const patentForm = reactive({
  patentId: null,
  topK: 10
})

const patentRules = {
  patentId: [
    { required: true, message: '请选择专利', trigger: 'change' }
  ]
}

const matchResult = computed(() => {
  return activeTab.value === 'text' ? textMatchResult.value : patentMatchResult.value
})

// 收集所有匹配结果中的实体匹配详情
const allMatchedEntities = computed(() => {
  if (!matchResult.value?.matches) return new Map()
  
  const entityMap = new Map()
  matchResult.value.matches.forEach(match => {
    if (match.matchedEntityDetails) {
      match.matchedEntityDetails.forEach(detail => {
        const key = detail.queryEntity
        if (!entityMap.has(key) || entityMap.get(key).similarity < detail.similarity) {
          entityMap.set(key, detail)
        }
      })
    }
  })
  return entityMap
})

// 已匹配实体数量
const matchedEntityCount = computed(() => {
  return allMatchedEntities.value.size
})

// 平均实体匹配度
const avgEntitySimilarity = computed(() => {
  if (allMatchedEntities.value.size === 0) return 0
  let total = 0
  allMatchedEntities.value.forEach(detail => {
    total += detail.similarity || 0
  })
  return Math.round(total / allMatchedEntities.value.size)
})

// 检查实体是否被匹配
const isEntityMatched = (entityName) => {
  return allMatchedEntities.value.has(entityName)
}

// 获取实体匹配的提示信息
const getEntityTooltip = (entityName) => {
  const detail = allMatchedEntities.value.get(entityName)
  if (!detail) return ''
  return `匹配到: ${detail.matchedEntity} (相似度: ${detail.similarity}%)`
}

const getScoreClass = (score) => {
  if (score >= 0.8) return 'high'
  if (score >= 0.6) return 'medium'
  return 'low'
}

const truncateText = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

// 获取进度条颜色
const getProgressColor = (percentage) => {
  if (percentage >= 80) return '#10B981'  // 绿色
  if (percentage >= 60) return '#F59E0B'  // 黄色
  if (percentage >= 40) return '#3B82F6'  // 蓝色
  return '#EF4444'  // 红色
}

const loadDefaultPatents = async () => {
  try {
    const res = await patentApi.getList({
      page: 1,
      size: 20,
      parseStatus: 'SUCCESS'
    })
    if (res.code === 200) {
      const existingIds = patentOptions.value.map(p => p.id)
      const newPatents = (res.data.records || []).filter(p => !existingIds.includes(p.id))
      patentOptions.value = [...patentOptions.value, ...newPatents]
    }
  } catch (error) {
    console.error('加载专利列表失败:', error)
  }
}

const searchPatents = async (query) => {
  if (!query) {
    await loadDefaultPatents()
    return
  }
  
  searchLoading.value = true
  try {
    const res = await patentApi.getList({
      page: 1,
      size: 20,
      keyword: query,
      parseStatus: 'SUCCESS'
    })
    if (res.code === 200) {
      patentOptions.value = res.data.records || []
    }
  } catch (error) {
    console.error('搜索专利失败:', error)
  } finally {
    searchLoading.value = false
  }
}

const handleTextMatch = async () => {
  if (!textFormRef.value) return
  
  await textFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    matching.value = true
    textMatchResult.value = null
    
    try {
      const res = await matchApi.match({
        query: textForm.query,
        domainFilter: textForm.domainFilter || undefined,
        topK: textForm.topK
      })
      
      if (res.code === 200) {
        textMatchResult.value = res.data
        ElMessage.success(`匹配完成，找到 ${res.data.matches?.length || 0} 条结果`)
      }
    } catch (error) {
      console.error('匹配失败:', error)
    } finally {
      matching.value = false
    }
  })
}

const handlePatentMatch = async () => {
  if (!patentFormRef.value) return
  
  await patentFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    matching.value = true
    patentMatchResult.value = null
    
    try {
      const res = await matchApi.matchByPatent(patentForm.patentId, {
        topK: patentForm.topK
      })
      
      if (res.code === 200) {
        patentMatchResult.value = res.data
        ElMessage.success(`匹配完成，找到 ${res.data.matches?.length || 0} 条结果`)
      }
    } catch (error) {
      console.error('匹配失败:', error)
    } finally {
      matching.value = false
    }
  })
}

const viewPatent = (id) => {
  router.push(`/patent/detail/${id}`)
}

const resetTextForm = () => {
  textFormRef.value?.resetFields()
  textMatchResult.value = null
}

const resetPatentForm = () => {
  patentFormRef.value?.resetFields()
  patentMatchResult.value = null
}

const handlePatentIdFromUrl = async () => {
  const patentId = route.query.patentId
  if (patentId) {
    activeTab.value = 'patent'
    patentForm.patentId = Number(patentId)
    
    try {
      const res = await patentApi.getDetail(patentId)
      if (res.code === 200) {
        const exists = patentOptions.value.some(p => p.id === res.data.id)
        if (!exists) {
          patentOptions.value = [res.data, ...patentOptions.value]
        }
      }
    } catch (error) {
      console.error('加载专利失败:', error)
    }
  }
}

onMounted(async () => {
  await loadDefaultPatents()
  await handlePatentIdFromUrl()
})

onActivated(async () => {
  await handlePatentIdFromUrl()
})

watch(() => route.query.patentId, async (newPatentId) => {
  if (newPatentId) {
    await handlePatentIdFromUrl()
  }
})
</script>

<style lang="scss" scoped>
.patent-match-page {
  max-width: 1000px;
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

// 匹配方式切换
.match-tabs {
  display: flex;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

.tab-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  height: 56px;
  background: var(--color-bg-primary);
  border: 2px solid var(--color-border);
  border-radius: var(--radius-lg);
  font-family: var(--font-body);
  font-size: var(--text-base);
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
    color: var(--color-text-primary);
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

// 查询表单
.query-card {
  margin-bottom: var(--space-6);
}

.query-form {
  max-width: 100%;
}

.query-textarea {
  :deep(.el-textarea__inner) {
    font-family: var(--font-body);
    line-height: var(--leading-relaxed);
    border-radius: var(--radius-md);
  }
}

.form-footer {
  display: flex;
  justify-content: space-between;
  margin-top: var(--space-2);
}

.char-count {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  font-family: var(--font-mono);
}

.form-tip {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.form-row {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: var(--space-6);
  align-items: start;
}

.form-col {
  margin-bottom: 0;
}

.domain-select {
  width: 100%;
}

.patent-select {
  width: 100%;
}

.slider-wrapper {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  min-width: 300px;
  
  .el-slider {
    flex: 1;
    min-width: 200px;
  }
}

.slider-value {
  min-width: 48px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-mono);
  font-size: var(--text-lg);
  font-weight: var(--font-semibold);
  color: var(--color-accent);
  background-color: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
  flex-shrink: 0;
}

// 单独的滑动条表单项（专利匹配页）
.slider-form-item {
  max-width: 500px;
}

.slider-wrapper.full-width {
  width: 100%;
}

.form-actions {
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border-light);
  display: flex;
  gap: var(--space-3);

  .el-button {
    min-width: 140px;
  }
}

// 结果区域
.results-section {
  margin-top: var(--space-8);
}

.results-header {
  margin-bottom: var(--space-5);
}

.results-title {
  display: flex;
  align-items: baseline;
  gap: var(--space-3);

  h2 {
    font-family: var(--font-heading);
    font-size: var(--text-xl);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }

  .results-count {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }
}

// 实体分析卡片
.analysis-card {
  margin-bottom: var(--space-5);
}

.analysis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);

  h3 {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-family: var(--font-heading);
    font-size: var(--text-base);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0;
  }

  .entity-count {
    font-size: var(--text-sm);
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
  gap: var(--space-2);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  border: 1px solid;
  position: relative;
  transition: all var(--duration-normal) var(--ease-default);

  small {
    font-size: var(--text-xs);
    opacity: 0.7;
  }

  // 匹配高亮状态
  &.entity-matched {
    box-shadow: 0 0 0 2px var(--color-success), var(--shadow-sm);
    transform: scale(1.02);
    
    .match-indicator {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 16px;
      height: 16px;
      background: var(--color-success);
      color: #fff;
      border-radius: 50%;
      margin-left: var(--space-1);
    }
  }

  &.entity-product {
    background-color: #EFF6FF;
    border-color: #BFDBFE;
    color: #1E40AF;
  }

  &.entity-method {
    background-color: #ECFDF5;
    border-color: #A7F3D0;
    color: #047857;
  }

  &.entity-material {
    background-color: #FEF3C7;
    border-color: #FDE68A;
    color: #B45309;
  }

  &.entity-component {
    background-color: #F3F4F6;
    border-color: #D1D5DB;
    color: #4B5563;
  }

  &.entity-effect {
    background-color: #FEE2E2;
    border-color: #FECACA;
    color: #B91C1C;
  }

  &.entity-application {
    background-color: #F3E8FF;
    border-color: #DDD6FE;
    color: #7C3AED;
  }
}

.matched-hint {
  color: var(--color-success);
  font-weight: var(--font-medium);
}

// 匹配统计摘要
.match-summary {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--color-border-light);
}

.summary-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);

  .summary-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 24px;
    height: 24px;
    background: var(--color-bg-tertiary);
    border-radius: 50%;
    
    svg {
      color: var(--color-accent);
    }
  }

  strong {
    color: var(--color-accent);
    font-weight: var(--font-semibold);
  }
}

// 结果列表
.results-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.result-card {
  display: flex;
  gap: var(--space-5);
  padding: var(--space-5);
  transition: all var(--duration-normal) var(--ease-default);

  &:hover {
    box-shadow: var(--shadow-md);
  }
}

.result-rank {
  flex-shrink: 0;
}

.rank-number {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-light) 100%);
  color: #fff;
  border-radius: 50%;
  font-family: var(--font-mono);
  font-size: var(--text-lg);
  font-weight: var(--font-bold);
}

.result-content {
  flex: 1;
  min-width: 0;
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
}

.score-badge {
  display: flex;
  align-items: baseline;
  flex-shrink: 0;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  font-family: var(--font-mono);

  .score-value {
    font-size: var(--text-xl);
    font-weight: var(--font-bold);
  }

  .score-unit {
    font-size: var(--text-sm);
    margin-left: 2px;
  }

  &.high {
    background-color: #ECFDF5;
    color: var(--color-success);
  }

  &.medium {
    background-color: #FEF3C7;
    color: var(--color-warning);
  }

  &.low {
    background-color: #FEE2E2;
    color: var(--color-danger);
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
}

.result-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border-light);
}

.match-tags {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.match-tag {
  font-size: var(--text-xs);
  font-weight: var(--font-medium);
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);

  &.domain-match {
    background-color: #ECFDF5;
    color: var(--color-success);
  }

  &.cross-domain {
    background-color: #F3F4F6;
    color: var(--color-text-muted);
  }

  &.entity-match {
    background-color: #EFF6FF;
    color: var(--color-accent);
  }
}

// 匹配标签区域
.result-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin: var(--space-3) 0;
}

.match-reason-brief {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  flex: 1;
}

// 详细匹配解释区域
.match-explanation-section {
  margin: var(--space-4) 0;
  border-top: 1px solid var(--color-border-light);
  padding-top: var(--space-3);

  :deep(.el-collapse) {
    border: none;
  }

  :deep(.el-collapse-item__header) {
    border: none;
    background: transparent;
    height: auto;
    padding: var(--space-2) 0;
    font-size: var(--text-sm);
  }

  :deep(.el-collapse-item__wrap) {
    border: none;
    background: transparent;
  }

  :deep(.el-collapse-item__content) {
    padding-bottom: 0;
  }
}

.explanation-toggle {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-accent);
  font-weight: var(--font-medium);
  cursor: pointer;

  svg {
    flex-shrink: 0;
  }
}

.explanation-content {
  background: var(--color-bg-tertiary);
  border-radius: var(--radius-md);
  padding: var(--space-4);
  margin-top: var(--space-2);
}

.explanation-block {
  margin-bottom: var(--space-4);

  &:last-child {
    margin-bottom: 0;
  }

  h4 {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-family: var(--font-heading);
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-2) 0;

    svg {
      color: var(--color-accent);
      flex-shrink: 0;
    }
  }

  p {
    font-size: var(--text-sm);
    line-height: var(--leading-relaxed);
    color: var(--color-text-secondary);
    margin: 0;
  }
}

// 技术相似性进度条
.similarity-bars {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  margin-bottom: var(--space-3);
}

.similarity-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);

  .similarity-label {
    width: 80px;
    font-size: var(--text-xs);
    color: var(--color-text-muted);
    flex-shrink: 0;
  }

  .el-progress {
    flex: 1;
  }
}

.key-difference {
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  background: var(--color-bg-primary);
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--color-warning);

  strong {
    color: var(--color-text-primary);
  }
}

// 实体匹配列表
.entity-match-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.entity-match-item {
  background: var(--color-bg-primary);
  border-radius: var(--radius-sm);
  padding: var(--space-3);
  border: 1px solid var(--color-border-light);
}

.entity-match-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-2);
}

.entity-pair {
  display: flex;
  align-items: center;
  gap: var(--space-2);

  svg {
    color: var(--color-text-muted);
    flex-shrink: 0;
  }
}

.query-entity,
.matched-entity {
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-sm);
  font-weight: var(--font-medium);

  &.entity-product {
    background-color: #EFF6FF;
    color: #1E40AF;
  }

  &.entity-method {
    background-color: #ECFDF5;
    color: #047857;
  }

  &.entity-material {
    background-color: #FEF3C7;
    color: #B45309;
  }

  &.entity-component {
    background-color: #F3F4F6;
    color: #4B5563;
  }

  &.entity-effect {
    background-color: #FEE2E2;
    color: #B91C1C;
  }

  &.entity-application {
    background-color: #F3E8FF;
    color: #7C3AED;
  }
}

.entity-similarity {
  font-family: var(--font-mono);
  font-size: var(--text-sm);
  font-weight: var(--font-bold);
  color: var(--color-accent);
  background: #EFF6FF;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
}

.entity-match-reason {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
}

.entity-type-badge {
  font-size: 10px;
  font-weight: var(--font-medium);
  padding: 2px 6px;
  border-radius: var(--radius-xs);
  background: var(--color-bg-tertiary);
  color: var(--color-text-muted);
  flex-shrink: 0;
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

// 响应式
@media (max-width: 768px) {
  .match-tabs {
    flex-direction: column;
  }

  .form-row {
    grid-template-columns: 1fr;
  }

  .result-card {
    flex-direction: column;
  }

  .result-title-row {
    flex-direction: column;
    gap: var(--space-2);
  }

  .score-badge {
    align-self: flex-start;
  }

  .result-footer {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-3);
  }
}
</style>
