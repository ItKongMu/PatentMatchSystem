package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM 配置响应 VO
 * API Key 脱敏处理后返回前端
 */
@Data
@Builder
@Schema(description = "LLM配置响应")
public class LlmConfigVO {

    @Schema(description = "配置ID")
    private Long id;

    @Schema(description = "用户ID（0=系统默认）")
    private Long userId;

    @Schema(description = "配置名称")
    private String configName;

    @Schema(description = "LLM模式：online/offline")
    private String llmMode;

    @Schema(description = "API BaseURL")
    private String baseUrl;

    @Schema(description = "API Key（已脱敏，如：sk-****xxxx）")
    private String apiKeyMasked;

    @Schema(description = "是否已配置API Key")
    private Boolean hasApiKey;

    @Schema(description = "对话模型名称")
    private String chatModel;

    @Schema(description = "分析模型名称")
    private String llmModel;

    @Schema(description = "向量嵌入模型名称")
    private String embedModel;

    @Schema(description = "Ollama服务地址")
    private String ollamaUrl;

    @Schema(description = "是否当前启用")
    private Boolean isActive;

    @Schema(description = "是否为系统默认配置（user_id=0），系统配置前端只读，不允许删除")
    private Boolean isSystemConfig;

    @Schema(description = "备注说明")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
