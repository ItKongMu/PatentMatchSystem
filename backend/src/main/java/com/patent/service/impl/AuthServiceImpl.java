package com.patent.service.impl;

import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.patent.common.exception.BusinessException;
import com.patent.mapper.PatentFavoriteMapper;
import com.patent.mapper.PatentMapper;
import com.patent.mapper.SysUserMapper;
import com.patent.model.dto.AdminResetPasswordDTO;
import com.patent.model.dto.ChangePasswordDTO;
import com.patent.model.dto.LoginDTO;
import com.patent.model.dto.RegisterDTO;
import com.patent.model.dto.UpdateUserRoleDTO;
import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentFavorite;
import com.patent.model.entity.SysUser;
import com.patent.model.vo.LoginVO;
import com.patent.model.vo.UserStatsVO;
import com.patent.model.vo.UserVO;
import com.patent.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final PatentMapper patentMapper;
    private final PatentFavoriteMapper patentFavoriteMapper;

    @Override
    public UserVO register(RegisterDTO dto) {
        SysUser existUser = userMapper.selectByUsername(dto.getUsername());
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

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
        SysUser user = userMapper.selectByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        if (!BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }

        if (user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系管理员");
        }

        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();

        log.info("用户登录成功: {}", dto.getUsername());

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

    @Override
    public void changePassword(ChangePasswordDTO dto) {
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("新密码与确认密码不一致");
        }

        Long userId = getCurrentUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        if (!BCrypt.checkpw(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }

        if (BCrypt.checkpw(dto.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与旧密码相同");
        }

        SysUser updateUser = new SysUser();
        updateUser.setId(userId);
        updateUser.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userMapper.updateById(updateUser);

        log.info("用户修改密码成功: {}", user.getUsername());
    }

    @Override
    public void adminResetPassword(AdminResetPasswordDTO dto) {
        checkAdmin();

        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            throw new BusinessException("新密码与确认密码不一致");
        }

        SysUser targetUser = userMapper.selectById(dto.getUserId());
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        SysUser updateUser = new SysUser();
        updateUser.setId(dto.getUserId());
        updateUser.setPassword(BCrypt.hashpw(dto.getNewPassword()));
        userMapper.updateById(updateUser);

        log.info("管理员 {} 重置了用户 {} 的密码", getCurrentUserId(), targetUser.getUsername());
    }

    @Override
    public List<UserVO> getAllUsers() {
        checkAdmin();

        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().orderByAsc(SysUser::getId)
        );

        return users.stream().map(user -> {
            UserVO vo = convertToVO(user);
            // 在线状态
            boolean online = StpUtil.isLogin(user.getId());
            vo.setOnline(online);
            // Token剩余有效时间（先获取该用户的token值，再查询其active超时）
            if (online) {
                try {
                    String tokenValue = StpUtil.getTokenValueByLoginId(user.getId());
                    long timeout = StpUtil.getTokenTimeout(tokenValue);
                    vo.setTokenTimeout(timeout < 0 ? -1L : timeout);
                } catch (Exception e) {
                    vo.setTokenTimeout(-1L);
                }
            } else {
                vo.setTokenTimeout(-1L);
            }
            // 上传专利数
            Long patentCount = patentMapper.selectCount(
                    new LambdaQueryWrapper<Patent>().eq(Patent::getCreatedBy, user.getId())
            );
            vo.setPatentCount(patentCount);
            // 收藏专利数
            Long favoriteCount = patentFavoriteMapper.selectCount(
                    new LambdaQueryWrapper<PatentFavorite>().eq(PatentFavorite::getUserId, user.getId())
            );
            vo.setFavoriteCount(favoriteCount);
            // 匹配次数（用null先占位，通过专用mapper统计）
            vo.setMatchCount(userMapper.countMatchRecordsByUserId(user.getId()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void kickoutUser(Long userId) {
        checkAdmin();

        SysUser targetUser = userMapper.selectById(userId);
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        // 防止管理员踢出自己
        Long currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId)) {
            throw new BusinessException("不能踢出自己");
        }

        StpUtil.kickout(userId);
        log.info("管理员 {} 踢出了用户 {} 的登录", currentUserId, targetUser.getUsername());
    }

    @Override
    public void updateUserStatus(Long userId, Integer status) {
        checkAdmin();

        SysUser targetUser = userMapper.selectById(userId);
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        // 防止禁用自身
        Long currentUserId = getCurrentUserId();
        if (userId.equals(currentUserId) && status == 0) {
            throw new BusinessException("不能禁用自己的账号");
        }

        userMapper.update(null,
                new LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .set(SysUser::getStatus, status)
        );

        // 禁用时同时踢出登录
        if (status == 0 && StpUtil.isLogin(userId)) {
            StpUtil.kickout(userId);
        }

        log.info("管理员 {} 将用户 {} 状态设置为 {}", currentUserId, targetUser.getUsername(), status == 1 ? "启用" : "禁用");
    }

    @Override
    public void updateUserRole(UpdateUserRoleDTO dto) {
        checkAdmin();

        SysUser targetUser = userMapper.selectById(dto.getUserId());
        if (targetUser == null) {
            throw new BusinessException("目标用户不存在");
        }

        // 防止管理员修改自身角色
        Long currentUserId = getCurrentUserId();
        if (dto.getUserId().equals(currentUserId)) {
            throw new BusinessException("不能修改自己的角色");
        }

        userMapper.update(null,
                new LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, dto.getUserId())
                        .set(SysUser::getRole, dto.getRole())
        );

        log.info("管理员 {} 将用户 {} 角色切换为 {}", currentUserId, targetUser.getUsername(), dto.getRole());
    }

    @Override
    public UserStatsVO getUserStats() {
        checkAdmin();

        UserStatsVO stats = new UserStatsVO();

        // 总用户数
        stats.setTotalUsers(userMapper.selectCount(null));

        // 当前在线人数
        int onlineCount = (int) userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().select(SysUser::getId)
        ).stream().filter(u -> StpUtil.isLogin(u.getId())).count();
        stats.setOnlineUsers(onlineCount);

        // 今日新增用户数
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long todayNew = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().ge(SysUser::getCreatedAt, todayStart)
        );
        stats.setTodayNewUsers(todayNew);

        // 禁用账号数
        Long disabledCount = userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 0)
        );
        stats.setDisabledUsers(disabledCount);

        return stats;
    }

    // ==================== 私有方法 ====================

    /**
     * 校验当前用户是否为管理员
     */
    private void checkAdmin() {
        Long currentUserId = getCurrentUserId();
        SysUser currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || !"admin".equals(currentUser.getRole())) {
            throw new BusinessException("无权限执行此操作");
        }
    }

    private UserVO convertToVO(SysUser user) {
        UserVO vo = new UserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
}
