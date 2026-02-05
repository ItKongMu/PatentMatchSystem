package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天消息 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "聊天消息信息")
public class ChatMessageVO {

    @Schema(description = "消息ID")
    private String id;

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "消息角色: user-用户, assistant-AI助手, system-系统")
    private String role;

    @Schema(description = "消息内容")
    private String content;

    @Schema(description = "消息时间")
    private LocalDateTime timestamp;

    @Schema(description = "消息序号")
    private Integer sequence;

    @Schema(description = "扩展元数据")
    private Map<String, Object> metadata;
}
