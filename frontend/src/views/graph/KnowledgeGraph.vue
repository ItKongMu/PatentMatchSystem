<template>
  <div class="page-container graph-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">知识图谱</h1>
        <p class="page-desc">可视化展示专利实体关系网络，支持按专利、实体、IPC分类查询</p>
      </div>
    </div>

    <!-- 查询控制面板 -->
    <div class="control-panel">
      <!-- 查询模式切换 -->
      <div class="mode-tabs">
        <button
          v-for="mode in queryModes"
          :key="mode.value"
          class="mode-tab"
          :class="{ active: queryMode === mode.value }"
          @click="switchMode(mode.value)"
        >
          <span class="mode-icon" v-html="mode.icon"></span>
          {{ mode.label }}
        </button>
      </div>

      <!-- 查询输入区 -->
      <div class="query-form">
        <!-- 专利查询 -->
        <template v-if="queryMode === 'patent'">
          <el-input
            v-model="queryInput"
            placeholder="输入专利公开号，如 CN201910123456A"
            clearable
            class="query-input"
            @keyup.enter="handleQuery"
          />
        </template>

        <!-- 实体查询 -->
        <template v-else-if="queryMode === 'entity'">
          <el-input
            v-model="queryInput"
            placeholder="输入实体名称，如 图像传感器"
            clearable
            class="query-input"
            @keyup.enter="handleQuery"
          />
        </template>

        <!-- IPC查询 -->
        <template v-else-if="queryMode === 'ipc'">
          <el-input
            v-model="queryInput"
            placeholder="输入IPC分类号，如 G06F16/30"
            clearable
            class="query-input"
            @keyup.enter="handleQuery"
          />
        </template>

        <!-- 通用查询 -->
        <template v-else>
          <el-select v-model="advancedForm.nodeType" placeholder="节点类型（必选）" class="query-select">
            <el-option label="专利 (Patent)" value="Patent" />
            <el-option label="实体 (Entity)" value="Entity" />
            <el-option label="IPC分类 (IPC)" value="IPC" />
            <el-option label="申请人 (Applicant)" value="Applicant" />
            <el-option label="概念 (Concept)" value="Concept" />
          </el-select>
          <el-input
            v-model="advancedForm.keyword"
            :placeholder="advancedKeywordPlaceholder"
            clearable
            class="query-input"
            @keyup.enter="handleQuery"
          />
          <el-select v-model="advancedForm.depth" placeholder="查询深度" class="query-select-sm">
            <el-option label="1跳" :value="1" />
            <el-option label="2跳" :value="2" />
            <el-option label="3跳" :value="3" />
          </el-select>
        </template>

        <el-button type="primary" :loading="loading" @click="handleQuery">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right:4px">
            <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          查询
        </el-button>
        <el-button @click="resetGraph">重置</el-button>
      </div>
    </div>

    <!-- 图谱主区域 -->
    <div class="graph-workspace">
      <!-- 图谱画布 -->
      <div ref="wrapRef" class="graph-canvas-wrap">
        <!-- 空状态 -->
        <div v-if="!hasData && !loading" class="graph-empty">
          <svg width="72" height="72" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1" opacity="0.3">
            <circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/>
            <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
            <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
          </svg>
          <p>请输入查询条件，探索专利知识图谱</p>
          <div class="empty-hints">
            <span class="hint-tag" @click="quickQuery('patent', 'CN201910123456A')">示例：专利图谱</span>
            <span class="hint-tag" @click="quickQuery('entity', '图像传感器')">示例：实体关系</span>
            <span class="hint-tag" @click="quickQuery('ipc', 'G06F16')">示例：IPC层次</span>
          </div>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="graph-loading">
          <div class="loading-spinner"></div>
          <p>正在查询图谱数据...</p>
        </div>

        <!-- ECharts 图谱 -->
        <div
          v-show="hasData && !loading"
          ref="chartRef"
          class="graph-canvas"
        ></div>

        <!-- 图谱工具栏 -->
        <div v-if="hasData && !loading" class="graph-toolbar">
          <el-tooltip content="放大" placement="left">
            <button class="toolbar-btn" @click="zoomIn">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                <line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/>
              </svg>
            </button>
          </el-tooltip>
          <el-tooltip content="缩小" placement="left">
            <button class="toolbar-btn" @click="zoomOut">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
                <line x1="8" y1="11" x2="14" y2="11"/>
              </svg>
            </button>
          </el-tooltip>
          <el-tooltip content="重置视图" placement="left">
            <button class="toolbar-btn" @click="resetZoom">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"/>
                <path d="M3 3v5h5"/>
              </svg>
            </button>
          </el-tooltip>
        </div>
      </div>

      <!-- 右侧信息面板 -->
      <div class="graph-info-panel" :class="{ 'has-selection': selectedNode }">
        <!-- 图例 -->
        <div class="panel-section">
          <h3 class="panel-title">节点图例</h3>
          <div class="legend-list">
            <div v-for="item in nodeLegend" :key="item.type" class="legend-item">
              <span class="legend-dot" :style="{ background: item.color }"></span>
              <span class="legend-label">{{ item.label }}</span>
            </div>
          </div>
        </div>

        <!-- 统计信息 -->
        <div v-if="hasData" class="panel-section">
          <h3 class="panel-title">图谱统计</h3>
          <div class="stats-list">
            <div class="stat-row">
              <span class="stat-label">节点数</span>
              <span class="stat-value">{{ graphData.nodes.length }}</span>
            </div>
            <div class="stat-row">
              <span class="stat-label">关系数</span>
              <span class="stat-value">{{ graphData.links.length }}</span>
            </div>
          </div>
        </div>

        <!-- 选中节点详情 -->
        <div v-if="selectedNode" class="panel-section node-detail">
          <h3 class="panel-title">节点详情</h3>
          <div class="node-info">
            <div class="node-type-badge" :style="{ background: getNodeColor(selectedNode.label) }">
              {{ selectedNode.label }}
            </div>
            <div class="node-name">{{ selectedNode.name }}</div>
            <div v-if="selectedNode.label === 'Patent'" class="node-actions">
              <el-button size="small" type="primary" @click="viewPatentDetail(selectedNode)">
                查看专利详情
              </el-button>
              <el-button size="small" @click="expandNode(selectedNode)">
                展开关联
              </el-button>
            </div>
            <div v-else class="node-actions">
              <el-button size="small" @click="expandNode(selectedNode)">
                展开关联
              </el-button>
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onActivated, onBeforeUnmount, nextTick } from 'vue'

