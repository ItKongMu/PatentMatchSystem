import request from './request'

/**
 * 技术匹配相关API
 */
export const matchApi = {
  /**
   * 提交文本查询技术匹配（异步），立即返回 sessionId
   */
  submitTextMatch(data) {
    return request({
      url: '/match',
      method: 'post',
      data,
      timeout: 30000
    })
  },

  /**
   * 提交相似专利匹配（异步），立即返回 sessionId
   */
  submitPatentMatch(patentId, params = {}) {
    return request({
      url: `/match/patent/${patentId}`,
      method: 'post',
      params,
      timeout: 30000
    })
  },

  /**
   * 轮询任务状态
   * @param {string} sessionId
   */
  getTaskStatus(sessionId) {
    return request({
      url: `/match/task/${sessionId}`,
      method: 'get',
      timeout: 10000
    })
  },

  /**
   * 获取匹配历史记录（按session聚合）
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
  },

  /**
   * 获取某个session下的所有匹配专利
   * @param {string} sessionId
   */
  getSessionDetails(sessionId) {
    return request({
      url: `/match/history/${sessionId}`,
      method: 'get',
      timeout: 10000
    })
  }
}
