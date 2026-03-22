package com.patent.service;

import com.patent.model.dto.AdminResetPasswordDTO;
import com.patent.model.dto.ChangePasswordDTO;
import com.patent.model.dto.LoginDTO;
import com.patent.model.dto.RegisterDTO;
import com.patent.model.dto.UpdateUserRoleDTO;
import com.patent.model.vo.LoginVO;
import com.patent.model.vo.UserStatsVO;
import com.patent.model.vo.UserVO;

import java.util.List;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户注册
     */
    UserVO register(RegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 用户登出
     */
    void logout();

    /**
     * 获取当前用户信息
     */
    UserVO getCurrentUser();

    /**
     * 获取当前用户ID
     */
    Long getCurrentUserId();

    /**
     * 修改自己的密码（需要旧密码验证）
     */
    void changePassword(ChangePasswordDTO dto);

    /**
     * 管理员重置指定用户的密码（无需旧密码）
     */
    void adminResetPassword(AdminResetPasswordDTO dto);

    /**
     * 获取所有用户列表（含统计数据，仅管理员）
     */
    List<UserVO> getAllUsers();

    /**
     * 踢出用户登录（使其Token立即失效，仅管理员）
     */
    void kickoutUser(Long userId);

    /**
     * 禁用或启用用户账号（仅管理员）
     *
     * @param userId 目标用户ID
     * @param status 1-启用 0-禁用
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 切换用户角色（admin ↔ user，仅管理员）
     */
    void updateUserRole(UpdateUserRoleDTO dto);

    /**
     * 获取用户管理统计面板数据（仅管理员）
     */
    UserStatsVO getUserStats();
}
