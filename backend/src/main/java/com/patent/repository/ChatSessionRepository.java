package com.patent.repository;

import com.patent.model.entity.ChatSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 聊天会话 MongoDB Repository
 */
@Repository
public interface ChatSessionRepository extends MongoRepository<ChatSession, String> {
    
    /**
     * 根据会话ID查询会话
     */
    Optional<ChatSession> findBySessionId(String sessionId);
    
    /**
     * 查询用户的所有活跃会话（按更新时间倒序）
     */
    List<ChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status);
    
    /**
     * 分页查询用户的会话
     */
    Page<ChatSession> findByUserIdAndStatusOrderByUpdatedAtDesc(Long userId, String status, Pageable pageable);
    
    /**
     * 查询用户的所有会话（不限状态）
     */
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    /**
     * 根据会话ID删除会话
     */
    void deleteBySessionId(String sessionId);
    
    /**
     * 检查会话是否存在
     */
    boolean existsBySessionId(String sessionId);
    
    /**
     * 统计用户的会话数量
     */
    long countByUserIdAndStatus(Long userId, String status);
}
