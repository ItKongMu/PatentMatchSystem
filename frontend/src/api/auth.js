import request from './request'

/**
 * 用户认证及用户管理相关API
 */
export const authApi = {
  // ==================== 基础认证 ====================

  /**
   * 用户登录
   */
  login(data) {
    return request({
      url: '/auth/login',
      method: 'post',
      data
    })
  },

  /**
   * 用户注册
   */
  register(data) {
    return request({
      url: '/auth/register',
      method: 'post',
      data
    })
  },

  /**
   * 退出登录
   */
  logout() {
    return request({
      url: '/auth/logout',
      method: 'post'
    })
  },

  /**
   * 获取当前用户信息
   */
  getUserInfo() {
    return request({
      url: '/auth/info',
      method: 'get'
    })
  },

  /**
   * 修改自己的密码（需要验证旧密码）
   */
  changePassword(data) {
    return request({
      url: '/auth/change-password',
      method: 'post',
      data
    })
  },

  // ==================== 管理员 - 用户管理 ====================

  /**
   * 获取用户管理统计面板数据（仅管理员）
   * @returns {{ totalUsers, onlineUsers, todayNewUsers, disabledUsers }}
   */
  getUserStats() {
    return request({
      url: '/auth/admin/stats',
      method: 'get'
    })
  },

  /**
   * 获取所有用户列表（含统计数据，仅管理员）
   */
  getAllUsers() {
    return request({
      url: '/auth/admin/users',
      method: 'get'
    })
  },

  /**
   * 管理员重置用户密码（无需旧密码）
   * @param {Object} data
   * @param {number} data.userId - 目标用户ID
   * @param {string} data.newPassword - 新密码
   * @param {string} data.confirmPassword - 确认新密码
   */
  adminResetPassword(data) {
    return request({
      url: '/auth/admin/reset-password',
      method: 'post',
      data
    })
  },

  /**
   * 踢出用户登录（使其Token立即失效）
   * @param {number} userId - 目标用户ID
   */
  kickoutUser(userId) {
    return request({
      url: `/auth/admin/kickout/${userId}`,
      method: 'post'
    })
  },

  /**
   * 禁用用户账号
   * @param {number} userId - 目标用户ID
   */
  disableUser(userId) {
    return request({
      url: `/auth/admin/disable/${userId}`,
      method: 'post'
    })
  },

  /**
   * 启用用户账号
   * @param {number} userId - 目标用户ID
   */
  enableUser(userId) {
    return request({
      url: `/auth/admin/enable/${userId}`,
      method: 'post'
    })
  },

  /**
   * 切换用户角色（admin ↔ user）
   * @param {Object} data
   * @param {number} data.userId - 目标用户ID
   * @param {string} data.role - 新角色：admin/user
   */
  updateUserRole(data) {
    return request({
      url: '/auth/admin/role',
      method: 'post',
      data
    })
  }
}
