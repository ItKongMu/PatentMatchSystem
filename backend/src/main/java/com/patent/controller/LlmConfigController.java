package com.patent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.patent.common.Result;
import com.patent.mapper.SysUserMapper;
import com.patent.model.dto.LlmConfigDTO;
import com.patent.model.entity.SysUser;
import com.patent.model.vo.LlmConfigVO;
import com.patent.service.LlmConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * LLM 配置管理接口
 * 提供用户自定义 API Key / BaseURL / 模型名称的 CRUD + 连接测试
 *
 * <p>权限规则：
 * <ul>
 *   <li>普通用户：可新增/修改/删除自己的离线和在线配置，不可修改系统默认配置</li>
 *   <li>管理员：可新增/修改/删除自己的配置，且可修改/删除系统默认配置</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/llm-config")
@RequiredArgsConstructor
@Tag(name = "LLM配置管理", description = "管理在线/离线 LLM 配置，支持自定义 API Key 和模型")
public class LlmConfigController {

    private final LlmConfigService llmConfigService;
    private final SysUserMapper sysUserMapper;

    // ==================== 公共接口 ====================

    /**
     * 获取系统当前 LLM 状态摘要
     * 无需登录，供前端展示当前模式
     */
    @GetMapping("/status")
    @Operation(summary = "获取系统LLM状态", description = "返回当前 LLM 模式及三模型配置信息")
    public Result<Map<String, Object>> getSystemStatus() {
        return Result.success(llmConfigService.getSystemStatus());
    }

    // ==================== 用户接口（需登录） ====================

    /**
     * 获取当前用户所有 LLM 配置列表
     * 包含：系统默认配置（userId=0）+ 用户自定义配置
     */
    @GetMapping("/list")
    @Operation(summary = "获取配置列表", description = "返回系统配置+当前用户的所有 LLM 配置（API Key 已脱敏）")
    public Result<List<LlmConfigVO>> listConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(llmConfigService.listConfigs(userId));
    }

    /**
     * 获取当前用户启用的配置
     */
    @GetMapping("/active")
    @Operation(summary = "获取当前启用配置", description = "返回用户当前激活的 LLM 配置，未设置则返回系统默认")
    public Result<LlmConfigVO> getActiveConfig() {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(llmConfigService.getActiveConfig(userId));
    }

    /**
     * 保存或更新 LLM 配置（离线 + 在线均支持）
     * <ul>
     *   <li>普通用户：只能保存自己的配置；系统配置不可修改</li>
     *   <li>管理员：可以保存自己的配置，也可通过 isSystemConfig=true 新增/修改系统配置</li>
     * </ul>
     */
    @PostMapping("/save")
    @Operation(summary = "保存配置", description = "新增或更新 LLM 配置（离线/在线均支持），id 为空表示新增")
    public Result<LlmConfigVO> saveConfig(@Valid @RequestBody LlmConfigDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = isCurrentUserAdmin(userId);
        return Result.success(llmConfigService.saveConfig(userId, isAdmin, dto));
    }

    /**
     * 激活指定配置（同时禁用其他配置）
     * 激活系统配置需管理员权限
     */
    @PutMapping("/{configId}/activate")
    @Operation(summary = "激活配置", description = "将指定配置设为启用，同时禁用该用户其他配置")
    public Result<Void> activateConfig(@PathVariable Long configId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = isCurrentUserAdmin(userId);
        llmConfigService.activateConfig(userId, isAdmin, configId);
        return Result.success();
    }

    /**
     * 删除配置
     * 删除系统配置需管理员权限
     */
    @DeleteMapping("/{configId}")
    @Operation(summary = "删除配置", description = "逻辑删除指定配置（当前启用的配置不可删除）")
    public Result<Void> deleteConfig(@PathVariable Long configId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = isCurrentUserAdmin(userId);
        llmConfigService.deleteConfig(userId, isAdmin, configId);
        return Result.success();
    }

    /**
     * 获取指定配置的明文 API Key（仅本人或管理员可调用）
     */
    @GetMapping("/{configId}/apikey")
    @Operation(summary = "查看明文API Key", description = "仅配置所有者或管理员可调用，返回解密后的 API Key")
    public Result<String> getPlainApiKey(@PathVariable Long configId) {
        Long userId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = isCurrentUserAdmin(userId);
        return Result.success(llmConfigService.getPlainApiKey(userId, isAdmin, configId));
    }

    /**
     * 测试 LLM 连接
     * 根据传入配置动态构建客户端并发送测试消息，不影响已保存配置
     */
    @PostMapping("/test")
    @Operation(summary = "测试连接", description = "使用提供的配置参数测试 LLM 连接（不保存，仅测试）")
    public Result<Map<String, Object>> testConnection(@RequestBody LlmConfigDTO dto) {
        return Result.success(llmConfigService.testConnection(dto));
    }

    // ==================== 管理员专用接口 ====================

    /**
     * 管理员接口：获取系统级配置列表（user_id=0）
     * 仅管理员可调用，权限在方法入口统一校验
     */
    @GetMapping("/system/list")
    @Operation(summary = "获取系统级配置列表", description = "仅管理员可用，获取 user_id=0 的系统默认配置")
    public Result<List<LlmConfigVO>> listSystemConfigs() {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!isCurrentUserAdmin(userId)) {
            return Result.forbidden("无权访问系统级配置");
        }
        return Result.success(llmConfigService.listConfigs(0L));
    }

    /**
     * 管理员接口：激活系统级配置
     * 仅管理员可调用，权限在方法入口统一校验
     */
    @PutMapping("/system/{configId}/activate")
    @Operation(summary = "激活系统级配置", description = "仅管理员可用，设置系统默认 LLM 配置")
    public Result<Void> activateSystemConfig(@PathVariable Long configId) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!isCurrentUserAdmin(userId)) {
            return Result.forbidden("无权激活系统级配置");
        }
        llmConfigService.activateConfig(userId, true, configId);
        return Result.success();
    }

    /**
     * 管理员接口：保存/更新系统级配置
     * 仅管理员可调用，权限在方法入口统一校验
     */
    @PostMapping("/system/save")
    @Operation(summary = "保存系统级配置", description = "仅管理员可用，新增或更新 user_id=0 的系统默认配置")
    public Result<LlmConfigVO> saveSystemConfig(@Valid @RequestBody LlmConfigDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!isCurrentUserAdmin(userId)) {
            return Result.forbidden("无权操作系统配置");
        }
        // 强制标记为系统配置
        dto.setIsSystemConfig(true);
        return Result.success(llmConfigService.saveConfig(userId, true, dto));
    }

    /**
     * 管理员接口：删除系统级配置
     * 仅管理员可调用，权限在方法入口统一校验
     */
    @DeleteMapping("/system/{configId}")
    @Operation(summary = "删除系统级配置", description = "仅管理员可用，删除 user_id=0 的系统默认配置")
    public Result<Void> deleteSystemConfig(@PathVariable Long configId) {
        Long userId = StpUtil.getLoginIdAsLong();
        if (!isCurrentUserAdmin(userId)) {
            return Result.forbidden("无权删除系统配置");
        }
        llmConfigService.deleteConfig(userId, true, configId);
        return Result.success();
    }

    // ==================== 私有工具方法 ====================

    /**
     * 判断当前用户是否为管理员
     */
    private boolean isCurrentUserAdmin(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user != null && "admin".equals(user.getRole());
    }
}