// keep-alive 缓存需要组件 name 与 cachedViews 中的名称一致
defineOptions({ name: 'KnowledgeGraph' })
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts/core'
import { GraphChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { graphApi } from '@/api/graph'
import { patentApi } from '@/api/patent'

// 按需注册 ECharts 组件
echarts.use([GraphChart, TooltipComponent, LegendComponent, CanvasRenderer])

const route = useRoute()
const router = useRouter()

// ---- 状态 ----
const loading = ref(false)
const queryMode = ref('patent')
const queryInput = ref('')
const advancedForm = ref({ nodeType: 'Patent', keyword: '', depth: 2 })
const selectedNode = ref(null)
const chartRef = ref(null)   // ECharts canvas div
const wrapRef = ref(null)    // graph-canvas-wrap div（用于 ResizeObserver）
let chartInstance = null
let resizeObserver = null

const graphData = ref({ nodes: [], links: [] })
const hasData = computed(() => graphData.value.nodes.length > 0)

// 通用查询关键词输入框占位符：有节点类型时可不填关键词
const advancedKeywordPlaceholder = computed(() => {
  if (advancedForm.value.nodeType) {
    return `关键词（不填则浏览前25个${advancedForm.value.nodeType}节点）`
  }
  return '关键词'
})

// ---- 节点颜色配置 ----
const NODE_COLORS = {
  Patent:    '#3B82F6',
  Entity:    '#10B981',
  IPC:       '#F59E0B',
  Applicant: '#8B5CF6',
  Concept:   '#EC4899',
  Method:    '#06B6D4',
  Component: '#6B7280',
  Material:  '#EF4444',
  Application: '#F97316'
}

const nodeLegend = [
  { type: 'Patent',    label: '专利',   color: NODE_COLORS.Patent },
  { type: 'Entity',    label: '实体',   color: NODE_COLORS.Entity },
  { type: 'IPC',       label: 'IPC分类', color: NODE_COLORS.IPC },
  { type: 'Applicant', label: '申请人', color: NODE_COLORS.Applicant },
  { type: 'Concept',   label: '概念',   color: NODE_COLORS.Concept },
  { type: 'Method',    label: '方法',   color: NODE_COLORS.Method },
  { type: 'Component', label: '组件',   color: NODE_COLORS.Component },
  { type: 'Material',  label: '材料',   color: NODE_COLORS.Material }
]

const queryModes = [
  {
    value: 'patent',
    label: '专利图谱',
    icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>'
  },
  {
    value: 'entity',
    label: '实体关系',
    icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/></svg>'
  },
  {
    value: 'ipc',
    label: 'IPC层次',
    icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="8" y1="6" x2="21" y2="6"/><line x1="8" y1="12" x2="21" y2="12"/><line x1="8" y1="18" x2="21" y2="18"/><line x1="3" y1="6" x2="3.01" y2="6"/><line x1="3" y1="12" x2="3.01" y2="12"/><line x1="3" y1="18" x2="3.01" y2="18"/></svg>'
  },
  {
    value: 'advanced',
    label: '通用查询',
    icon: '<svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>'
  }
]

// ---- 工具函数 ----
const getNodeColor = (label) => NODE_COLORS[label] || '#94A3B8'

const getNodeSize = (label) => {
  const sizeMap = { Patent: 28, IPC: 22, Applicant: 20, Concept: 18, Entity: 16 }
  return sizeMap[label] || 14
}

// ---- 查询逻辑 ----
const switchMode = (mode) => {
  queryMode.value = mode
  queryInput.value = ''
  // 切换到通用查询时，默认选中 Patent 类型，方便用户直接点查询
  advancedForm.value = { nodeType: mode === 'advanced' ? 'Patent' : '', keyword: '', depth: 2 }
}

const quickQuery = (mode, input) => {
  queryMode.value = mode
  queryInput.value = input
  handleQuery()
}

const handleQuery = async () => {
  const mode = queryMode.value

  if (mode !== 'advanced' && !queryInput.value.trim()) {
    ElMessage.warning('请输入查询内容')
    return
  }
  if (mode === 'advanced' && !advancedForm.value.nodeType && !advancedForm.value.keyword) {
    ElMessage.warning('请至少选择一个节点类型')
    return
  }

  loading.value = true
  selectedNode.value = null

  try {
    let res
    if (mode === 'patent') {
      res = await graphApi.getPatentGraph(queryInput.value.trim())
    } else if (mode === 'entity') {
      res = await graphApi.getEntityGraph(queryInput.value.trim())
    } else if (mode === 'ipc') {
      res = await graphApi.getIpcGraph(queryInput.value.trim())
    } else {
      res = await graphApi.queryGraph({
        nodeType: advancedForm.value.nodeType || null,
        keyword: advancedForm.value.keyword.trim() || null,
        depth: advancedForm.value.depth,
        limit: advancedForm.value.keyword.trim() ? 50 : 25
      })
    }

    if (res && res.data) {
      graphData.value = res.data
      await nextTick()
      renderChart()
    } else {
      ElMessage.info('未查询到相关图谱数据')
      graphData.value = { nodes: [], links: [] }
    }
  } catch (err) {
    console.error('图谱查询失败:', err)
    graphData.value = { nodes: [], links: [] }
  } finally {
    loading.value = false
  }
}

const expandNode = async (node) => {
  if (!node) return
  loading.value = true
  try {
    let res
    if (node.label === 'Patent') {
      res = await graphApi.getPatentGraph(node.name)
    } else if (node.label === 'Entity') {
      res = await graphApi.getEntityGraph(node.name)
    } else if (node.label === 'IPC') {
      // node.id 格式为 "IPC:A61K9/70"，提取冒号后的 ipcCode
      const ipcCode = node.id ? node.id.replace(/^IPC:/, '') : node.name
      res = await graphApi.getIpcGraph(ipcCode)
    } else {
      res = await graphApi.queryGraph({ nodeType: node.label, nodeKey: node.name, depth: 2 })
    }

    if (res && res.data) {
      // 合并节点和关系（去重）
      const existingIds = new Set(graphData.value.nodes.map(n => n.id))
      const existingLinks = new Set(
        graphData.value.links.map(l => `${l.source}-${l.target}-${l.type}`)
      )
      const newNodes = res.data.nodes.filter(n => !existingIds.has(n.id))
      const newLinks = res.data.links.filter(
        l => !existingLinks.has(`${l.source}-${l.target}-${l.type}`)
      )
      graphData.value = {
        nodes: [...graphData.value.nodes, ...newNodes],
        links: [...graphData.value.links, ...newLinks]
      }
      await nextTick()
      renderChart()
      ElMessage.success(`已展开 ${newNodes.length} 个新节点`)
    }
  } catch (err) {
    console.error('展开节点失败:', err)
  } finally {
    loading.value = false
  }
}

const resetGraph = () => {
  queryInput.value = ''
  advancedForm.value = { nodeType: 'Patent', keyword: '', depth: 2 }
  selectedNode.value = null
  graphData.value = { nodes: [], links: [] }
  if (chartInstance) {
    chartInstance.clear()
  }
}

// ---- ECharts 渲染 ----
const buildChartOption = () => {
  const nodes = graphData.value.nodes.map(node => ({
    id: node.id,
    name: node.name,
    // 用 nodeType 保存节点类型，避免与 ECharts label 配置冲突
    nodeType: node.label,
    symbolSize: getNodeSize(node.label),
    itemStyle: { color: getNodeColor(node.label) },
    label: {
      show: true,
      position: 'right',
      fontSize: 11,
      color: '#374151',
      formatter: (params) => {
        const name = params.data.name || ''
        return name.length > 12 ? name.substring(0, 12) + '…' : name
      }
    },
    // 保留原始数据供点击使用
    _raw: node
  }))

  const links = graphData.value.links.map(link => ({
    source: link.source,
    target: link.target,
    label: {
      show: true,
      formatter: link.type || '',
      fontSize: 10,
      color: '#9CA3AF'
    },
    lineStyle: {
      color: '#CBD5E1',
      width: 1.5,
      curveness: 0.1
    }
  }))

  return {
    tooltip: {
      trigger: 'item',
      formatter: (params) => {
        if (params.dataType === 'node') {
          return `<div style="font-size:13px">
            <strong>${params.data.name}</strong><br/>
            <span style="color:#9CA3AF">类型: ${params.data.nodeType || ''}</span>
          </div>`
        }
        if (params.dataType === 'edge') {
          // edge label 是对象，取 formatter 字段作为关系类型文本
          const relType = typeof params.data.label === 'object'
            ? (params.data.label.formatter || '')
            : (params.data.label || '')
          return relType ? `<span style="color:#9CA3AF">${relType}</span>` : ''
        }
        return ''
      }
    },
    series: [
      {
        type: 'graph',
        layout: 'force',
        roam: true,
        draggable: true,
        data: nodes,
        links: links,
        force: {
          repulsion: 200,
          edgeLength: [80, 200],
          gravity: 0.1,
          layoutAnimation: true
        },
        emphasis: {
          focus: 'adjacency',
          lineStyle: { width: 3 }
        },
        edgeSymbol: ['none', 'arrow'],
        edgeSymbolSize: [0, 8],
        lineStyle: {
          color: '#CBD5E1',
          width: 1.5,
          curveness: 0.1
        }
      }
    ]
  }
}

const initChart = () => {
  if (!chartRef.value || chartInstance) return
  // 用 wrapRef 的实际尺寸初始化，彻底避免 CSS 高度链问题
  const wrap = wrapRef.value
  const w = wrap ? wrap.clientWidth : 800
  const h = wrap ? wrap.clientHeight : 500
  chartInstance = echarts.init(chartRef.value, null, {
    width: w || 800,
    height: h || 500
  })
  chartInstance.on('click', (params) => {
    if (params.dataType === 'node') {
      selectedNode.value = params.data._raw || { name: params.data.name, label: params.data.nodeType }
    }
  })
}

const renderChart = () => {
  if (!chartRef.value) return
  initChart()
  // 每次渲染前同步容器尺寸
  if (wrapRef.value) {
    chartInstance.resize({
      width: wrapRef.value.clientWidth,
      height: wrapRef.value.clientHeight
    })
  }
  chartInstance.setOption(buildChartOption(), true)
}

const zoomIn = () => {
  if (!chartInstance) return
  const option = chartInstance.getOption()
  const zoom = (option.series[0].zoom || 1) * 1.2
  chartInstance.setOption({ series: [{ zoom }] })
}

const zoomOut = () => {
  if (!chartInstance) return
  const option = chartInstance.getOption()
  const zoom = (option.series[0].zoom || 1) / 1.2
  chartInstance.setOption({ series: [{ zoom }] })
}

const resetZoom = () => {
  if (!chartInstance) return
  chartInstance.setOption({ series: [{ zoom: 1, center: ['50%', '50%'] }] })
}

// ---- 专利详情跳转 ----
const viewPatentDetail = async (node) => {
  // node.name 是专利公开号（如 CN201910123456A）
  // 需要通过公开号查询专利列表获取数据库 id，再跳转详情页
  const publicationNo = node.name
  if (!publicationNo) return

  try {
    const res = await patentApi.getList({ keyword: publicationNo, pageNum: 1, pageSize: 5 })
    if (res && res.data && res.data.list && res.data.list.length > 0) {
      // 优先精确匹配公开号
      const matched = res.data.list.find(p => p.publicationNo === publicationNo)
      const patent = matched || res.data.list[0]
      router.push({ path: `/patent/detail/${patent.id}` })
    } else {
      // 未找到则降级到搜索页
      ElMessage.info('未找到对应专利，将跳转到搜索页')
      router.push({ path: '/search', query: { keyword: publicationNo } })
    }
  } catch (err) {
    console.error('查询专利失败:', err)
    // 降级到搜索页
    router.push({ path: '/search', query: { keyword: publicationNo } })
  }
}

// ---- 响应式 resize ----
const handleResize = () => {
  if (!chartInstance || !wrapRef.value) return
  chartInstance.resize({
    width: wrapRef.value.clientWidth,
    height: wrapRef.value.clientHeight
  })
}

// ---- keep-alive 激活时重新同步 ECharts 尺寸，并处理路由跳转参数 ----
onActivated(() => {
  // 1. 同步 ECharts 容器尺寸
  if (chartInstance && wrapRef.value) {
    nextTick(() => {
      chartInstance.resize({
        width: wrapRef.value.clientWidth,
        height: wrapRef.value.clientHeight
      })
    })
  }

  // 2. 若路由携带 mode + q 参数（如从专利详情页跳转），自动填充并查询
  const { mode, q } = route.query
  if (mode && q) {
    queryMode.value = mode
    queryInput.value = q
    handleQuery()
  }
})

// ---- 从路由参数自动查询 ----
onMounted(() => {
  window.addEventListener('resize', handleResize)

  // ResizeObserver 监听容器尺寸变化，自动同步 ECharts 尺寸
  if (wrapRef.value && typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      handleResize()
    })
    resizeObserver.observe(wrapRef.value)
  }

  const { mode, q } = route.query
  if (mode && q) {
    queryMode.value = mode
    queryInput.value = q
    handleQuery()
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  resizeObserver?.disconnect()
  resizeObserver = null
  chartInstance?.dispose()
  chartInstance = null
})
</script>

