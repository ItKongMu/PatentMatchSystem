package com.patent.repository;

import com.patent.model.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 聊天消息 MongoDB Repository
 */
@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    
    /**
     * 查询会话的所有消息（按序号升序）
     */
    List<ChatMessage> findBySessionIdOrderBySequenceAsc(String sessionId);
    
    /**
     * 分页查询会话的消息（按序号倒序）
     */
    Page<ChatMessage> findBySessionIdOrderBySequenceDesc(String sessionId, Pageable pageable);
    
    /**
     * 查询会话的最新N条消息（按序号倒序）
     */
    List<ChatMessage> findTop20BySessionIdOrderBySequenceDesc(String sessionId);
    
    /**
     * 根据会话ID删除所有消息
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 统计会话的消息数量
     */
    long countBySessionId(String sessionId);
    
    /**
     * 查询会话中指定角色的消息
     */
    List<ChatMessage> findBySessionIdAndRoleOrderBySequenceAsc(String sessionId, String role);
    
    /**
     * 查询会话的最后一条消息
     */
    ChatMessage findFirstBySessionIdOrderBySequenceDesc(String sessionId);
}
