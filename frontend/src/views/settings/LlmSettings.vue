<template>
  <div class="llm-settings-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-title">
        <h2>模型配置</h2>
        <p class="subtitle">
          {{ isAdmin ? '管理员可新增/编辑离线和在线配置，并可修改系统默认配置' : '可新增/编辑自定义离线和在线配置，系统默认配置只读' }}
        </p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openAddDialog()">
          <el-icon><Plus /></el-icon>
          新增配置
        </el-button>
        <el-button @click="loadData" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 当前启用配置概览 Banner -->
    <div class="active-banner" v-if="activeConfig">
      <div class="active-banner-left">
        <el-icon class="banner-check-icon"><CircleCheckFilled /></el-icon>
        <div class="banner-info">
          <div class="banner-title">
            <span class="banner-label">当前启用</span>
            <strong class="banner-name">{{ activeConfig.configName }}</strong>
            <el-tag size="small" :type="activeConfig.llmMode === 'offline' ? 'success' : 'primary'" effect="dark">
              {{ activeConfig.llmMode === 'offline' ? '离线模式' : '在线模式' }}
            </el-tag>
            <el-tag v-if="activeConfig.isSystemConfig" size="small" type="info" effect="plain">系统预设</el-tag>
          </div>
          <div class="banner-models">
            <span v-if="activeConfig.chatModel" class="banner-model-item">
              <span class="banner-model-label">对话</span>
              <code>{{ activeConfig.chatModel }}</code>
            </span>
            <span v-if="activeConfig.llmModel" class="banner-model-item">
              <span class="banner-model-label">分析</span>
              <code>{{ activeConfig.llmModel }}</code>
            </span>
          </div>
        </div>
      </div>
      <div class="banner-status">
        <span class="status-pulse" :class="activeConfig.llmMode === 'offline' ? 'offline' : 'online'"></span>
        <span class="status-text">{{ activeConfig.llmMode === 'offline' ? '离线模式' : '在线模式' }}</span>
        <span class="status-model">{{ activeConfig.chatModel }}</span>
      </div>
    </div>

    <!-- 配置列表 -->
    <div class="config-list" v-loading="loading">
      <el-empty v-if="configs.length === 0 && !loading" description="暂无配置，点击「新增配置」添加" />

      <!-- ===== 系统预设配置分组 ===== -->
      <div v-if="systemConfigs.length > 0" class="section-block system-section">
        <div class="section-header system-section-header">
          <div class="section-header-left">
            <div class="section-icon system-icon">
              <el-icon><Setting /></el-icon>
            </div>
            <div>
              <div class="section-title">系统预设配置</div>
              <div class="section-desc">由管理员内置，所有用户可见，普通用户只读</div>
            </div>
          </div>
          <el-tag type="info" size="small" effect="plain">共 {{ systemConfigs.length }} 项</el-tag>
        </div>

        <el-card shadow="never" class="table-card">
          <el-table :data="systemConfigs" row-key="id" stripe class="config-table">
            <!-- 配置名称（含状态指示） -->
            <el-table-column label="配置名称" min-width="200" header-align="center">
              <template #default="{ row }">
                <div class="name-cell">
                  <el-icon v-if="row.isActive" class="row-active-icon"><CircleCheckFilled /></el-icon>
                  <span v-else class="row-inactive-dot"></span>
                  <div class="name-cell-content">
                    <div class="name-cell-top">
                      <span class="config-name-text" :class="{ 'is-active-name': row.isActive }">{{ row.configName }}</span>
                      <el-tag v-if="row.isActive" size="small" type="warning" effect="dark" class="active-tag">启用中</el-tag>
                    </div>
                    <div v-if="row.remark" class="config-remark-text">{{ row.remark }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <!-- 类型 -->
            <el-table-column label="类型" min-width="108" align="center">
              <template #default="{ row }">
                <el-tag :type="row.llmMode === 'offline' ? 'success' : 'primary'" size="small" effect="plain">
                  {{ row.llmMode === 'offline' ? 'Ollama 离线' : '在线 API' }}
                </el-tag>
              </template>
            </el-table-column>

            <!-- 地址/URL -->
            <el-table-column label="接入地址" min-width="180" header-align="center">
              <template #default="{ row }">
                <span class="url-text">{{ row.llmMode === 'online' ? row.baseUrl : row.ollamaUrl }}</span>
              </template>
            </el-table-column>

            <!-- 模型 -->
            <el-table-column label="模型配置" min-width="220" header-align="center">
              <template #default="{ row }">
                <div class="model-tags-cell">
                  <el-tag v-if="row.chatModel" size="small" effect="plain" type="primary">对话: {{ row.chatModel }}</el-tag>
                  <el-tag v-if="row.llmModel" size="small" effect="plain" type="success">分析: {{ row.llmModel }}</el-tag>
                </div>
              </template>
            </el-table-column>

            <!-- API Key -->
            <el-table-column label="API Key" width="210" header-align="center">
              <template #default="{ row }">
                <template v-if="row.llmMode === 'online'">
                  <div v-if="row.hasApiKey" class="apikey-inline">
                    <!-- 默认显示截断的明文（apiKeyMasked前缀），visible=true时展示完整key可滚动 -->
                    <div class="apikey-scroll-wrap" :class="{ 'is-expanded': getApiKeyState(row.id).visible }">
                      <code class="apikey-code">{{
                        getApiKeyState(row.id).visible
                          ? (getApiKeyState(row.id).value || row.apiKeyMasked || '加载中...')
                          : (row.apiKeyMasked || '••••••••')
                      }}</code>
                    </div>
                    <el-tooltip :content="getApiKeyState(row.id).visible ? '隐藏完整 Key' : '展开查看完整 Key'" placement="top">
                      <el-button link size="small" :loading="getApiKeyState(row.id).loading" @click="toggleApiKeyVisible(row)">
                        <el-icon v-if="getApiKeyState(row.id).visible"><Hide /></el-icon>
                        <el-icon v-else><View /></el-icon>
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="复制完整 Key" placement="top">
                      <el-button link size="small" @click="copyApiKey(row)">
                        <el-icon><CopyDocument /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </div>
                  <el-tag v-else size="small" type="warning" effect="plain">未配置</el-tag>
                </template>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>

            <!-- 操作 -->
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="{ row }">
                <div class="action-btns" @click.stop>
                  <el-button v-if="!row.isActive" size="small" type="primary" link @click="handleActivate(row)">激活</el-button>
                  <template v-if="isAdmin">
                    <el-button size="small" link @click="openEditDialog(row)">编辑</el-button>
                    <el-button size="small" type="danger" link :disabled="row.isActive" @click="handleDelete(row)">删除</el-button>
                  </template>
                  <el-tag v-else size="small" type="info" effect="plain">只读</el-tag>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>

      <!-- ===== 自定义配置分组 ===== -->
      <div class="section-block user-section">
        <div class="section-header user-section-header">
          <div class="section-header-left">
            <div class="section-icon user-icon">
              <el-icon><User /></el-icon>
            </div>
            <div>
              <div class="section-title">我的自定义配置</div>
              <div class="section-desc">仅自己可见，可自由编辑和删除</div>
            </div>
          </div>
          <el-tag type="primary" size="small" effect="plain">共 {{ userConfigs.length }} 项</el-tag>
        </div>

        <el-card shadow="never" class="table-card">
          <el-empty v-if="userConfigs.length === 0 && !loading" description="暂无自定义配置，点击右上角「新增配置」添加" :image-size="80" />
          <el-table v-else :data="userConfigs" row-key="id" stripe class="config-table">
            <!-- 配置名称（含状态指示） -->
            <el-table-column label="配置名称" min-width="200" header-align="center">
              <template #default="{ row }">
                <div class="name-cell">
                  <el-icon v-if="row.isActive" class="row-active-icon"><CircleCheckFilled /></el-icon>
                  <span v-else class="row-inactive-dot"></span>
                  <div class="name-cell-content">
                    <div class="name-cell-top">
                      <span class="config-name-text" :class="{ 'is-active-name': row.isActive }">{{ row.configName }}</span>
                      <el-tag v-if="row.isActive" size="small" type="warning" effect="dark" class="active-tag">启用中</el-tag>
                    </div>
                    <div v-if="row.remark" class="config-remark-text">{{ row.remark }}</div>
                  </div>
                </div>
              </template>
            </el-table-column>

            <!-- 类型 -->
            <el-table-column label="类型" min-width="108" align="center">
              <template #default="{ row }">
                <el-tag :type="row.llmMode === 'offline' ? 'success' : 'primary'" size="small" effect="plain">
                  {{ row.llmMode === 'offline' ? 'Ollama 离线' : '在线 API' }}
                </el-tag>
              </template>
            </el-table-column>

            <!-- 地址/URL -->
            <el-table-column label="接入地址" min-width="180" header-align="center">
              <template #default="{ row }">
                <span class="url-text">{{ row.llmMode === 'online' ? row.baseUrl : row.ollamaUrl }}</span>
              </template>
            </el-table-column>

            <!-- 模型 -->
            <el-table-column label="模型配置" min-width="220" header-align="center">
              <template #default="{ row }">
                <div class="model-tags-cell">
                  <el-tag v-if="row.chatModel" size="small" effect="plain" type="primary">对话: {{ row.chatModel }}</el-tag>
                  <el-tag v-if="row.llmModel" size="small" effect="plain" type="success">分析: {{ row.llmModel }}</el-tag>
                </div>
              </template>
            </el-table-column>

            <!-- API Key -->
            <el-table-column label="API Key" width="210" header-align="center">
              <template #default="{ row }">
                <template v-if="row.llmMode === 'online'">
                  <div v-if="row.hasApiKey" class="apikey-inline">
                    <div class="apikey-scroll-wrap" :class="{ 'is-expanded': getApiKeyState(row.id).visible }">
                      <code class="apikey-code">{{
                        getApiKeyState(row.id).visible
                          ? (getApiKeyState(row.id).value || row.apiKeyMasked || '加载中...')
                          : (row.apiKeyMasked || '••••••••')
                      }}</code>
                    </div>
                    <el-tooltip :content="getApiKeyState(row.id).visible ? '隐藏完整 Key' : '展开查看完整 Key'" placement="top">
                      <el-button link size="small" :loading="getApiKeyState(row.id).loading" @click="toggleApiKeyVisible(row)">
                        <el-icon v-if="getApiKeyState(row.id).visible"><Hide /></el-icon>
                        <el-icon v-else><View /></el-icon>
                      </el-button>
                    </el-tooltip>
                    <el-tooltip content="复制完整 Key" placement="top">
                      <el-button link size="small" @click="copyApiKey(row)">
                        <el-icon><CopyDocument /></el-icon>
                      </el-button>
                    </el-tooltip>
                  </div>
                  <el-tag v-else size="small" type="warning" effect="plain">未配置</el-tag>
                </template>
                <span v-else class="text-muted">-</span>
              </template>
            </el-table-column>

            <!-- 操作 -->
            <el-table-column label="操作" width="160" align="center" fixed="right">
              <template #default="{ row }">
                <div class="action-btns" @click.stop>
                  <el-button v-if="!row.isActive" size="small" type="primary" link @click="handleActivate(row)">激活</el-button>
                  <el-button size="small" link @click="openEditDialog(row)">编辑</el-button>
                  <el-button size="small" type="danger" link :disabled="row.isActive" @click="handleDelete(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </div>

    <!-- 新增/编辑对话框（离线 + 在线双模式） -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="110px"
        label-position="right"
      >
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="form.configName" placeholder="如：本地 DeepSeek、通义千问生产" maxlength="64" show-word-limit />
        </el-form-item>

        <el-form-item label="模式" prop="llmMode">
          <el-radio-group v-model="form.llmMode" :disabled="isEditingSystemConfig && !isAdmin">
            <el-radio-button value="offline">离线 (Ollama)</el-radio-button>
            <el-radio-button value="online">在线 API</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <!-- 管理员新增时可选是否为系统配置 -->
        <el-form-item v-if="isAdmin && !editingConfig" label="配置归属">
          <el-checkbox v-model="form.isSystemConfig">设为系统默认配置（所有用户可见）</el-checkbox>
        </el-form-item>

        <!-- 离线模式字段 -->
        <template v-if="form.llmMode === 'offline'">
          <el-divider content-position="left">Ollama 配置</el-divider>
          <el-form-item label="Ollama 地址">
            <el-input v-model="form.ollamaUrl" placeholder="http://localhost:11434" />
          </el-form-item>
          <el-form-item label="对话模型" prop="chatModel">
            <el-select v-model="form.chatModel" allow-create filterable placeholder="选择或输入模型名" style="width:100%">
              <el-option v-for="m in ollamaChatModels" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item label="分析模型">
            <el-select v-model="form.llmModel" allow-create filterable placeholder="选择或输入模型名" style="width:100%">
              <el-option v-for="m in ollamaLlmModels" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 在线模式字段 -->
        <template v-if="form.llmMode === 'online'">
          <el-divider content-position="left">在线 API 配置</el-divider>
          <el-form-item label="API BaseURL" prop="baseUrl">
            <el-input v-model="form.baseUrl" placeholder="https://dashscope.aliyuncs.com/compatible-mode" />
          </el-form-item>
          <el-form-item label="API Key" prop="apiKey">
            <el-input
              v-model="form.apiKey"
              type="password"
              show-password
              :placeholder="editingConfig && editingConfig.hasApiKey ? '已配置（留空保留原有 Key，输入新值则覆盖）' : '请输入 API Key'"
            >
              <template v-if="editingConfig && editingConfig.hasApiKey && !form.apiKey" #prefix>
                <span style="font-size:12px;color:var(--el-text-color-placeholder);letter-spacing:3px;line-height:1">••••••••</span>
              </template>
            </el-input>
            <div v-if="editingConfig && editingConfig.hasApiKey" style="font-size:12px;color:var(--el-color-warning);margin-top:4px;">
              <el-icon style="vertical-align:-2px"><Warning /></el-icon>
              当前已有 API Key，留空则保留，输入新值则覆盖原有 Key
            </div>
          </el-form-item>
          <el-form-item label="对话模型" prop="chatModel">
            <el-select v-model="form.chatModel" allow-create filterable placeholder="选择或输入模型名" style="width:100%">
              <el-option v-for="m in onlineChatModels" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item label="分析模型">
            <el-select v-model="form.llmModel" allow-create filterable placeholder="选择或输入模型名" style="width:100%">
              <el-option v-for="m in onlineLlmModels" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
        </template>

        <el-divider />
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="可选说明" maxlength="256" />
        </el-form-item>
      </el-form>

      <!-- 连接测试结果 -->
      <div class="test-result" v-if="testResult">
        <el-alert
          :title="testResult.success ? '连接测试成功' : '连接测试失败'"
          :type="testResult.success ? 'success' : 'error'"
          :description="`${testResult.message}（耗时 ${testResult.responseTimeMs}ms）`"
          show-icon
          :closable="false"
        />
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleTest" :loading="testLoading" plain>
            <el-icon><Connection /></el-icon>
            测试连接
          </el-button>
          <div>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleSave" :loading="saveLoading">保存</el-button>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  CircleCheckFilled, Plus, Refresh,
  Connection, Setting, User, InfoFilled, View, Hide, CopyDocument, Warning
} from '@element-plus/icons-vue'
import {
  getSystemStatus, listConfigs, getActiveConfig,
  saveConfig, activateConfig, deleteConfig, testConnection,
  saveSystemConfig, deleteSystemConfig, getPlainApiKey
} from '@/api/llmConfig'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const isAdmin = computed(() => userStore.isAdmin)

