<template>
  <div class="page-container analysis-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">数据分析</h1>
        <p class="page-desc">可视化展示专利库的实体分布、领域构成和申请趋势</p>
      </div>
      <el-button type="primary" :loading="loading" @click="refreshAllData">
        <el-icon>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="23 4 23 10 17 10"/>
            <polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
        </el-icon>
        刷新数据
      </el-button>
    </div>

    <!-- 概览统计卡片 -->
    <div class="overview-section">
      <div class="stats-row">
        <div class="stat-card">
          <div class="stat-icon patents">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
              <polyline points="14 2 14 8 20 8"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ overview.totalPatents || 0 }}</span>
            <span class="stat-label">专利总数</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon entities">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <path d="M8 14s1.5 2 4 2 4-2 4-2"/>
              <line x1="9" y1="9" x2="9.01" y2="9"/>
              <line x1="15" y1="9" x2="15.01" y2="9"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ overview.totalEntities || 0 }}</span>
            <span class="stat-label">实体总数</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon domains">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="12 2 2 7 12 12 22 7 12 2"/>
              <polyline points="2 17 12 22 22 17"/>
              <polyline points="2 12 12 17 22 12"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ overview.totalDomains || 0 }}</span>
            <span class="stat-label">技术领域</span>
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-icon matches">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="18" cy="5" r="3"/>
              <circle cx="6" cy="12" r="3"/>
              <circle cx="18" cy="19" r="3"/>
              <line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/>
              <line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/>
            </svg>
          </div>
          <div class="stat-info">
            <span class="stat-value">{{ overview.totalMatches || 0 }}</span>
            <span class="stat-label">匹配记录</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 图表区域 -->
    <div class="charts-section">
      <!-- 第一行：实体词云 + 领域分布 -->
      <div class="chart-row two-columns">
        <!-- 实体词云图 -->
        <div class="card chart-card">
          <div class="chart-header">
            <h3 class="chart-title">
              <el-icon>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="12" y1="8" x2="12" y2="12"/>
                  <line x1="12" y1="16" x2="12.01" y2="16"/>
                </svg>
              </el-icon>
              实体词云
            </h3>
            <span class="chart-desc">展示专利中出现频率最高的技术实体</span>
          </div>
          <div class="chart-body">
            <div
              v-if="wordCloudData.length > 0"
              ref="wordCloudRef"
              class="chart-container wordcloud-container"
            />
            <div v-else class="chart-empty">
              <el-icon class="empty-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                </svg>
              </el-icon>
              <span>暂无实体数据</span>
            </div>
          </div>
        </div>

        <!-- 领域分布饼图 -->
        <div class="card chart-card">
          <div class="chart-header">
            <h3 class="chart-title">
              <el-icon>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21.21 15.89A10 10 0 1 1 8 2.83"/>
                  <path d="M22 12A10 10 0 0 0 12 2v10z"/>
                </svg>
              </el-icon>
              领域分布
            </h3>
            <span class="chart-desc">IPC分类部级的专利数量分布</span>
          </div>
          <div class="chart-body">
            <div
              v-if="domainData.length > 0"
              ref="domainPieRef"
              class="chart-container"
            />
            <div v-else class="chart-empty">
              <el-icon class="empty-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                </svg>
              </el-icon>
              <span>暂无领域数据</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 第二行：专利趋势 + 实体类型分布 -->
      <div class="chart-row two-columns">
        <!-- 专利申请趋势折线图 -->
        <div class="card chart-card">
          <div class="chart-header">
            <h3 class="chart-title">
              <el-icon>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
                </svg>
              </el-icon>
              专利趋势
            </h3>
            <span class="chart-desc">按年份的专利申请数量变化</span>
          </div>
          <div class="chart-body">
            <div
              v-if="trendData.length > 0"
              ref="trendLineRef"
              class="chart-container"
            />
            <div v-else class="chart-empty">
              <el-icon class="empty-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                </svg>
              </el-icon>
              <span>暂无趋势数据</span>
            </div>
          </div>
        </div>

        <!-- 实体类型分布柱状图 -->
        <div class="card chart-card">
          <div class="chart-header">
            <h3 class="chart-title">
              <el-icon>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="20" x2="18" y2="10"/>
                  <line x1="12" y1="20" x2="12" y2="4"/>
                  <line x1="6" y1="20" x2="6" y2="14"/>
                </svg>
              </el-icon>
              实体类型
            </h3>
            <span class="chart-desc">各类型技术实体的数量统计</span>
          </div>
          <div class="chart-body">
            <div
              v-if="entityTypeData.length > 0"
              ref="entityTypeRef"
              class="chart-container"
            />
            <div v-else class="chart-empty">
              <el-icon class="empty-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                </svg>
              </el-icon>
              <span>暂无实体类型数据</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 第三行：申请人排行 -->
      <div class="chart-row one-column">
        <div class="card chart-card">
          <div class="chart-header">
            <h3 class="chart-title">
              <el-icon>
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
              </el-icon>
              申请人排行
            </h3>
            <span class="chart-desc">专利申请数量最多的申请人 TOP 10</span>
          </div>
          <div class="chart-body">
            <div
              v-if="applicantData.length > 0"
              ref="applicantBarRef"
              class="chart-container applicant-chart"
            />
            <div v-else class="chart-empty">
              <el-icon class="empty-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <circle cx="12" cy="12" r="10"/>
                  <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
                </svg>
              </el-icon>
              <span>暂无申请人数据</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import 'echarts-wordcloud'
