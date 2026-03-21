package com.patent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.patent.common.Result;
import com.patent.model.dto.LlmConfigDTO;
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
 */
@Slf4j
@RestController
@RequestMapping("/api/llm-config")
@RequiredArgsConstructor
@Tag(name = "LLM配置管理", description = "管理在线/离线 LLM 配置，支持自定义 API Key 和模型")
public class LlmConfigController {

    private final LlmConfigService llmConfigService;

    /**
     * 获取系统当前 LLM 状态摘要
     * 无需登录，供前端展示当前模式
     */
    @GetMapping("/status")
    @Operation(summary = "获取系统LLM状态", description = "返回当前 LLM 模式及三模型配置信息")
    public Result<Map<String, Object>> getSystemStatus() {
        return Result.success(llmConfigService.getSystemStatus());
    }

    /**
     * 获取当前用户所有 LLM 配置列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取配置列表", description = "返回当前用户的所有 LLM 配置（API Key 已脱敏）")
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
     * 保存或更新 LLM 配置
     */
    @PostMapping("/save")
    @Operation(summary = "保存配置", description = "新增或更新 LLM 配置，id 为空表示新增")
    public Result<LlmConfigVO> saveConfig(@Valid @RequestBody LlmConfigDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.success(llmConfigService.saveConfig(userId, dto));
    }

    /**
     * 激活指定配置（同时禁用其他配置）
     */
    @PutMapping("/{configId}/activate")
    @Operation(summary = "激活配置", description = "将指定配置设为启用，同时禁用该用户其他配置")
    public Result<Void> activateConfig(@PathVariable Long configId) {
        Long userId = StpUtil.getLoginIdAsLong();
        llmConfigService.activateConfig(userId, configId);
        return Result.success();
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/{configId}")
    @Operation(summary = "删除配置", description = "逻辑删除指定配置（当前启用的配置不可删除）")
    public Result<Void> deleteConfig(@PathVariable Long configId) {
        Long userId = StpUtil.getLoginIdAsLong();
        llmConfigService.deleteConfig(userId, configId);
        return Result.success();
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

    /**
     * 管理员接口：获取系统级配置列表（user_id=0）
     */
    @GetMapping("/system/list")
    @Operation(summary = "获取系统级配置列表", description = "仅管理员可用，获取 user_id=0 的系统默认配置")
    public Result<List<LlmConfigVO>> listSystemConfigs() {
        // 系统级配置使用 user_id=0
        return Result.success(llmConfigService.listConfigs(0L));
    }

    /**
     * 管理员接口：激活系统级配置
     */
    @PutMapping("/system/{configId}/activate")
    @Operation(summary = "激活系统级配置", description = "仅管理员可用，设置系统默认 LLM 配置")
    public Result<Void> activateSystemConfig(@PathVariable Long configId) {
        llmConfigService.activateConfig(0L, configId);
        return Result.success();
    }
}