// ==================== 状态 ====================
const loading = ref(false)
const saveLoading = ref(false)
const testLoading = ref(false)
const dialogVisible = ref(false)
const editingConfig = ref(null)
const testResult = ref(null)
const formRef = ref(null)

const systemStatus = ref(null)
const activeConfig = ref(null)
const configs = ref([])

// 查看明文 API Key 状态：{ [configId]: { visible: bool, value: string, loading: bool } }
const apiKeyState = reactive({})

const getApiKeyState = (configId) => {
  if (!apiKeyState[configId]) {
    apiKeyState[configId] = { visible: false, value: '', loading: false }
  }
  return apiKeyState[configId]
}

const toggleApiKeyVisible = async (config) => {
  const state = getApiKeyState(config.id)
  if (state.visible) {
    state.visible = false
    return
  }
  if (state.value) {
    state.visible = true
    return
  }
  state.loading = true
  try {
    const res = await getPlainApiKey(config.id)
    state.value = res.data || ''
    state.visible = true
  } catch (e) {
    ElMessage.error('获取 API Key 失败: ' + (e.message || '未知错误'))
  } finally {
    state.loading = false
  }
}

const copyApiKey = async (config) => {
  const state = getApiKeyState(config.id)
  let key = state.value
  if (!key) {
    try {
      const res = await getPlainApiKey(config.id)
      key = res.data || ''
      state.value = key
    } catch (e) {
      ElMessage.error('获取 API Key 失败')
      return
    }
  }
  await navigator.clipboard.writeText(key)
  ElMessage.success('API Key 已复制到剪贴板')
}

