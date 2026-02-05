package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 对话消息 DTO
 */
@Data
@Schema(description = "对话消息请求")
public class ChatMessageDTO {

    @NotBlank(message = "消息内容不能为空")
    @Size(min = 2, max = 2000, message = "消息长度应在2-2000字符之间")
    @Schema(description = "用户消息内容", example = "帮我找一下关于深度学习图像识别的专利")
    private String message;

    @Schema(description = "会话ID，用于保持上下文", example = "uuid-xxx-xxx")
    private String sessionId;

    @Schema(description = "是否启用流式响应", example = "false")
    private Boolean stream = false;
}
