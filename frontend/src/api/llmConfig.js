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
 * 获取当前用户所有配置列表（含系统配置+用户自定义配置）
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
 * 保存或更新配置（用户自定义：离线/在线均支持）
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

/**
 * 保存/更新系统级配置（管理员用）
 * @param {Object} data - 配置数据
 */
export const saveSystemConfig = (data) => {
  return request.post('/llm-config/system/save', data)
}

/**
 * 删除系统级配置（管理员用）
 * @param {number} configId - 配置ID
 */
export const deleteSystemConfig = (configId) => {
  return request.delete(`/llm-config/system/${configId}`)
}

/**
 * 获取指定配置的明文 API Key（仅本人或管理员）
 * @param {number} configId - 配置ID
 */
export const getPlainApiKey = (configId) => {
  return request.get(`/llm-config/${configId}/apikey`)
}
