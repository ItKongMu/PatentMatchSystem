<template>
  <div class="llm-settings">
    <!-- 页面标题 + 系统状态 -->
    <div class="page-header">
      <div class="header-title">
        <h2>模型配置</h2>
        <p class="subtitle">
          {{ isAdmin ? '管理员可新增/编辑离线和在线配置，并可修改系统默认配置' : '可新增/编辑自定义离线和在线配置，系统默认配置只读' }}
        </p>
      </div>
      <div class="system-status" v-if="systemStatus">
        <el-tag
          :type="systemStatus.currentMode === 'offline' ? 'success' : 'primary'"
          size="large"
          effect="light"
        >
          <span class="status-dot" :class="systemStatus.currentMode"></span>
          {{ systemStatus.currentMode === 'offline' ? '离线模式' : '在线模式' }}
        </el-tag>
        <span class="status-model">{{ systemStatus.chatModel }}</span>
      </div>
    </div>

    <!-- 当前启用配置概览 -->
    <el-card class="active-config-card" v-if="activeConfig" shadow="never">
      <div class="active-config-header">
        <el-icon class="active-icon"><CircleCheckFilled /></el-icon>
        <span class="active-label">当前启用</span>
        <strong>{{ activeConfig.configName }}</strong>
        <el-tag size="small" :type="activeConfig.llmMode === 'offline' ? 'success' : 'primary'">
          {{ activeConfig.llmMode === 'offline' ? 'Ollama 离线' : '在线 API' }}
        </el-tag>
        <el-tag v-if="activeConfig.isSystemConfig" size="small" type="info" effect="plain">系统预设</el-tag>
      </div>
      <div class="active-config-models">
        <div class="model-item" v-if="activeConfig.chatModel">
          <span class="model-label">对话模型</span>
          <el-tag size="small" effect="plain">{{ activeConfig.chatModel }}</el-tag>
        </div>
        <div class="model-item" v-if="activeConfig.llmModel">
          <span class="model-label">分析模型</span>
          <el-tag size="small" effect="plain">{{ activeConfig.llmModel }}</el-tag>
        </div>
        <div class="model-item" v-if="activeConfig.embedModel">
          <span class="model-label">向量模型</span>
          <el-tag size="small" effect="plain">{{ activeConfig.embedModel }}</el-tag>
        </div>
      </div>
    </el-card>

    <!-- 操作栏 -->
    <div class="toolbar">
      <el-button type="primary" @click="openAddDialog()">
        <el-icon><Plus /></el-icon>
        新增配置
      </el-button>
      <el-button @click="loadData">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 配置列表 -->
    <div class="config-list" v-loading="loading">
      <el-empty v-if="configs.length === 0 && !loading" description="暂无配置，点击「新增 Ollama 离线配置」添加" />

      <!-- 系统预设配置分组 -->
      <div v-if="systemConfigs.length > 0" class="config-group">
        <div class="group-label">
          <el-icon><Setting /></el-icon>
          系统预设配置
        </div>
        <div v-for="config in systemConfigs" :key="config.id" class="config-card">
          <el-card shadow="hover" :class="{ 'is-active': config.isActive }">
            <div class="config-card-header">
              <div class="config-name-row">
                <el-icon v-if="config.isActive" class="active-badge"><CircleCheckFilled /></el-icon>
                <span class="config-name">{{ config.configName }}</span>
                <el-tag size="small" :type="config.llmMode === 'offline' ? 'success' : 'primary'">
                  {{ config.llmMode === 'offline' ? 'Ollama' : '在线 API' }}
                </el-tag>
                <el-tag size="small" type="info" effect="plain">系统预设</el-tag>
                <el-tag v-if="config.isActive" size="small" type="warning" effect="dark">启用中</el-tag>
              </div>
              <div class="config-actions">
                <el-button
                  v-if="!config.isActive"
                  size="small"
                  type="primary"
                  plain
                  @click="handleActivate(config)"
                >激活</el-button>
                <template v-if="isAdmin">
                  <el-button size="small" @click="openEditDialog(config)">编辑</el-button>
                  <el-button size="small" type="danger" plain :disabled="config.isActive" @click="handleDelete(config)">删除</el-button>
                </template>
                <el-tag v-else size="small" type="info" effect="plain" class="readonly-tag">只读</el-tag>
              </div>
            </div>

            <div class="config-details">
              <div class="detail-row" v-if="config.llmMode === 'online' && config.baseUrl">
                <span class="detail-label">API BaseURL</span>
                <span class="detail-value url">{{ config.baseUrl }}</span>
              </div>
              <div class="detail-row" v-if="config.llmMode === 'online'">
                <span class="detail-label">API Key</span>
                <span class="detail-value">
                  <el-tag v-if="config.hasApiKey" size="small" type="success" effect="plain">已配置</el-tag>
                  <el-tag v-else size="small" type="warning" effect="plain">未配置（需管理员设置）</el-tag>
                </span>
              </div>
              <div class="detail-row" v-if="config.llmMode === 'offline' && config.ollamaUrl">
                <span class="detail-label">Ollama 地址</span>
                <span class="detail-value url">{{ config.ollamaUrl }}</span>
              </div>
              <div class="detail-row models-row">
                <span class="detail-label">模型配置</span>
                <div class="models-tags">
                  <el-tag v-if="config.chatModel" size="small" effect="plain">对话: {{ config.chatModel }}</el-tag>
                  <el-tag v-if="config.llmModel" size="small" effect="plain">分析: {{ config.llmModel }}</el-tag>
                  <el-tag v-if="config.embedModel" size="small" effect="plain">向量: {{ config.embedModel }}</el-tag>
                </div>
              </div>
              <div class="detail-row" v-if="config.remark">
                <span class="detail-label">备注</span>
                <span class="detail-value muted">{{ config.remark }}</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 用户自定义配置分组 -->
      <div v-if="userConfigs.length > 0" class="config-group">
        <div class="group-label">
          <el-icon><User /></el-icon>
          我的自定义配置
        </div>
        <div v-for="config in userConfigs" :key="config.id" class="config-card">
          <el-card shadow="hover" :class="{ 'is-active': config.isActive }">
            <div class="config-card-header">
              <div class="config-name-row">
                <el-icon v-if="config.isActive" class="active-badge"><CircleCheckFilled /></el-icon>
                <span class="config-name">{{ config.configName }}</span>
                <el-tag size="small" :type="config.llmMode === 'offline' ? 'success' : 'primary'">
                  {{ config.llmMode === 'offline' ? 'Ollama' : '在线 API' }}
                </el-tag>
                <el-tag v-if="config.isActive" size="small" type="warning" effect="dark">启用中</el-tag>
              </div>
              <div class="config-actions">
                <el-button
                  v-if="!config.isActive"
                  size="small"
                  type="primary"
                  plain
                  @click="handleActivate(config)"
                >激活</el-button>
                <el-button size="small" @click="openEditDialog(config)">编辑</el-button>
                <el-button size="small" type="danger" plain
                  @click="handleDelete(config)"
                  :disabled="config.isActive"
                >删除</el-button>
              </div>
            </div>

            <div class="config-details">
              <div class="detail-row" v-if="config.llmMode === 'online' && config.baseUrl">
                <span class="detail-label">API BaseURL</span>
                <span class="detail-value url">{{ config.baseUrl }}</span>
              </div>
              <div class="detail-row" v-if="config.llmMode === 'online'">
                <span class="detail-label">API Key</span>
                <span class="detail-value">
                  <el-tag v-if="config.hasApiKey" size="small" type="success" effect="plain">已配置</el-tag>
                  <el-tag v-else size="small" type="warning" effect="plain">未配置</el-tag>
                </span>
              </div>
              <div class="detail-row" v-if="config.llmMode === 'offline' && config.ollamaUrl">
                <span class="detail-label">Ollama 地址</span>
                <span class="detail-value url">{{ config.ollamaUrl }}</span>
              </div>
              <div class="detail-row models-row">
                <span class="detail-label">模型配置</span>
                <div class="models-tags">
                  <el-tag v-if="config.chatModel" size="small" effect="plain">对话: {{ config.chatModel }}</el-tag>
                  <el-tag v-if="config.llmModel" size="small" effect="plain">分析: {{ config.llmModel }}</el-tag>
                  <el-tag v-if="config.embedModel" size="small" effect="plain">向量: {{ config.embedModel }}</el-tag>
                </div>
              </div>
              <div class="detail-row" v-if="config.remark">
                <span class="detail-label">备注</span>
                <span class="detail-value muted">{{ config.remark }}</span>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 无用户配置时提示 -->
      <div v-if="userConfigs.length === 0 && !loading && systemConfigs.length > 0" class="user-empty-tip">
        <el-text type="info">
          <el-icon><InfoFilled /></el-icon>
          暂无自定义配置，点击「新增配置」添加离线或在线模型配置
        </el-text>
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
          <el-form-item label="向量模型">
            <el-select v-model="form.embedModel" allow-create filterable placeholder="选择或输入模型名" style="width:100%">
              <el-option v-for="m in ollamaEmbedModels" :key="m" :label="m" :value="m" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 在线模式字段 -->
        <template v-if="form.llmMode === 'online'">
          <el-divider content-position="left">在线 API 配置</el-divider>
          <el-form-item label="API BaseURL" prop="baseUrl">
            <el-input v-model="form.baseUrl" placeholder="https://dashscope.aliyuncs.com/compatible-mode" />
          </el-form-item>
          <el-form-item label="API Key">
            <el-input v-model="form.apiKey" type="password" show-password
              :placeholder="editingConfig ? '留空则保留原有 Key' : '请输入 API Key'" />
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
          <el-form-item label="向量模型">
            <el-select v-model="form.embedModel" allow-create filterable placeholder="选择或输入模型名" style="width:100%">
              <el-option v-for="m in onlineEmbedModels" :key="m" :label="m" :value="m" />
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
  Connection, Setting, User, InfoFilled
} from '@element-plus/icons-vue'
import {
  getSystemStatus, listConfigs, getActiveConfig,
  saveConfig, activateConfig, deleteConfig, testConnection,
  saveSystemConfig, deleteSystemConfig
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
  embedModel: '',
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
    : []
}))