import {
  getOverviewStats,
  getEntityWordCloud,
  getEntityTypeStats,
  getDomainStats,
  getPatentTrend,
  getTopApplicants
} from '@/api/stats'

// 状态
const loading = ref(false)
const overview = reactive({
  totalPatents: 0,
  totalEntities: 0,
  totalDomains: 0,
  totalUsers: 0,
  totalMatches: 0
})

// 图表数据
const wordCloudData = ref([])
const domainData = ref([])
const trendData = ref([])
const entityTypeData = ref([])
const applicantData = ref([])

// 图表DOM引用
const wordCloudRef = ref(null)
const domainPieRef = ref(null)
const trendLineRef = ref(null)
const entityTypeRef = ref(null)
const applicantBarRef = ref(null)

// 图表实例
let wordCloudChart = null
let domainPieChart = null
let trendLineChart = null
let entityTypeChart = null
let applicantBarChart = null

// 颜色配置
const entityTypeColors = {
  PRODUCT: '#3B82F6',    // 蓝色 - 产品/设备
  METHOD: '#10B981',     // 绿色 - 方法/工艺
  MATERIAL: '#F59E0B',   // 橙色 - 材料/物质
  COMPONENT: '#8B5CF6',  // 紫色 - 组件/部件
  EFFECT: '#EF4444',     // 红色 - 效果/性能
  APPLICATION: '#06B6D4' // 青色 - 应用场景
}

const domainColors = [
  '#3B82F6', '#10B981', '#F59E0B', '#EF4444',
  '#8B5CF6', '#06B6D4', '#EC4899', '#84CC16'
]

// 加载概览数据
const loadOverview = async () => {
  try {
    const res = await getOverviewStats()
    if (res.code === 200 && res.data) {
      Object.assign(overview, res.data)
    }
  } catch (error) {
    console.error('加载概览数据失败:', error)
  }
}

// 加载实体词云数据
const loadWordCloud = async () => {
  try {
    const res = await getEntityWordCloud(100)
    if (res.code === 200 && res.data?.data) {
      wordCloudData.value = res.data.data.map(item => ({
        name: item.name,
        value: item.count,
        type: item.type
      }))
      await nextTick()
      renderWordCloud()
    }
  } catch (error) {
    console.error('加载词云数据失败:', error)
  }
}

// 加载领域分布数据
const loadDomainStats = async () => {
  try {
    const res = await getDomainStats()
    if (res.code === 200 && res.data) {
      domainData.value = res.data.map(item => ({
        name: `${item.code} - ${item.description}`,
        value: item.count,
        code: item.code
      }))
      await nextTick()
      renderDomainPie()
    }
  } catch (error) {
    console.error('加载领域数据失败:', error)
  }
}

// 加载专利趋势数据
const loadTrend = async () => {
  try {
    const res = await getPatentTrend(10)
    if (res.code === 200 && res.data) {
      trendData.value = res.data
      await nextTick()
      renderTrendLine()
    }
  } catch (error) {
    console.error('加载趋势数据失败:', error)
  }
}

// 加载实体类型数据
const loadEntityTypes = async () => {
  try {
    const res = await getEntityTypeStats()
    if (res.code === 200 && res.data) {
      entityTypeData.value = res.data
      await nextTick()
      renderEntityTypeBar()
    }
  } catch (error) {
    console.error('加载实体类型数据失败:', error)
  }
}

// 加载申请人排行数据
const loadApplicants = async () => {
  try {
    const res = await getTopApplicants(10)
    if (res.code === 200 && res.data) {
      applicantData.value = res.data
      await nextTick()
      renderApplicantBar()
    }
  } catch (error) {
    console.error('加载申请人数据失败:', error)
  }
}

