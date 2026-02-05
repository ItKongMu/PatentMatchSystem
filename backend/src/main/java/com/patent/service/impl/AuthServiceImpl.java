package com.patent.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.patent.common.exception.BusinessException;
import com.patent.mapper.SysUserMapper;
import com.patent.model.dto.LoginDTO;
import com.patent.model.dto.RegisterDTO;
import com.patent.model.entity.SysUser;
import com.patent.model.vo.LoginVO;
import com.patent.model.vo.UserVO;
import com.patent.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;

    @Override
    public UserVO register(RegisterDTO dto) {
        // 检查用户名是否已存在
        SysUser existUser = userMapper.selectByUsername(dto.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        // 创建用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(BCrypt.hashpw(dto.getPassword()));
        user.setNickname(dto.getNickname() != null ? dto.getNickname() : dto.getUsername());
        user.setRole("user");
        user.setStatus(1);

        userMapper.insert(user);
        log.info("用户注册成功: {}", dto.getUsername());

        return convertToVO(user);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 查询用户
        SysUser user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        // 验证密码
        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        // 检查状态
        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用");
        }

        // 登录
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        log.info("用户登录成功: {}", dto.getUsername());

        // 构建响应
        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserInfo(convertToVO(user));

        return vo;
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public UserVO getCurrentUser() {
        Long userId = getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToVO(user);
    }

    @Override
    public Long getCurrentUserId() {
        return StpUtil.getLoginIdAsLong();
    }

    private UserVO convertToVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
