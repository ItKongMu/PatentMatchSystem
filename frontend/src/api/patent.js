import request from './request'

/**
 * 专利管理相关API
 */
export const patentApi = {
  /**
   * 上传专利PDF
   * @param {FormData} formData - 包含file和publicationNo的表单数据
   */
  upload(formData) {
    return request({
      url: '/patent/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 120000 // 上传超时2分钟
    })
  },
  
  /**
   * 文本录入专利
   * @param {Object} data - 专利信息
   * @param {string} data.title - 专利标题
   * @param {string} data.abstract - 专利摘要
   * @param {string} data.publicationNo - 公开号（可选）
   * @param {string} data.applicant - 申请人（可选）
   */
  createByText(data) {
    return request({
      url: '/patent/text',
      method: 'post',
      data
    })
  },
  
  /**
   * 触发专利处理流程
   * @param {number} id - 专利ID
   */
  process(id) {
    return request({
      url: `/patent/process/${id}`,
      method: 'post',
      timeout: 180000 // 处理超时3分钟（LLM处理可能较慢）
    })
  },
  
  /**
   * 获取专利详情
   * @param {number} id - 专利ID
   */
  getDetail(id) {
    return request({
      url: `/patent/${id}`,
      method: 'get'
    })
  },
  
  /**
   * 获取专利实体列表
   * @param {number} id - 专利ID
   */
  getEntities(id) {
    return request({
      url: `/patent/${id}/entities`,
      method: 'get'
    })
  },
  
  /**
   * 获取专利领域层次
   * @param {number} id - 专利ID
   */
  getDomains(id) {
    return request({
      url: `/patent/${id}/domains`,
      method: 'get'
    })
  },
  
  /**
   * 获取专利向量信息
   * @param {number} id - 专利ID
   */
  getVector(id) {
    return request({
      url: `/patent/${id}/vector`,
      method: 'get'
    })
  },
  
  /**
   * 获取专利列表
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码
   * @param {number} params.size - 每页数量
   * @param {string} params.parseStatus - 解析状态（可选）
   * @param {string} params.keyword - 关键词（可选）
   */
  getList(params) {
    return request({
      url: '/patent/list',
      method: 'get',
      params
    })
  },
  
  /**
   * 删除专利
   * @param {number} id - 专利ID
   */
  delete(id) {
    return request({
      url: `/patent/${id}`,
      method: 'delete'
    })
  },

  /**
   * 预览CSV文件
   * @param {FormData} formData - 包含file的表单数据
   */
  previewCsv(formData) {
    return request({
      url: '/patent/csv/preview',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 60000
    })
  },

  /**
   * 导入CSV文件
   * @param {FormData} formData - 包含file的表单数据
   * @param {boolean} autoProcess - 是否自动处理
   */
  importCsv(formData, autoProcess = false) {
    return request({
      url: `/patent/csv/import?autoProcess=${autoProcess}`,
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: 300000 // 5分钟超时（批量导入可能较慢）
    })
  },

  /**
   * 导入预览的CSV数据
   * @param {Array} dataList - 预览的数据列表
   * @param {boolean} autoProcess - 是否自动处理
   */
  importCsvPreviewData(dataList, autoProcess = false) {
    return request({
      url: `/patent/csv/import/preview?autoProcess=${autoProcess}`,
      method: 'post',
      data: dataList,
      timeout: 300000
    })
  }
}
