package com.patent.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天会话实体
 * 存储在 MongoDB 的 chat_sessions 集合中
 */
@Data
@Document(collection = "chat_sessions")
@CompoundIndex(name = "user_updated_idx", def = "{'userId': 1, 'updatedAt': -1}")
public class ChatSession {
    
    /**
     * MongoDB 文档ID
     */
    @Id
    private String id;
    
    /**
     * 会话唯一标识（UUID）
     */
    @Indexed(unique = true)
    private String sessionId;
    
    /**
     * 关联用户ID（SysUser.id）
     */
    @Indexed
    private Long userId;
    
    /**
     * 会话标题（从首条消息生成）
     */
    private String title;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 最后更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 消息数量
     */
    private Integer messageCount;
    
    /**
     * 状态：active-活跃, archived-已归档, deleted-已删除
     */
    private String status;
    
    /**
     * 扩展元数据
     * 可存储：lastTopic（最后话题）、toolsUsed（使用的工具）等
     */
    private Map<String, Object> metadata;
}
