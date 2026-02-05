package com.patent.service;

import com.patent.model.dto.LoginDTO;
import com.patent.model.dto.RegisterDTO;
import com.patent.model.vo.LoginVO;
import com.patent.model.vo.UserVO;

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
}