<style lang="scss" scoped>
.graph-page {
  display: flex;
  flex-direction: column;
  /* 使用父容器高度而非 100vh 计算，避免 layout-content padding 导致溢出 */
  height: 100%;
  min-height: 600px;
}

.page-header {
  margin-bottom: var(--space-4);
  .page-title {
    font-family: var(--font-heading);
    font-size: var(--text-2xl);
    font-weight: var(--font-bold);
    color: var(--color-text-primary);
    margin-bottom: var(--space-1);
  }
  .page-desc {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }
}

.control-panel {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4) var(--space-5);
  margin-bottom: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.mode-tabs {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
}

.mode-tab {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border);
  background: var(--color-bg-secondary);
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);

  .mode-icon {
    display: flex;
    align-items: center;
  }

  &:hover {
    border-color: var(--color-accent);
    color: var(--color-accent);
  }

  &.active {
    background: var(--color-accent);
    border-color: var(--color-accent);
    color: #fff;
  }
}

.query-form {
  display: flex;
  gap: var(--space-3);
  align-items: center;
  flex-wrap: wrap;
}

.query-input {
  flex: 1;
  min-width: 200px;
}

.query-select {
  width: 160px;
}

.query-select-sm {
  width: 100px;
}

.graph-workspace {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 220px;
  grid-template-rows: 1fr;
  gap: var(--space-4);
  /* grid 子元素需要 min-height:0 才能收缩到容器内 */
  min-height: 0;
}