// ==================== 计算属性 ====================
const systemConfigs = computed(() => configs.value.filter(c => c.isSystemConfig))
const userConfigs = computed(() => configs.value.filter(c => !c.isSystemConfig))
const isEditingSystemConfig = computed(() => editingConfig.value?.isSystemConfig === true)
const dialogTitle = computed(() => {
  if (!editingConfig.value) return '新增配置'
  return isEditingSystemConfig.value ? '编辑系统默认配置' : '编辑配置'
})

// ==================== 表单 ====================
const defaultForm = {
  id: null,
  configName: '',
  llmMode: 'offline',
  chatModel: '',
  llmModel: '',
  ollamaUrl: 'http://localhost:11434',
  baseUrl: '',
  apiKey: '',
  isSystemConfig: false,
  remark: ''
}

const form = reactive({ ...defaultForm })

const formRules = computed(() => ({
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  chatModel: [{ required: true, message: '请填写对话模型名称', trigger: 'blur' }],
  baseUrl: form.llmMode === 'online'
    ? [{ required: true, message: '在线模式需填写 API BaseURL', trigger: 'blur' }]
    : [],
  // 新增配置时（id 为空）在线模式必须提供 API Key；编辑时留空表示沿用原有 Key
  apiKey: form.llmMode === 'online' && !form.id
    ? [{ required: true, message: '新增在线配置时必须填写 API Key', trigger: 'blur' }]
    : []
}))