// ==================== 常量数据 ====================
const ollamaChatModels = ['deepseek-r1:7b', 'deepseek-r1:14b', 'qwen2.5:7b', 'qwen2.5:14b', 'llama3.2:3b']
const ollamaLlmModels = ['qwen2.5:7b', 'qwen2.5:3b', 'deepseek-r1:7b', 'llama3.2:3b']
const ollamaEmbedModels = ['bge-m3', 'nomic-embed-text', 'mxbai-embed-large']
const onlineChatModels = ['qwen-max', 'qwen-plus', 'qwen-turbo', 'deepseek-chat', 'deepseek-reasoner', 'gpt-4o', 'gpt-4o-mini']
const onlineLlmModels = ['qwen-plus', 'qwen-turbo', 'deepseek-chat', 'gpt-4o-mini']
const onlineEmbedModels = ['text-embedding-v3', 'text-embedding-v2', 'text-embedding-ada-002']

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
    embedModel: config.embedModel || '',
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
.llm-settings {
  max-width: 900px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-6);

  .header-title h2 {
    font-size: var(--text-xl);
    font-weight: var(--font-bold);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-1) 0;
  }

  .subtitle {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
    margin: 0;
  }
}

.system-status {
  display: flex;
  align-items: center;
  gap: var(--space-3);

  .status-dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    margin-right: var(--space-1);
    background: currentColor;
    animation: pulse 2s infinite;

    &.offline { background: #67c23a; }
    &.online { background: #409eff; }
  }

  .status-model {
    font-size: var(--text-sm);
    color: var(--color-text-secondary);
    font-family: monospace;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.active-config-card {
  margin-bottom: var(--space-4);
  border: 1px solid var(--color-accent);
  background: rgba(29, 78, 216, 0.03);

  .active-config-header {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    margin-bottom: var(--space-3);

    .active-icon { color: #67c23a; font-size: 18px; }
    .active-label { color: var(--color-text-muted); font-size: var(--text-sm); }
  }

  .active-config-models {
    display: flex;
    gap: var(--space-4);
    flex-wrap: wrap;

    .model-item {
      display: flex;
      align-items: center;
      gap: var(--space-2);

      .model-label {
        font-size: var(--text-xs);
        color: var(--color-text-muted);
      }
    }
  }
}

.toolbar {
  margin-bottom: var(--space-4);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  align-items: center;

  .toolbar-tip {
    flex: 1;
    min-width: 300px;
  }
}

.config-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.config-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);

  .group-label {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-sm);
    font-weight: var(--font-semibold);
    color: var(--color-text-secondary);
    padding-bottom: var(--space-1);
    border-bottom: 1px solid var(--color-border);
  }
}

.config-card {
  :deep(.el-card) {
    transition: all var(--duration-normal) var(--ease-default);
    border: 1px solid var(--color-border);

    &.is-active {
      border-color: var(--color-accent);
    }

    &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.08); }
  }
}

.config-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-3);

  .config-name-row {
    display: flex;
    align-items: center;
    gap: var(--space-2);

    .active-badge { color: #67c23a; font-size: 16px; }
    .config-name { font-weight: var(--font-semibold); font-size: var(--text-base); }
  }

  .config-actions {
    display: flex;
    align-items: center;
    gap: var(--space-2);

    .readonly-tag {
      font-size: var(--text-xs);
    }
  }
}

.config-details {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-top: var(--space-2);
  border-top: 1px solid var(--color-border);
}

.detail-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-sm);

  .detail-label {
    width: 80px;
    color: var(--color-text-muted);
    flex-shrink: 0;
    font-size: var(--text-xs);
  }

  .detail-value {
    color: var(--color-text-primary);

    &.url { font-family: monospace; font-size: var(--text-xs); color: var(--color-text-secondary); }
    &.muted { color: var(--color-text-muted); }
  }

  &.models-row { align-items: flex-start; }

  .models-tags { display: flex; flex-wrap: wrap; gap: var(--space-1); }
}

.user-empty-tip {
  padding: var(--space-4);
  text-align: center;
  border: 1px dashed var(--color-border);
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
}

.test-result {
  margin-top: var(--space-4);
}

.dialog-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