// 渲染实体词云图
const renderWordCloud = () => {
  if (!wordCloudRef.value || wordCloudData.value.length === 0) return

  if (wordCloudChart) {
    wordCloudChart.dispose()
  }

  wordCloudChart = echarts.init(wordCloudRef.value)

  const option = {
    tooltip: {
      show: true,
      formatter: (params) => {
        const typeNames = {
          PRODUCT: '产品/设备',
          METHOD: '方法/工艺',
          MATERIAL: '材料/物质',
          COMPONENT: '组件/部件',
          EFFECT: '效果/性能',
          APPLICATION: '应用场景'
        }
        return `<div style="font-weight: 600;">${params.name}</div>
                <div style="margin-top: 4px;">出现次数: ${params.value}</div>
                <div>类型: ${typeNames[params.data.type] || params.data.type}</div>`
      }
    },
    series: [{
      type: 'wordCloud',
      shape: 'circle',
      left: 'center',
      top: 'center',
      width: '90%',
      height: '90%',
      sizeRange: [14, 60],
      rotationRange: [-45, 45],
      rotationStep: 15,
      gridSize: 8,
      drawOutOfBound: false,
      layoutAnimation: true,
      textStyle: {
        fontFamily: 'Inter, -apple-system, BlinkMacSystemFont, sans-serif',
        fontWeight: 600,
        color: (params) => {
          return entityTypeColors[params.data.type] || '#6B7280'
        }
      },
      emphasis: {
        textStyle: {
          color: '#1D4ED8',
          fontWeight: 700
        }
      },
      data: wordCloudData.value
    }]
  }

  wordCloudChart.setOption(option)
}

// 渲染领域分布饼图
const renderDomainPie = () => {
  if (!domainPieRef.value || domainData.value.length === 0) return

  if (domainPieChart) {
    domainPieChart.dispose()
  }

  domainPieChart = echarts.init(domainPieRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}<br/>专利数量: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: {
        color: '#6B7280',
        fontSize: 12
      }
    },
    series: [{
      type: 'pie',
      radius: ['40%', '70%'],
      center: ['35%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 14,
          fontWeight: 'bold'
        },
        itemStyle: {
          shadowBlur: 10,
          shadowOffsetX: 0,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      data: domainData.value.map((item, index) => ({
        ...item,
        itemStyle: {
          color: domainColors[index % domainColors.length]
        }
      }))
    }]
  }

  domainPieChart.setOption(option)
}

// 渲染专利趋势折线图
const renderTrendLine = () => {
  if (!trendLineRef.value || trendData.value.length === 0) return

  if (trendLineChart) {
    trendLineChart.dispose()
  }

  trendLineChart = echarts.init(trendLineRef.value)

  const years = trendData.value.map(item => item.year)
  const counts = trendData.value.map(item => item.count)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'cross',
        label: {
          backgroundColor: '#1D4ED8'
        }
      },
      formatter: '{b}年<br/>专利数量: {c}'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: years,
      axisLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      },
      axisLabel: {
        color: '#6B7280'
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#6B7280'
      },
      splitLine: {
        lineStyle: {
          color: '#F3F4F6'
        }
      }
    },
    series: [{
      name: '专利数量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      sampling: 'lttb',
      itemStyle: {
        color: '#3B82F6'
      },
      lineStyle: {
        width: 3,
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#3B82F6' },
          { offset: 1, color: '#8B5CF6' }
        ])
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(59, 130, 246, 0.3)' },
          { offset: 1, color: 'rgba(59, 130, 246, 0.05)' }
        ])
      },
      data: counts
    }]
  }

  trendLineChart.setOption(option)
}

// 渲染实体类型柱状图
const renderEntityTypeBar = () => {
  if (!entityTypeRef.value || entityTypeData.value.length === 0) return

  if (entityTypeChart) {
    entityTypeChart.dispose()
  }

  entityTypeChart = echarts.init(entityTypeRef.value)

  const types = entityTypeData.value.map(item => item.description)
  const counts = entityTypeData.value.map(item => item.count)
  const colors = entityTypeData.value.map(item => entityTypeColors[item.type] || '#6B7280')

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: '{b}<br/>数量: {c}'
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: types,
      axisLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      },
      axisLabel: {
        color: '#6B7280',
        interval: 0,
        rotate: 30
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#6B7280'
      },
      splitLine: {
        lineStyle: {
          color: '#F3F4F6'
        }
      }
    },
    series: [{
      name: '实体数量',
      type: 'bar',
      barWidth: '50%',
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: (params) => colors[params.dataIndex]
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      data: counts
    }]
  }

  entityTypeChart.setOption(option)
}

