package com.patent.service;

import com.patent.model.dto.LlmConfigDTO;
import com.patent.model.vo.LlmConfigVO;

import java.util.List;
import java.util.Map;

/**
 * LLM 配置管理服务
 */
public interface LlmConfigService {

    /**
     * 获取当前用户的所有 LLM 配置列表
     *
     * @param userId 用户ID
     */
    List<LlmConfigVO> listConfigs(Long userId);

    /**
     * 获取当前用户启用的配置
     *
     * @param userId 用户ID
     */
    LlmConfigVO getActiveConfig(Long userId);

    /**
     * 保存或更新 LLM 配置
     *
     * @param userId 用户ID
     * @param dto    配置信息
     * @return 保存后的配置
     */
    LlmConfigVO saveConfig(Long userId, boolean isAdmin, LlmConfigDTO dto);

    /**
     * 激活指定配置（同时禁用该用户其他配置）
     *
     * @param userId   用户ID
     * @param isAdmin  是否管理员
     * @param configId 配置ID
     */
    void activateConfig(Long userId, boolean isAdmin, Long configId);

    /**
     * 删除配置
     *
     * @param userId   用户ID
     * @param isAdmin  是否管理员
     * @param configId 配置ID
     */
    void deleteConfig(Long userId, boolean isAdmin, Long configId);

    /**
     * 测试 LLM 连接
     * 根据传入配置动态构建客户端并发送测试消息
     *
     * @param dto 配置信息（未保存的临时测试）
     * @return 测试结果，包含是否成功、响应时间、错误信息
     */
    Map<String, Object> testConnection(LlmConfigDTO dto);

    /**
     * 获取系统当前生效的 LLM 配置摘要
     * （供前端展示当前模式状态）
     */
    Map<String, Object> getSystemStatus();
}
