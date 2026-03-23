import request from './request'

/**
 * API 常量配置
 * @description 与后端保持一致的配置参数
 */
export const PATENT_API_CONFIG = {
  /** PDF 上传超时时间（毫秒） */
  PDF_UPLOAD_TIMEOUT: 120000,
  /** 专利处理超时时间（毫秒） */
  PROCESS_TIMEOUT: 180000,
  /** CSV 预览超时时间（毫秒） */
  CSV_PREVIEW_TIMEOUT: 60000,
  /** CSV 导入超时时间（毫秒） */
  CSV_IMPORT_TIMEOUT: 300000,
  /** PDF 最大文件大小（字节） */
  MAX_PDF_SIZE: 50 * 1024 * 1024,
  /** CSV 最大文件大小（字节） */
  MAX_CSV_SIZE: 10 * 1024 * 1024
}

/**
 * 专利管理相关 API
 * @namespace patentApi
 */
export const patentApi = {
  /**
   * 上传专利 PDF 文件
   * @description 上传 PDF 文件到 MinIO 存储，创建专利记录
   * @param {FormData} formData - 表单数据
   * @param {File} formData.file - PDF 文件（必填，最大 50MB）
   * @param {string} [formData.publicationNo] - 公开号/专利号（可选，如 CN123456789A）
   * @returns {Promise<{code: number, message: string, data: {patentId: number, filePath: string, parseStatus: string}}>}
   * @throws {Error} 文件类型错误或大小超限时抛出
   * @example
   * const formData = new FormData()
   * formData.append('file', pdfFile)
   * formData.append('publicationNo', 'CN123456789A')
   * const result = await patentApi.upload(formData)
   */
  upload(formData) {
    return request({
      url: '/patent/upload',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: PATENT_API_CONFIG.PDF_UPLOAD_TIMEOUT
    })
  },
  
  /**
   * 文本录入专利
   * @description 手动录入专利信息，创建专利记录
   * @param {Object} data - 专利信息对象
   * @param {string} data.title - 专利标题（必填，5-200 字符）
   * @param {string} data.patentAbstract - 专利摘要（必填，50-5000 字符）
   * @param {string} [data.publicationNo] - 公开号/专利号（可选，格式如 CN123456789A）
   * @param {string} [data.applicant] - 申请人（可选，最大 200 字符）
   * @param {string} [data.publicationDate] - 公开日期（可选，格式 YYYY-MM-DD）
   * @param {string} [data.ipcClassification] - IPC 分类号（可选，多个用逗号分隔，最大 500 字符）
   * @param {string} [data.fullText] - 专利正文（可选，最大 10 万字符）
   * @returns {Promise<{code: number, message: string, data: {patentId: number, filePath: string, parseStatus: string}}>}
   * @example
   * const result = await patentApi.createByText({
   *   title: '一种智能检测方法',
   *   patentAbstract: '本发明提供一种基于深度学习的智能检测方法...',
   *   applicant: '华为技术有限公司',
   *   ipcClassification: 'G06F 16/30, G06N 3/08'
   * })
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
   * @description 异步执行 PDF 解析、实体提取、向量化和 ES 索引
   * @param {number} id - 专利 ID
   * @returns {Promise<{code: number, message: string, data: null}>}
   * @example
   * await patentApi.process(123)
   */
  process(id) {
    return request({
      url: `/patent/process/${id}`,
      method: 'post',
      timeout: PATENT_API_CONFIG.PROCESS_TIMEOUT
    })
  },

  /**
   * 重新处理专利（仅管理员）
   * @description 将专利状态重置为 PENDING 并重新触发处理流程，用于修复失败或异常的专利
   * @param {number} id - 专利 ID
   * @returns {Promise<{code: number, message: string, data: null}>}
   * @example
   * await patentApi.reprocess(123)
   */
  reprocess(id) {
    return request({
      url: `/patent/reprocess/${id}`,
      method: 'post',
      timeout: PATENT_API_CONFIG.PROCESS_TIMEOUT
    })
  },
  
  /**
   * 获取专利详情
   * @description 获取专利完整信息，包括实体、领域和向量信息
   * @param {number} id - 专利 ID
   * @returns {Promise<{code: number, message: string, data: Object}>}
   */
  getDetail(id) {
    return request({
      url: `/patent/${id}`,
      method: 'get'
    })
  },
  
  /**
   * 获取专利实体列表
   * @description 获取专利提取的技术实体（产品、方法、材料等）
   * @param {number} id - 专利 ID
   * @returns {Promise<{code: number, message: string, data: {id: number, entities: Array}}>}
   */
  getEntities(id) {
    return request({
      url: `/patent/${id}/entities`,
      method: 'get'
    })
  },
  
  /**
   * 获取专利领域层次
   * @description 获取专利的 IPC 分类领域信息
   * @param {number} id - 专利 ID
   * @returns {Promise<{code: number, message: string, data: {id: number, domains: Array}}>}
   */
  getDomains(id) {
    return request({
      url: `/patent/${id}/domains`,
      method: 'get'
    })
  },
  
  /**
   * 获取专利向量信息
   * @description 获取专利的向量化信息（向量 ID、模型、维度）
   * @param {number} id - 专利 ID
   * @returns {Promise<{code: number, message: string, data: {id: number, vector: Object}}>}
   */
  getVector(id) {
    return request({
      url: `/patent/${id}/vector`,
      method: 'get'
    })
  },
  
  /**
   * 分页查询专利列表
   * @description 支持按状态和关键词筛选
   * @param {Object} params - 查询参数
   * @param {number} [params.pageNum=1] - 页码
   * @param {number} [params.pageSize=10] - 每页数量
   * @param {string} [params.parseStatus] - 解析状态筛选（PENDING/PROCESSING/COMPLETED/FAILED）
   * @param {string} [params.keyword] - 关键词搜索（标题/摘要）
   * @returns {Promise<{code: number, message: string, data: {list: Array, total: number, pageNum: number, pageSize: number}}>}
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
   * @description 删除专利及其关联数据（向量、实体、领域、MinIO 文件、ES 索引）
   * @param {number} id - 专利 ID
   * @returns {Promise<{code: number, message: string, data: null}>}
   */
  delete(id) {
    return request({
      url: `/patent/${id}`,
      method: 'delete'
    })
  },

  /**
   * 预览 CSV 文件
   * @description 解析 CSV 文件并返回预览数据和校验结果
   * @param {FormData} formData - 表单数据
   * @param {File} formData.file - CSV 文件（必填，最大 10MB）
   * @returns {Promise<{code: number, message: string, data: {totalRows: number, validRows: number, invalidRows: number, previewData: Array, allData: Array, canImport: boolean}}>}
   */
  previewCsv(formData) {
    return request({
      url: '/patent/csv/preview',
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: PATENT_API_CONFIG.CSV_PREVIEW_TIMEOUT
    })
  },

  /**
   * 直接导入 CSV 文件
   * @description 上传并直接导入 CSV 文件中的专利数据
   * @param {FormData} formData - 表单数据
   * @param {File} formData.file - CSV 文件（必填，最大 10MB）
   * @param {boolean} [autoProcess=false] - 是否导入后自动处理
   * @returns {Promise<{code: number, message: string, data: {totalRows: number, successCount: number, failedCount: number, skippedCount: number, importedPatentIds: Array}}>}
   */
  importCsv(formData, autoProcess = false) {
    return request({
      url: `/patent/csv/import?autoProcess=${autoProcess}`,
      method: 'post',
      data: formData,
      headers: {
        'Content-Type': 'multipart/form-data'
      },
      timeout: PATENT_API_CONFIG.CSV_IMPORT_TIMEOUT
    })
  },

  /**
   * 导入预览的 CSV 数据
   * @description 导入经过预览和筛选的专利数据
   * @param {Array<Object>} dataList - 预览数据列表（通常只包含有效数据）
   * @param {string} dataList[].title - 专利标题
   * @param {string} dataList[].patentAbstract - 专利摘要
   * @param {string} [dataList[].publicationNo] - 公开号
   * @param {string} [dataList[].applicant] - 申请人
   * @param {string} [dataList[].publicationDate] - 公开日期
   * @param {string} [dataList[].ipcClassification] - IPC 分类号
   * @param {string} [dataList[].fullText] - 专利正文
   * @param {boolean} [autoProcess=false] - 是否导入后自动处理
   * @returns {Promise<{code: number, message: string, data: {totalRows: number, successCount: number, failedCount: number, skippedCount: number, importedPatentIds: Array, failedRows: Array}}>}
   */
  importCsvPreviewData(dataList, autoProcess = false) {
    return request({
      url: `/patent/csv/import/preview?autoProcess=${autoProcess}`,
      method: 'post',
      data: dataList,
      timeout: PATENT_API_CONFIG.CSV_IMPORT_TIMEOUT
    })
  }
}