// 渲染申请人排行柱状图
const renderApplicantBar = () => {
  if (!applicantBarRef.value || applicantData.value.length === 0) return

  if (applicantBarChart) {
    applicantBarChart.dispose()
  }

  applicantBarChart = echarts.init(applicantBarRef.value)

  // 倒序排列，数量最多的在最上面
  const sortedData = [...applicantData.value].reverse()
  const names = sortedData.map(item => item.name)
  const counts = sortedData.map(item => item.count)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: '{b}<br/>专利数量: {c}'
    },
    grid: {
      left: '3%',
      right: '8%',
      bottom: '3%',
      top: '5%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLine: {
        show: false
      },
      axisTick: {
        show: false
      },
      axisLabel: {
        color: '#6B7280'
      },
      splitLine: {
        lineStyle: {
          color: '#F3F4F6'
        }
      }
    },
    yAxis: {
      type: 'category',
      data: names,
      axisLine: {
        lineStyle: {
          color: '#E5E7EB'
        }
      },
      axisLabel: {
        color: '#374151',
        fontSize: 12,
        formatter: (value) => {
          return value.length > 15 ? value.substring(0, 15) + '...' : value
        }
      }
    },
    series: [{
      name: '专利数量',
      type: 'bar',
      barWidth: '60%',
      itemStyle: {
        borderRadius: [0, 6, 6, 0],
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#3B82F6' },
          { offset: 1, color: '#8B5CF6' }
        ])
      },
      label: {
        show: true,
        position: 'right',
        color: '#6B7280',
        fontSize: 12
      },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.2)'
        }
      },
      data: counts
    }]
  }

  applicantBarChart.setOption(option)
}

// 刷新所有数据
const refreshAllData = async () => {
  loading.value = true
  try {
    await Promise.all([
      loadOverview(),
      loadWordCloud(),
      loadDomainStats(),
      loadTrend(),
      loadEntityTypes(),
      loadApplicants()
    ])
  } finally {
    loading.value = false
  }
}

// 处理窗口大小变化
const handleResize = () => {
  wordCloudChart?.resize()
  domainPieChart?.resize()
  trendLineChart?.resize()
  entityTypeChart?.resize()
  applicantBarChart?.resize()
}

// 生命周期
onMounted(() => {
  refreshAllData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  wordCloudChart?.dispose()
  domainPieChart?.dispose()
  trendLineChart?.dispose()
  entityTypeChart?.dispose()
  applicantBarChart?.dispose()
})
</script>

<style lang="scss" scoped>
.analysis-page {
  max-width: 1600px;
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

// 概览统计
.overview-section {
  margin-bottom: var(--space-6);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-4);
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
  width: 52px;
  height: 52px;
  border-radius: var(--radius-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;

  &.patents {
    background-color: #EFF6FF;
    color: #3B82F6;
  }

  &.entities {
    background-color: #ECFDF5;
    color: #10B981;
  }

  &.domains {
    background-color: #FEF3C7;
    color: #F59E0B;
  }

  &.matches {
    background-color: #F3E8FF;
    color: #8B5CF6;
  }
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-value {
  font-family: var(--font-heading);
  font-size: var(--text-3xl);
  font-weight: var(--font-bold);
  color: var(--color-text-primary);
  line-height: 1;
}

.stat-label {
  font-size: var(--text-sm);
  color: var(--color-text-muted);
  margin-top: var(--space-1);
}

// 图表区域
.charts-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
}

.chart-row {
  display: grid;
  gap: var(--space-6);

  &.two-columns {
    grid-template-columns: repeat(2, 1fr);
  }

  &.one-column {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  padding: var(--space-5);
}

.chart-header {
  margin-bottom: var(--space-4);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border-light);

  .chart-title {
    display: flex;
    align-items: center;
    gap: var(--space-2);
    font-size: var(--text-lg);
    font-weight: var(--font-semibold);
    color: var(--color-text-primary);
    margin: 0 0 var(--space-1) 0;

    .el-icon {
      color: var(--color-accent);
    }
  }

  .chart-desc {
    font-size: var(--text-sm);
    color: var(--color-text-muted);
  }
}

.chart-body {
  min-height: 350px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.chart-container {
  width: 100%;
  height: 350px;

  &.wordcloud-container {
    height: 380px;
  }

  &.applicant-chart {
    height: 400px;
  }
}

.chart-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  color: var(--color-text-muted);

  .empty-icon {
    font-size: 48px;
    color: var(--color-border);
  }

  span {
    font-size: var(--text-sm);
  }
}

// 响应式
@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }

  .chart-row.two-columns {
    grid-template-columns: 1fr;
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

  .chart-body {
    min-height: 280px;
  }

  .chart-container {
    height: 280px;

    &.wordcloud-container {
      height: 300px;
    }

    &.applicant-chart {
      height: 320px;
    }
  }
}
</style>
