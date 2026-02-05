<template>
  <el-tooltip 
    :content="isFavorite ? '取消收藏' : '添加收藏'" 
    placement="top"
  >
    <el-button
      :type="isFavorite ? 'warning' : 'default'"
      :icon="isFavorite ? StarFilled : Star"
      :loading="loading"
      :size="size"
      :circle="circle"
      @click="toggleFavorite"
    >
      <template v-if="showText">
        {{ isFavorite ? '已收藏' : '收藏' }}
      </template>
    </el-button>
  </el-tooltip>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'
import { Star, StarFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { favoriteApi } from '@/api/favorite'

const props = defineProps({
  patentId: {
    type: [Number, String],
    required: true
  },
  // 初始收藏状态（可选，用于批量加载优化）
  initialFavorite: {
    type: Boolean,
    default: null
  },
  size: {
    type: String,
    default: 'default'
  },
  circle: {
    type: Boolean,
    default: false
  },
  showText: {
    type: Boolean,
    default: true
  }
})

const emit = defineEmits(['change'])

const loading = ref(false)
const isFavorite = ref(false)

// 检查收藏状态
const checkFavorite = async () => {
  if (props.initialFavorite !== null) {
    isFavorite.value = props.initialFavorite
    return
  }
  
  try {
    const res = await favoriteApi.check(props.patentId)
    if (res.code === 200) {
      isFavorite.value = res.data
    }
  } catch (error) {
    console.error('检查收藏状态失败:', error)
  }
}

// 切换收藏状态
const toggleFavorite = async () => {
  loading.value = true
  try {
    if (isFavorite.value) {
      // 取消收藏
      const confirmed = await ElMessageBox.confirm(
        '确定要取消收藏该专利吗？',
        '提示',
        { type: 'warning' }
      ).catch(() => false)
      
      if (!confirmed) {
        loading.value = false
        return
      }
      
      const res = await favoriteApi.remove(props.patentId)
      if (res.code === 200) {
        isFavorite.value = false
        ElMessage.success('已取消收藏')
        emit('change', false)
      }
    } else {
      // 添加收藏
      const res = await favoriteApi.add({ patentId: Number(props.patentId) })
      if (res.code === 200) {
        isFavorite.value = true
        ElMessage.success('收藏成功')
        emit('change', true)
      }
    }
  } catch (error) {
    if (error !== 'cancel' && error !== false) {
      console.error('收藏操作失败:', error)
      ElMessage.error('操作失败，请重试')
    }
  } finally {
    loading.value = false
  }
}

// 监听初始状态变化
watch(() => props.initialFavorite, (newVal) => {
  if (newVal !== null) {
    isFavorite.value = newVal
  }
})

// 监听专利ID变化
watch(() => props.patentId, () => {
  checkFavorite()
})

onMounted(() => {
  checkFavorite()
})

// 暴露方法供父组件调用
defineExpose({
  refresh: checkFavorite
})
</script>

<style lang="scss" scoped>
:deep(.el-button) {
  transition: all 0.2s ease;
  
  &.el-button--warning {
    --el-button-bg-color: #fef3c7;
    --el-button-border-color: #fcd34d;
    --el-button-text-color: #d97706;
    
    &:hover {
      --el-button-bg-color: #fde68a;
      --el-button-border-color: #fbbf24;
    }
  }
}
</style>