// ==================== 常量数据 ====================
const ollamaChatModels = ['deepseek-r1:7b', 'deepseek-r1:14b', 'qwen2.5:7b', 'qwen2.5:14b', 'llama3.2:3b']
const ollamaLlmModels = ['qwen2.5:7b', 'qwen2.5:3b', 'deepseek-r1:7b', 'llama3.2:3b']
const onlineChatModels = ['qwen-max', 'qwen-plus', 'qwen-turbo', 'deepseek-chat', 'deepseek-reasoner', 'gpt-4o', 'gpt-4o-mini']
const onlineLlmModels = ['qwen-plus', 'qwen-turbo', 'deepseek-chat', 'gpt-4o-mini']

// ==================== 方法 ====================
const loadData = async () => {
  loading.value = true
  try {
    const [statusRes, listRes, activeRes] = await Promise.all([
      getSystemStatus(),
      listConfigs(),
      getActiveConfig()
    ])
    systemStatus.value = statusRes.data
    configs.value = listRes.data || []
    activeConfig.value = activeRes.data
  } catch (e) {
    ElMessage.error('加载配置失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const openAddDialog = () => {
  editingConfig.value = null
  testResult.value = null
  Object.assign(form, defaultForm)
  dialogVisible.value = true
}

const openEditDialog = (config) => {
  // 普通用户不能编辑系统配置
  if (config.isSystemConfig && !isAdmin.value) return
  editingConfig.value = config
  testResult.value = null
  Object.assign(form, {
    id: config.id,
    configName: config.configName,
    llmMode: config.llmMode || 'offline',
    chatModel: config.chatModel || '',
    llmModel: config.llmModel || '',
    ollamaUrl: config.ollamaUrl || 'http://localhost:11434',
    baseUrl: config.baseUrl || '',
    apiKey: '',   // 不回填 apiKey，留空表示不修改
    isSystemConfig: config.isSystemConfig || false,
    remark: config.remark || ''
  })
  dialogVisible.value = true
}

const resetForm = () => {
  testResult.value = null
  formRef.value?.resetFields()
}

const handleSave = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saveLoading.value = true
  try {
    // 管理员编辑系统配置走专用接口
    if (isAdmin.value && (form.isSystemConfig || isEditingSystemConfig.value)) {
      await saveSystemConfig({ ...form })
    } else {
      await saveConfig({ ...form })
    }
    ElMessage.success(editingConfig.value ? '配置更新成功' : '配置新增成功')
    dialogVisible.value = false
    await loadData()
  } catch (e) {
    ElMessage.error('保存失败: ' + (e.message || '未知错误'))
  } finally {
    saveLoading.value = false
  }
}

const handleActivate = async (config) => {
  try {
    await activateConfig(config.id)
    ElMessage.success(`已激活配置：${config.configName}`)
    await loadData()
  } catch (e) {
    ElMessage.error('激活失败: ' + (e.message || '未知错误'))
  }
}

const handleDelete = (config) => {
  ElMessageBox.confirm(
    `确定要删除配置「${config.configName}」吗？`,
    '删除确认',
    { type: 'warning', confirmButtonText: '确定删除', cancelButtonText: '取消' }
  ).then(async () => {
    try {
      if (config.isSystemConfig) {
        await deleteSystemConfig(config.id)
      } else {
        await deleteConfig(config.id)
      }
      ElMessage.success('配置已删除')
      await loadData()
    } catch (e) {
      ElMessage.error('删除失败: ' + (e.message || '未知错误'))
    }
  }).catch(() => {})
}

const handleTest = async () => {
  testLoading.value = true
  testResult.value = null
  try {
    const res = await testConnection({ ...form })
    testResult.value = res.data
  } catch (e) {
    testResult.value = { success: false, message: e.message || '请求失败', responseTimeMs: 0 }
  } finally {
    testLoading.value = false
  }
}

// ==================== 初始化 ====================
onMounted(loadData)
</script>

<style lang="scss" scoped>
// ========== 页面容器：全宽 ==========
.llm-settings-page {
  max-width: 1400px;
  margin: 0 auto;
}

// ========== 页面标题 ==========
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-6);

  .header-title {
    h2 {
      font-family: var(--font-heading);
      font-size: var(--text-2xl);
      font-weight: var(--font-bold);
      color: var(--color-text-primary);
      margin-bottom: var(--space-1);
    }
    .subtitle {
      font-size: var(--text-sm);
      color: var(--color-text-muted);
      margin: 0;
    }
  }

  .header-actions {
    display: flex;
    gap: var(--space-3);
    align-items: center;
  }
}

