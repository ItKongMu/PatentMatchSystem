import request from './request'

/**
 * LLM 配置管理 API
 */

/**
 * 获取系统 LLM 状态（无需登录）
 */
export const getSystemStatus = () => {
  return request.get('/llm-config/status')
}

/**
 * 获取当前用户所有配置列表
 */
export const listConfigs = () => {
  return request.get('/llm-config/list')
}

/**
 * 获取当前用户启用的配置
 */
export const getActiveConfig = () => {
  return request.get('/llm-config/active')
}

/**
 * 保存或更新配置
 * @param {Object} data - 配置数据
 */
export const saveConfig = (data) => {
  return request.post('/llm-config/save', data)
}

/**
 * 激活指定配置
 * @param {number} configId - 配置ID
 */
export const activateConfig = (configId) => {
  return request.put(`/llm-config/${configId}/activate`)
}

/**
 * 删除配置
 * @param {number} configId - 配置ID
 */
export const deleteConfig = (configId) => {
  return request.delete(`/llm-config/${configId}`)
}

/**
 * 测试 LLM 连接
 * @param {Object} data - 配置参数（不保存，仅测试）
 */
export const testConnection = (data) => {
  return request.post('/llm-config/test', data)
}

/**
 * 获取系统级配置列表（管理员用）
 */
export const listSystemConfigs = () => {
  return request.get('/llm-config/system/list')
}

/**
 * 激活系统级配置（管理员用）
 * @param {number} configId - 配置ID
 */
export const activateSystemConfig = (configId) => {
  return request.put(`/llm-config/system/${configId}/activate`)
}
