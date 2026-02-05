package com.patent.model.entity;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天消息实体
 * 存储在 MongoDB 的 chat_messages 集合中
 */
@Data
@Document(collection = "chat_messages")
@CompoundIndexes({
    @CompoundIndex(name = "session_sequence_idx", def = "{'sessionId': 1, 'sequence': 1}"),
    @CompoundIndex(name = "session_time_idx", def = "{'sessionId': 1, 'timestamp': -1}")
})
public class ChatMessage {
    
    /**
     * MongoDB 文档ID
     */
    @Id
    private String id;
    
    /**
     * 关联会话ID
     */
    private String sessionId;
    
    /**
     * 冗余用户ID（便于查询）
     */
    private Long userId;
    
    /**
     * 消息角色：user-用户, assistant-AI助手, system-系统
     */
    private String role;
    
    /**
     * 消息内容
     */
    private String content;
    
    /**
     * 消息时间
     */
    private LocalDateTime timestamp;
    
    /**
     * 消息序号（用于排序）
     */
    private Integer sequence;
    
    /**
     * 扩展元数据
     * 可存储：toolCalls（工具调用记录）、patents（关联专利结果）、tokens（Token消耗）等
     */
    private Map<String, Object> metadata;
}