// ========== 当前启用 Banner ==========
.active-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  margin-bottom: var(--space-6);
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.06) 0%, rgba(29, 78, 216, 0.02) 100%);
  border: 1.5px solid var(--color-accent);
  border-radius: var(--radius-lg);
  gap: var(--space-4);

  .active-banner-left {
    display: flex;
    align-items: center;
    gap: var(--space-4);
    flex: 1;
    min-width: 0;

    .banner-check-icon {
      font-size: 28px;
      color: #67c23a;
      flex-shrink: 0;
    }

    .banner-info {
      min-width: 0;

      .banner-title {
        display: flex;
        align-items: center;
        gap: var(--space-2);
        flex-wrap: wrap;
        margin-bottom: var(--space-2);

        .banner-label {
          font-size: var(--text-sm);
          color: var(--color-text-muted);
        }

        .banner-name {
          font-size: var(--text-base);
          font-weight: var(--font-bold);
          color: var(--color-text-primary);
        }
      }

      .banner-models {
        display: flex;
        gap: var(--space-4);
        flex-wrap: wrap;

        .banner-model-item {
          display: flex;
          align-items: center;
          gap: var(--space-1);

          .banner-model-label {
            font-size: var(--text-xs);
            color: var(--color-text-muted);
          }

          code {
            font-size: var(--text-xs);
            background: rgba(29, 78, 216, 0.08);
            color: var(--color-accent);
            padding: 1px 6px;
            border-radius: 4px;
            font-family: monospace;
          }
        }
      }
    }
  }

  .banner-status {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-shrink: 0;

    .status-pulse {
      display: inline-block;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      animation: pulse 2s infinite;

      &.offline { background: #67c23a; box-shadow: 0 0 0 3px rgba(103, 194, 58, 0.2); }
      &.online  { background: #409eff; box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.2); }
    }

    .status-text {
      font-size: var(--text-sm);
      color: var(--color-text-secondary);
      font-weight: var(--font-medium);
    }

    .status-model {
      font-size: var(--text-sm);
      color: var(--color-text-muted);
      font-family: monospace;
    }
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

// ========== 配置列表容器 ==========
.config-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

// ========== 分组区块 ==========
.section-block {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-1);

  .section-header-left {
    display: flex;
    align-items: center;
    gap: var(--space-3);
  }

  .section-icon {
    width: 40px;
    height: 40px;
    border-radius: var(--radius-md);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    flex-shrink: 0;
  }

  .section-title {
    font-size: var(--text-base);
    font-weight: var(--font-bold);
    color: var(--color-text-primary);
    margin-bottom: 2px;
  }

  .section-desc {
    font-size: var(--text-xs);
    color: var(--color-text-muted);
  }
}

// 系统预设分组醒目样式
.system-section-header {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08) 0%, rgba(245, 158, 11, 0.03) 100%);
  border: 1px solid rgba(245, 158, 11, 0.3);

  .section-icon.system-icon {
    background: rgba(245, 158, 11, 0.15);
    color: #f59e0b;
  }

  .section-title {
    color: #92400e;
  }
}

