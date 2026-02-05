import request from './request'

/**
 * 技术匹配相关API
 */
export const matchApi = {
  /**
   * LLM技术匹配（文本查询）
   * @param {Object} data - 匹配参数
   * @param {string} data.query - 查询文本
   * @param {string} data.domainFilter - 领域过滤（可选，如G06）
   * @param {number} data.topK - 返回结果数量（可选，默认10）
   */
  match(data) {
    return request({
      url: '/match',
      method: 'post',
      data,
      timeout: 180000 // 匹配超时3分钟（LLM处理可能较慢）
    })
  },
  
  /**
   * 相似专利匹配
   * @param {number} patentId - 源专利ID
   * @param {Object} params - 查询参数
   * @param {number} params.topK - 返回结果数量（可选，默认10）
   */
  matchByPatent(patentId, params = {}) {
    return request({
      url: `/match/patent/${patentId}`,
      method: 'post',
      params,
      timeout: 180000
    })
  },
  
  /**
   * 获取匹配历史记录
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码
   * @param {number} params.size - 每页数量
   * @param {string} params.matchMode - 匹配模式（可选：PATENT/TEXT）
   */
  getHistory(params) {
    return request({
      url: '/match/history',
      method: 'get',
      params: {
        pageNum: params.page,
        pageSize: params.size,
        matchMode: params.matchMode
      }
    })
  }
}
