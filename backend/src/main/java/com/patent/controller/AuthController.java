package com.patent.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.patent.common.Result;
import com.patent.model.dto.AdminResetPasswordDTO;
import com.patent.model.dto.ChangePasswordDTO;
import com.patent.model.dto.LoginDTO;
import com.patent.model.dto.RegisterDTO;
import com.patent.model.dto.UpdateUserRoleDTO;
import com.patent.model.vo.LoginVO;
import com.patent.model.vo.UserStatsVO;
import com.patent.model.vo.UserVO;
import com.patent.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 认证控制器（含用户管理）
 */
@Tag(name = "认证管理", description = "用户登录、注册、登出、用户管理等接口")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==================== 基础认证 ====================

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserVO> register(@Valid @RequestBody RegisterDTO dto) {
        return Result.success("注册成功", authService.register(dto));
    }

    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    @Operation(summary = "用户登出")
    @SaCheckLogin
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success("登出成功", null);
    }

    @Operation(summary = "获取当前用户信息")
    @SaCheckLogin
    @GetMapping("/info")
    public Result<UserVO> getCurrentUser() {
        return Result.success(authService.getCurrentUser());
    }

    @Operation(summary = "修改自己的密码（需验证旧密码）")
    @SaCheckLogin
    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success("密码修改成功", null);
    }

    // ==================== 管理员 - 用户管理 ====================

    @Operation(summary = "获取用户管理统计面板（仅管理员）")
    @SaCheckLogin
    @GetMapping("/admin/stats")
    public Result<UserStatsVO> getUserStats() {
        return Result.success(authService.getUserStats());
    }

    @Operation(summary = "获取所有用户列表（含统计数据，仅管理员）")
    @SaCheckLogin
    @GetMapping("/admin/users")
    public Result<List<UserVO>> getAllUsers() {
        return Result.success(authService.getAllUsers());
    }

    @Operation(summary = "管理员重置用户密码（无需旧密码）")
    @SaCheckLogin
    @PostMapping("/admin/reset-password")
    public Result<Void> adminResetPassword(@Valid @RequestBody AdminResetPasswordDTO dto) {
        authService.adminResetPassword(dto);
        return Result.success("密码重置成功", null);
    }

    @Operation(summary = "踢出用户登录（使Token立即失效）")
    @SaCheckLogin
    @PostMapping("/admin/kickout/{userId}")
    public Result<Void> kickoutUser(
            @Parameter(description = "目标用户ID") @PathVariable Long userId) {
        authService.kickoutUser(userId);
        return Result.success("已踢出用户登录", null);
    }

    @Operation(summary = "禁用用户账号")
    @SaCheckLogin
    @PostMapping("/admin/disable/{userId}")
    public Result<Void> disableUser(
            @Parameter(description = "目标用户ID") @PathVariable Long userId) {
        authService.updateUserStatus(userId, 0);
        return Result.success("用户账号已禁用", null);
    }

    @Operation(summary = "启用用户账号")
    @SaCheckLogin
    @PostMapping("/admin/enable/{userId}")
    public Result<Void> enableUser(
            @Parameter(description = "目标用户ID") @PathVariable Long userId) {
        authService.updateUserStatus(userId, 1);
        return Result.success("用户账号已启用", null);
    }

    @Operation(summary = "切换用户角色（admin ↔ user）")
    @SaCheckLogin
    @PostMapping("/admin/role")
    public Result<Void> updateUserRole(@Valid @RequestBody UpdateUserRoleDTO dto) {
        authService.updateUserRole(dto);
        return Result.success("角色切换成功", null);
    }
}
