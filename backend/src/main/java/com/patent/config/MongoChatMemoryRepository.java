package com.patent.config;

import com.patent.model.entity.ChatMessage;
import com.patent.model.entity.ChatSession;
import com.patent.repository.ChatMessageRepository;
import com.patent.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * MongoDB 实现的 ChatMemoryRepository
 * 用于持久化聊天记忆到 MongoDB
 * 
 * 实现 Spring AI 的 ChatMemoryRepository 接口，
 * 使聊天历史能够持久化存储，支持服务重启后恢复会话
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MongoChatMemoryRepository implements ChatMemoryRepository {
    
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    
    /**
     * 用于临时存储当前用户ID（通过 ThreadLocal 传递）
     * 在 ChatService 中调用前设置，调用后清除
     */
    private static final ThreadLocal<Long> currentUserId = new ThreadLocal<>();
    
    /**
     * 用于存储会话ID到用户ID的映射（解决WebFlux跨线程问题）
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private static final java.util.concurrent.ConcurrentHashMap<String, Long> sessionUserMap = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * 设置当前请求的用户ID
     */
    public static void setCurrentUserId(Long userId) {
        currentUserId.set(userId);
    }
    
    /**
     * 清除当前请求的用户ID
     */
    public static void clearCurrentUserId() {
        currentUserId.remove();
    }
    
    /**
     * 获取当前请求的用户ID
     */
    public static Long getCurrentUserId() {
        return currentUserId.get();
    }
    
    /**
     * 静态 logger，用于静态方法中的日志输出
     */
    private static final org.slf4j.Logger staticLog = org.slf4j.LoggerFactory.getLogger(MongoChatMemoryRepository.class);
    
    /**
     * 注册会话ID与用户ID的映射（解决WebFlux跨线程问题）
     */
    public static void registerSessionUser(String sessionId, Long userId) {
        if (sessionId != null && userId != null) {
            sessionUserMap.put(sessionId, userId);
            staticLog.debug("注册会话用户映射: sessionId={}, userId={}", sessionId, userId);
        }
    }
    
    /**
     * 获取会话对应的用户ID
     */
    public static Long getSessionUserId(String sessionId) {
        return sessionUserMap.get(sessionId);
    }
    
    /**
     * 清除会话用户映射
     */
    public static void clearSessionUser(String sessionId) {
        if (sessionId != null) {
            sessionUserMap.remove(sessionId);
        }
    }
    
    /**
     * 获取所有会话ID列表
     * Spring AI ChatMemoryRepository 接口要求实现此方法
     */
    @Override
    public List<String> findConversationIds() {
        log.debug("查询所有会话ID");
        
        // 获取当前用户ID，如果有的话只返回当前用户的会话
        Long userId = currentUserId.get();
        
        List<ChatSession> sessions;
        if (userId != null) {
            // 只返回当前用户的活跃会话
            sessions = sessionRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(userId, "active");
        } else {
            // 返回所有会话
            sessions = sessionRepository.findAll();
        }
        
        List<String> conversationIds = sessions.stream()
                .map(ChatSession::getSessionId)
                .collect(Collectors.toList());
        
        log.debug("找到 {} 个会话", conversationIds.size());
        return conversationIds;
    }
    
    /**
     * 根据会话ID查询消息历史
     * Spring AI 在构建上下文时会调用此方法
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        log.debug("查询会话消息: conversationId={}", conversationId);
        
        List<ChatMessage> messages = messageRepository.findBySessionIdOrderBySequenceAsc(conversationId);
        
        if (messages.isEmpty()) {
            log.debug("会话 {} 无历史消息", conversationId);
            return Collections.emptyList();
        }
        
        List<Message> result = messages.stream()
                .map(this::toSpringAiMessage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        log.debug("会话 {} 加载了 {} 条历史消息", conversationId, result.size());
        return result;
    }
    
    /**
     * 保存消息到指定会话
     * Spring AI 在对话完成后会调用此方法保存新消息
     * 
     * 注意：Spring AI 的 MessageWindowChatMemory 会传入完整的消息列表（包含历史和新消息），
     * 我们需要智能处理，只保存新增的消息
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            log.debug("没有消息需要保存: conversationId={}", conversationId);
            return;
        }
        
        // 优先从 ThreadLocal 获取，如果为空则从会话映射中获取（解决WebFlux跨线程问题）
        Long userId = currentUserId.get();
        if (userId == null) {
            userId = sessionUserMap.get(conversationId);
        }
        log.debug("保存消息: conversationId={}, userId={}, messageCount={}", conversationId, userId, messages.size());
        
        // 确保会话存在
        final Long finalUserId = userId;
        ChatSession session = sessionRepository.findBySessionId(conversationId)
                .map(existingSession -> {
                    // 如果现有会话的 userId 为空但当前有用户ID，则更新
                    if (existingSession.getUserId() == null && finalUserId != null) {
                        existingSession.setUserId(finalUserId);
                        return sessionRepository.save(existingSession);
                    }
                    return existingSession;
                })
                .orElseGet(() -> createNewSession(conversationId, finalUserId));
        
        // 获取当前已存储的消息数量
        long existingCount = messageRepository.countBySessionId(conversationId);
        
        // 只保存新增的消息（跳过已存在的）
        int newMessageCount = 0;
        for (int i = (int) existingCount; i < messages.size(); i++) {
            Message message = messages.get(i);
            
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setSessionId(conversationId);
            chatMessage.setUserId(userId);
            chatMessage.setRole(message.getMessageType().getValue());
            chatMessage.setContent(message.getText());
            chatMessage.setTimestamp(LocalDateTime.now());
            chatMessage.setSequence(i + 1);
            
            // 保存消息元数据
            if (message.getMetadata() != null && !message.getMetadata().isEmpty()) {
                chatMessage.setMetadata(new HashMap<>(message.getMetadata()));
            }
            
            messageRepository.save(chatMessage);
            newMessageCount++;
        }
        
        if (newMessageCount > 0) {
            // 更新会话信息
            session.setUpdatedAt(LocalDateTime.now());
            session.setMessageCount((int) (existingCount + newMessageCount));
            
            // 如果会话还没有标题，从第一条用户消息生成
            if (session.getTitle() == null || session.getTitle().isEmpty()) {
                messages.stream()
                        .filter(m -> m.getMessageType() == MessageType.USER)
                        .findFirst()
                        .ifPresent(m -> {
                            String title = m.getText();
                            if (title != null) {
                                if (title.length() > 50) {
                                    title = title.substring(0, 50) + "...";
                                }
                                session.setTitle(title);
                            }
                        });
            }
            
            sessionRepository.save(session);
            log.info("保存了 {} 条新消息到会话 {}", newMessageCount, conversationId);
        }
    }
    
    /**
     * 删除指定会话的所有消息
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        log.info("删除会话: conversationId={}", conversationId);
        
        // 删除所有消息
        messageRepository.deleteBySessionId(conversationId);
        
        // 删除会话记录
        sessionRepository.deleteBySessionId(conversationId);
        
        log.info("已删除会话及其所有消息: {}", conversationId);
    }
    
    /**
     * 创建新会话
     */
    private ChatSession createNewSession(String sessionId, Long userId) {
        log.info("创建新会话: sessionId={}, userId={}", sessionId, userId);
        
        ChatSession session = new ChatSession();
        session.setSessionId(sessionId);
        session.setUserId(userId);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        session.setMessageCount(0);
        session.setStatus("active");
        session.setMetadata(new HashMap<>());
        
        return sessionRepository.save(session);
    }
    
    /**
     * 将 MongoDB 存储的消息转换为 Spring AI Message
     */
    private Message toSpringAiMessage(ChatMessage chatMessage) {
        if (chatMessage == null || chatMessage.getContent() == null) {
            return null;
        }
        
        String role = chatMessage.getRole();
        String content = chatMessage.getContent();
        
        return switch (role) {
            case "user" -> new UserMessage(content);
            case "assistant" -> new AssistantMessage(content);
            case "system" -> new SystemMessage(content);
            default -> {
                log.warn("未知的消息角色: {}, 默认作为用户消息处理", role);
                yield new UserMessage(content);
            }
        };
    }
}