// 自定义配置分组样式
.user-section-header {
  background: linear-gradient(135deg, rgba(29, 78, 216, 0.06) 0%, rgba(29, 78, 216, 0.02) 100%);
  border: 1px solid rgba(29, 78, 216, 0.2);

  .section-icon.user-icon {
    background: rgba(29, 78, 216, 0.1);
    color: var(--color-accent);
  }

  .section-title {
    color: var(--color-accent);
  }
}

// ========== 表格卡片 ==========
.table-card {
  :deep(.el-card__body) { padding: 0; }
}

.config-table {
  width: 100%;

  :deep(.el-table__row:hover) td {
    background: var(--color-bg-secondary);
  }

  :deep(.el-table__header-wrapper) th {
    background: var(--color-bg-secondary);
    color: var(--color-text-secondary);
    font-size: var(--text-xs);
    font-weight: var(--font-semibold);
    text-transform: uppercase;
    letter-spacing: 0.04em;
  }
}

// ========== 操作按钮（link 风格，行内排列） ==========
.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-1);
  flex-wrap: nowrap;
}

// ========== 表格单元格样式 ==========
.name-cell {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex-wrap: nowrap;

  .row-active-icon {
    color: #67c23a;
    font-size: 16px;
    flex-shrink: 0;
  }

  .row-inactive-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background: var(--color-border-dark);
    flex-shrink: 0;
  }

  .config-name-text {
    font-size: var(--text-sm);
    font-weight: var(--font-medium);
    color: var(--color-text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;

    &.is-active-name {
      font-weight: var(--font-bold);
      color: var(--color-accent);
    }
  }

  .active-tag {
    flex-shrink: 0;
  }
}

