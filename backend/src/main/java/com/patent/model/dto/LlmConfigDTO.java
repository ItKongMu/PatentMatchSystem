package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * LLM 配置保存/更新 DTO
 */
@Data
@Schema(description = "LLM配置保存请求")
public class LlmConfigDTO {

    @Schema(description = "配置ID（新增时为空，更新时必填）")
    private Long id;

    @NotBlank(message = "配置名称不能为空")
    @Schema(description = "配置名称，如：通义千问、DeepSeek官方", example = "通义千问")
    private String configName;

    @NotBlank(message = "LLM模式不能为空")
    @Pattern(regexp = "^(online|offline)$", message = "LLM模式只能为 online 或 offline")
    @Schema(description = "LLM模式：online/offline", example = "online")
    private String llmMode;

    @Schema(description = "API BaseURL（在线模式必填）", example = "https://dashscope.aliyuncs.com/compatible-mode")
    private String baseUrl;

    @Schema(description = "API Key（在线模式必填，传空字符串表示使用系统默认）", example = "sk-xxxx")
    private String apiKey;

    @Schema(description = "对话模型名称（ChatService使用）", example = "qwen-max")
    private String chatModel;

    @Schema(description = "分析模型名称（LlmService使用）", example = "qwen-plus")
    private String llmModel;

    @Schema(description = "向量嵌入模型名称（VectorService使用）", example = "text-embedding-v3")
    private String embedModel;

    @Schema(description = "Ollama服务地址（离线模式使用）", example = "http://localhost:11434")
    private String ollamaUrl;

    @Schema(description = "备注说明", example = "生产环境高质量配置")
    private String remark;

    @Schema(description = "是否作为系统默认配置（仅管理员新增时有效，true=userId=0）")
    private Boolean isSystemConfig;
}
