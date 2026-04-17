import request from './request'

/**
 * 知识图谱 API
 * 对应后端 /api/graph/* 接口
 */
export const graphApi = {
  /**
   * 获取某专利的图谱邻接节点与关系
   * GET /api/graph/patent/{publicationNo}
   */
  getPatentGraph(publicationNo) {
    return request({
      url: `/graph/patent/${encodeURIComponent(publicationNo)}`,
      method: 'get'
    })
  },

  /**
   * 获取某实体的关联图谱
   * GET /api/graph/entity/{entityName}
   */
  getEntityGraph(entityName) {
    return request({
      url: `/graph/entity/${encodeURIComponent(entityName)}`,
      method: 'get'
    })
  },

  /**
   * 获取 IPC 层次结构与关联专利
   * GET /api/graph/ipc?ipcCode=A61K9/70
   * 使用 params 而非 PathVariable，避免斜杠被 Tomcat 截断
   */
  getIpcGraph(ipcCode) {
    return request({
      url: '/graph/ipc',
      method: 'get',
      params: { ipcCode }
    })
  },

  /**
   * 通用图谱查询
   * POST /api/graph/query
   * @param {Object} params - { nodeType, relationType, depth, keyword, limit }
   */
  queryGraph(params) {
    return request({
      url: '/graph/query',
      method: 'post',
      data: params
    })
  },

}
