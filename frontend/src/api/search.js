import request from './request'

/**
 * 专利检索相关API
 */
export const searchApi = {
  /**
   * 快速检索（推荐）- 支持高亮显示
   * @param {Object} params - 检索参数
   * @param {string} params.keyword - 关键词
   * @param {number} params.page - 页码
   * @param {number} params.size - 每页数量
   */
  quickSearch(params) {
    return request({
      url: '/search/quick',
      method: 'get',
      params: {
        keyword: params.keyword,
        pageNum: params.page,
        pageSize: params.size
      }
    })
  },

  /**
   * ES关键词检索（兼容旧接口）
   * @param {Object} params - 检索参数
   * @param {string} params.keyword - 关键词
   * @param {number} params.page - 页码
   * @param {number} params.size - 每页数量
   */
  search(params) {
    return request({
      url: '/search',
      method: 'get',
      params: {
        keyword: params.keyword,
        pageNum: params.page,
        pageSize: params.size
      }
    })
  },
  
  /**
   * 高级检索（推荐）- 支持高亮显示
   * @param {Object} data - 检索条件
   */
  advancedSearchV2(data) {
    return request({
      url: '/search/advanced/v2',
      method: 'post',
      data: {
        keyword: data.keyword,
        title: data.title,
        abstractKeyword: data.abstractKeyword,
        domainCode: data.domainCode,
        domainSections: data.domainSections,
        applicant: data.applicant,
        applicantKeyword: data.applicantKeyword,
        publicationNo: data.publicationNo,
        entityKeyword: data.entityKeyword,
        entityType: data.entityType,
        entityTypes: data.entityTypes,
        publicationDateFrom: data.publicationDateFrom,
        publicationDateTo: data.publicationDateTo,
        enableHighlight: data.enableHighlight !== false,
        sortField: data.sortField || '_score',
        sortOrder: data.sortOrder || 'desc',
        pageNum: data.page || data.pageNum || 1,
        pageSize: data.size || data.pageSize || 10
      }
    })
  },

  /**
   * 高级检索（兼容旧接口）
   * @param {Object} data - 检索条件
   */
  advancedSearch(data) {
    return request({
      url: '/search/advanced',
      method: 'post',
      data: {
        title: data.title,
        abstractKeyword: data.abstract || data.abstractKeyword,
        domainCode: data.domainCode,
        applicant: data.applicant,
        entityType: data.entityType,
        pageNum: data.page,
        pageSize: data.size
      }
    })
  },

  /**
   * 深度分页检索（search_after）
   * @param {Object} data - 检索条件（含searchAfter参数）
   */
  searchWithScroll(data) {
    return request({
      url: '/search/scroll',
      method: 'post',
      data: {
        ...data,
        useSearchAfter: true
      }
    })
  },

  /**
   * 领域分布统计
   * @param {string} keyword - 关键词（可选）
   */
  getDomainStats(keyword) {
    return request({
      url: '/search/stats/domain',
      method: 'get',
      params: { keyword }
    })
  },

  /**
   * 申请人排行统计
   * @param {string} keyword - 关键词（可选）
   * @param {number} topN - 返回前N名
   */
  getTopApplicants(keyword, topN = 10) {
    return request({
      url: '/search/stats/applicants',
      method: 'get',
      params: { keyword, topN }
    })
  },

  /**
   * 检查ES可用性
   */
  checkHealth() {
    return request({
      url: '/search/health',
      method: 'get'
    })
  }
}