.graph-canvas-wrap {
  position: relative;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  /* grid 子元素：height:100% 填满 grid 行高 */
  height: 100%;
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.graph-canvas {
  /* 绝对定位填满父容器，ECharts 才能拿到正确尺寸 */
  position: absolute;
  inset: 0;
  width: 100% !important;
  height: 100% !important;
}

.graph-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);
  color: var(--color-text-muted);
  padding: var(--space-12);

  p {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }
}

.empty-hints {
  display: flex;
  gap: var(--space-2);
  flex-wrap: wrap;
  justify-content: center;
}

.hint-tag {
  padding: var(--space-1) var(--space-3);
  background: var(--color-bg-secondary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-full);
  font-size: var(--text-xs);
  color: var(--color-accent);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-default);

  &:hover {
    background: #EFF6FF;
    border-color: var(--color-accent);
  }
}

.graph-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-4);

  p {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }
}

.loading-spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--color-border);
  border-top-color: var(--color-accent);
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.graph-toolbar {
  position: absolute;
  right: var(--space-4);
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  z-index: 10;
}

.toolbar-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  color: var(--color-text-secondary);
  transition: all var(--duration-fast) var(--ease-default);
  box-shadow: var(--shadow-sm);

  &:hover {
    border-color: var(--color-accent);
    color: var(--color-accent);
    box-shadow: var(--shadow-md);
  }
}

.graph-info-panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  overflow-y: auto;
}

.panel-section {
  background: var(--color-bg-primary);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: var(--space-4);
}

.panel-title {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  margin-bottom: var(--space-3);
  padding-bottom: var(--space-2);
  border-bottom: 1px solid var(--color-border-light);
}

.legend-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-label {
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}

.stats-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.stat-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-label {
  font-size: var(--text-xs);
  color: var(--color-text-muted);
}

.stat-value {
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  color: var(--color-text-primary);
  font-family: var(--font-mono);
}

.node-detail {
  border-color: var(--color-accent);
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.node-type-badge {
  display: inline-block;
  padding: var(--space-1) var(--space-2);
  border-radius: var(--radius-sm);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
  color: #fff;
  width: fit-content;
}

.node-name {
  font-size: var(--text-sm);
  font-weight: var(--font-medium);
  color: var(--color-text-primary);
  word-break: break-all;
}

.node-actions {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

@media (max-width: 1024px) {
  .graph-workspace {
    grid-template-columns: 1fr;
  }

  .graph-info-panel {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .mode-tabs {
    flex-wrap: wrap;
  }

  .graph-info-panel {
    grid-template-columns: 1fr;
  }
}
</style>
