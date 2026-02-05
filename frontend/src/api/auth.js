import request from './request'

/**
 * 用户认证相关API
 */
export const authApi = {
  /**
   * 用户登录
   * @param {Object} data - 登录信息
   * @param {string} data.username - 用户名
   * @param {string} data.password - 密码
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
   * @param {Object} data - 注册信息
   * @param {string} data.username - 用户名
   * @param {string} data.password - 密码
   * @param {string} data.nickname - 昵称（可选）
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
  }
}
