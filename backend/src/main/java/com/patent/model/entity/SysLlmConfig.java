package com.patent.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * LLM 配置实体
 * 支持用户自定义 API Key / BaseURL / 模型名称（两级配置机制）
 * - user_id = 0：系统默认配置（管理员管理）
 * - user_id > 0：用户自定义配置
 */
@Data
@Accessors(chain = true)
@TableName("sys_llm_config")
public class SysLlmConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID（0=系统默认，>0=用户自定义）
     */
    private Long userId;

    /**
     * 配置名称（如：通义千问、DeepSeek官方、Ollama本地）
     */
    private String configName;

    /**
     * LLM模式：online / offline
     */
    private String llmMode;

    /**
     * 自定义 API BaseURL（在线模式使用）
     */
    private String baseUrl;

    /**
     * API Key（建议加密存储）
     */
    private String apiKey;

    /**
     * 对话模型名称（ChatService 使用）
     */
    private String chatModel;

    /**
     * 分析模型名称（LlmService 使用 - 实体提取/匹配评估）
     */
    private String llmModel;

    /**
     * 向量嵌入模型名称（VectorService 使用）
     */
    private String embedModel;

    /**
     * Ollama 服务地址（离线模式使用）
     */
    private String ollamaUrl;

    /**
     * 是否当前启用（0=禁用，1=启用）
     */
    private Integer isActive;

    /**
     * 备注说明
     */
    private String remark;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /**
     * 逻辑删除（0=正常，1=已删除）
     */
    @TableLogic
    private Integer deleted;
}
