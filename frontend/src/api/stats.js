import request from './request'

/**
 * 统计分析 API
 */

/**
 * 获取系统概览统计
 */
export function getOverviewStats() {
  return request({
    url: '/stats/overview',
    method: 'get'
  })
}

/**
 * 获取实体词云数据
 * @param {number} topN 返回前N个高频实体
 */
export function getEntityWordCloud(topN = 100) {
  return request({
    url: '/stats/entity/wordcloud',
    method: 'get',
    params: { topN }
  })
}

/**
 * 获取实体类型分布
 */
export function getEntityTypeStats() {
  return request({
    url: '/stats/entity/types',
    method: 'get'
  })
}

/**
 * 获取领域分布统计
 */
export function getDomainStats() {
  return request({
    url: '/stats/domain/distribution',
    method: 'get'
  })
}

/**
 * 获取专利申请趋势
 * @param {number} years 统计最近N年的数据
 */
export function getPatentTrend(years = 10) {
  return request({
    url: '/stats/trend',
    method: 'get',
    params: { years }
  })
}

/**
 * 获取申请人排行
 * @param {number} topN 返回前N名
 */
export function getTopApplicants(topN = 10) {
  return request({
    url: '/stats/applicants/top',
    method: 'get',
    params: { topN }
  })
}
