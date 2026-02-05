import request from './request'

/**
 * 专利收藏相关API
 */
export const favoriteApi = {
  /**
   * 添加收藏
   * @param {Object} data - 收藏信息
   * @param {number} data.patentId - 专利ID
   * @param {string} data.remark - 收藏备注（可选）
   * @param {string} data.groupName - 分组名称（可选）
   */
  add(data) {
    return request({
      url: '/favorite',
      method: 'post',
      data
    })
  },

  /**
   * 取消收藏
   * @param {number} patentId - 专利ID
   */
  remove(patentId) {
    return request({
      url: `/favorite/${patentId}`,
      method: 'delete'
    })
  },

  /**
   * 检查是否已收藏
   * @param {number} patentId - 专利ID
   */
  check(patentId) {
    return request({
      url: `/favorite/check/${patentId}`,
      method: 'get'
    })
  },

  /**
   * 批量检查是否已收藏
   * @param {number[]} patentIds - 专利ID列表
   */
  batchCheck(patentIds) {
    return request({
      url: '/favorite/check/batch',
      method: 'post',
      data: patentIds
    })
  },

  /**
   * 获取收藏列表
   * @param {Object} params - 查询参数
   * @param {number} params.pageNum - 页码
   * @param {number} params.pageSize - 每页数量
   * @param {string} params.keyword - 关键词（可选）
   * @param {string} params.groupName - 分组名称（可选）
   */
  getList(params) {
    return request({
      url: '/favorite/list',
      method: 'get',
      params
    })
  },

  /**
   * 获取收藏夹分组列表
   */
  getGroups() {
    return request({
      url: '/favorite/groups',
      method: 'get'
    })
  },

  /**
   * 更新收藏信息
   * @param {number} patentId - 专利ID
   * @param {Object} data - 更新数据
   * @param {string} data.remark - 备注
   * @param {string} data.groupName - 分组名称
   */
  update(patentId, data) {
    return request({
      url: `/favorite/${patentId}`,
      method: 'put',
      data
    })
  },

  /**
   * 获取收藏数量
   */
  getCount() {
    return request({
      url: '/favorite/count',
      method: 'get'
    })
  }
}
