package com.patent.service;

import com.patent.model.dto.ChatMessageDTO;
import com.patent.model.vo.ChatMessageVO;
import com.patent.model.vo.ChatResponseVO;
import com.patent.model.vo.ChatSessionVO;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * 对话式检索服务接口
 * 基于 LLM Function Calling 实现智能专利检索对话
 */
public interface ChatService {

    /**
     * 处理用户对话消息（同步模式）
     *
     * @param dto 用户消息
     * @return AI回复（包含检索结果）
     */
    ChatResponseVO chat(ChatMessageDTO dto);

    /**
     * 处理用户对话消息（流式模式）
     *
     * @param dto 用户消息
     * @return AI回复流（SSE事件流）
     */
    Flux<String> chatStream(ChatMessageDTO dto);

    /**
     * 处理用户对话消息（流式模式，使用标准 ServerSentEvent）
     *
     * @param dto 用户消息
     * @return AI回复流（标准SSE事件流）
     */
    Flux<ServerSentEvent<String>> chatStreamSSE(ChatMessageDTO dto);

    /**
     * 清除会话历史
     *
     * @param sessionId 会话ID
     */
    void clearSession(String sessionId);

    // ==================== 会话管理接口 ====================

    /**
     * 获取用户的会话列表
     *
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 会话列表
     */
    List<ChatSessionVO> getUserSessions(Long userId, Integer page, Integer size);

    /**
     * 获取会话的消息历史
     *
     * @param sessionId 会话ID
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 消息列表
     */
    List<ChatMessageVO> getSessionMessages(String sessionId, Integer page, Integer size);

    /**
     * 删除会话
     *
     * @param sessionId 会话ID
     * @param userId 用户ID（用于权限校验）
     */
    void deleteSession(String sessionId, Long userId);

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param title 新标题
     * @param userId 用户ID（用于权限校验）
     */
    void updateSessionTitle(String sessionId, String title, Long userId);

    /**
     * 归档会话
     *
     * @param sessionId 会话ID
     * @param userId 用户ID（用于权限校验）
     */
    void archiveSession(String sessionId, Long userId);

    /**
     * 获取用户的已归档会话列表
     *
     * @param userId 用户ID
     * @param page 页码（从1开始）
     * @param size 每页数量
     * @return 已归档会话列表
     */
    List<ChatSessionVO> getArchivedSessions(Long userId, Integer page, Integer size);

    /**
     * 恢复已归档的会话
     *
     * @param sessionId 会话ID
     * @param userId 用户ID（用于权限校验）
     */
    void restoreSession(String sessionId, Long userId);
}
