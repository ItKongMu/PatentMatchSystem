import { defineStore } from 'pinia'
import { ref } from 'vue'
import { patentApi } from '@/api/patent'

export const usePatentStore = defineStore('patent', () => {
  // 状态
  const patentList = ref([])
  const currentPatent = ref(null)
  const loading = ref(false)
  const total = ref(0)
  const queryParams = ref({
    page: 1,
    size: 10,
    parseStatus: '',
    keyword: ''
  })
  
  // 获取专利列表
  async function fetchPatentList(params = {}) {
    loading.value = true
    try {
      const mergedParams = { ...queryParams.value, ...params }
      const res = await patentApi.getList(mergedParams)
      if (res.code === 200) {
        patentList.value = res.data.records || []
        total.value = res.data.total || 0
        queryParams.value = { ...queryParams.value, ...params }
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    } finally {
      loading.value = false
    }
  }
  
  // 获取专利详情
  async function fetchPatentDetail(id) {
    loading.value = true
    try {
      const res = await patentApi.getDetail(id)
      if (res.code === 200) {
        currentPatent.value = res.data
        return { success: true, data: res.data }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    } finally {
      loading.value = false
    }
  }
  
  // 删除专利
  async function deletePatent(id) {
    try {
      const res = await patentApi.delete(id)
      if (res.code === 200) {
        // 重新获取列表
        await fetchPatentList()
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }
  
  // 触发处理流程
  async function processPatent(id) {
    try {
      const res = await patentApi.process(id)
      if (res.code === 200) {
        return { success: true }
      }
      return { success: false, message: res.message }
    } catch (error) {
      return { success: false, message: error.message }
    }
  }
  
  // 重置状态
  function reset() {
    patentList.value = []
    currentPatent.value = null
    total.value = 0
    queryParams.value = {
      page: 1,
      size: 10,
      parseStatus: '',
      keyword: ''
    }
  }
  
  return {
    // 状态
    patentList,
    currentPatent,
    loading,
    total,
    queryParams,
    // 方法
    fetchPatentList,
    fetchPatentDetail,
    deletePatent,
    processPatent,
    reset
  }
})