.name-cell-content {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;

  .name-cell-top {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    flex-wrap: nowrap;
  }
}

.config-remark-text {
  font-size: 11px;
  color: var(--color-text-muted);
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 220px;
  opacity: 0.8;
}

.url-text {
  font-family: monospace;
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
  word-break: break-all;
}

.model-tags-cell {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
}

// API Key 内联样式（深色背景 code 块 + 可滚动展开）
.apikey-inline {
  display: flex;
  align-items: center;
  gap: 2px;
  min-width: 0;

  // 外层滚动容器
  .apikey-scroll-wrap {
    flex: 1;
    min-width: 0;
    max-width: 130px;
    overflow: hidden;
    border-radius: 4px;
    background: var(--color-bg-secondary);
    border: 1px solid var(--color-border);

    // 默认：截断不滚动
    &:not(.is-expanded) {
      .apikey-code {
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }

    // 展开后：可横向滚动查看完整 key
    &.is-expanded {
      overflow-x: auto;
      max-width: 130px;
      scrollbar-width: thin;
      scrollbar-color: var(--color-border-dark) transparent;

      &::-webkit-scrollbar {
        height: 3px;
      }
      &::-webkit-scrollbar-thumb {
        background: var(--color-border-dark);
        border-radius: 2px;
      }

      .apikey-code {
        white-space: nowrap;
      }
    }
  }

  .apikey-code {
    display: block;
    font-family: monospace;
    font-size: 12px;
    color: var(--color-text-primary);
    padding: 2px 6px;
    letter-spacing: 0.03em;
    line-height: 1.5;
    cursor: default;
    user-select: text;
  }

  .el-button {
    flex-shrink: 0;
    padding: 2px 3px;
    color: var(--color-text-muted);

    &:hover {
      color: var(--color-accent);
    }
  }
}

.text-muted {
  color: var(--color-text-muted);
  font-size: var(--text-sm);
}

.action-btns {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 4px;
  flex-wrap: nowrap;

  // 让删除按钮不换行
  .el-button {
    margin: 0 !important;
    padding: 5px 8px;
  }
}

// ========== 对话框 ==========
.test-result {
  margin-top: var(--space-4);
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
