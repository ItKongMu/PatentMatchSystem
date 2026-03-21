package com.patent.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.patent.common.PageResult;
import com.patent.common.exception.BusinessException;
import com.patent.config.MongoChatMemoryRepository;
import com.patent.model.dto.ChatMessageDTO;
import com.patent.model.dto.MatchQueryDTO;
import com.patent.model.dto.SearchDTO;
import com.patent.model.entity.ChatMessage;
import com.patent.model.entity.ChatSession;
import com.patent.model.vo.*;
import com.patent.model.vo.ChatResponseVO.PatentSummaryVO;
import com.patent.model.vo.ChatResponseVO.ToolCallInfo;
import com.patent.repository.ChatMessageRepository;
import com.patent.repository.ChatSessionRepository;
import com.patent.service.ChatService;
import com.patent.service.MatchService;
import com.patent.service.SearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 对话式检索服务实现
 * 使用 Spring AI Function Calling 实现智能专利检索
 * 集成 MongoDB 持久化聊天历史
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private final ChatClient chatClient;
    private final SearchService searchService;
    private final MatchService matchService;
    private final ChatMemory chatMemory;
    private final ChatSessionRepository sessionRepository;
    private final ChatMessageRepository messageRepository;
    private final MongoChatMemoryRepository mongoChatMemoryRepository;

    /**
     * 工具调用记录（临时存储，用于返回给前端）
     */
    private final ThreadLocal<List<ToolCallInfo>> toolCallRecords = ThreadLocal.withInitial(ArrayList::new);

    /**
     * 检索结果缓存（临时存储，用于返回给前端）
     */
    private final ThreadLocal<List<PatentSummaryVO>> searchResults = ThreadLocal.withInitial(ArrayList::new);

    /**
     * 系统提示词
     */
    private static final String SYSTEM_PROMPT = """
            你是一个专业的专利技术检索助手，专门帮助用户查找和分析专利信息。
            
            你的职责包括：
            1. 理解用户的专利检索需求
            2. 使用提供的工具函数执行检索操作
            3. 对检索结果进行分析和总结
            4. 提供专业的技术解读和建议
            
            工具使用指南：
            - 当用户想要搜索专利时，使用 searchPatents 工具
            - 当用户想要进行技术匹配分析时，使用 matchPatents 工具
            - 当用户询问统计数据时，使用 getDomainStats 工具
            
            回复要求：
            1. 使用中文回复
            2. 回复要简洁专业，避免过于冗长
            3. 对检索结果进行简要总结，说明找到了多少专利、主要涉及哪些领域
            4. 如果用户需求不明确，主动询问细化条件
            5. 最后可以提供1-2个后续建议问题帮助用户深入探索
            """;

    public ChatServiceImpl(@Qualifier("chatChatModel") ChatModel chatModel,
                           SearchService searchService,
                           MatchService matchService,
                           MongoChatMemoryRepository mongoChatMemoryRepository,
                           ChatSessionRepository sessionRepository,
                           ChatMessageRepository messageRepository) {
        this.searchService = searchService;
        this.matchService = matchService;
        this.mongoChatMemoryRepository = mongoChatMemoryRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;

        // 使用 MongoDB 实现的 ChatMemoryRepository 创建会话记忆
        this.chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(mongoChatMemoryRepository)
                .maxMessages(20)
                .build();

        // 创建 ChatClient，注册工具函数和记忆advisor
        // 使用 chatChatModel（对话专用：在线=qwen-max，离线=deepseek-r1:7b）
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(this)  // 注册当前类中的 @Tool 方法
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        log.info("对话式检索服务初始化完成（使用 chatChatModel 对话专用模型，MongoDB 持久化已启用）");
    }

    @Override
    public ChatResponseVO chat(ChatMessageDTO dto) {
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        // 获取当前登录用户ID并设置到 ThreadLocal
        Long userId = getCurrentUserId();
        MongoChatMemoryRepository.setCurrentUserId(userId);

        // 清理线程本地存储
        toolCallRecords.get().clear();
        searchResults.get().clear();

        try {
            log.info("处理对话消息: sessionId={}, userId={}, message={}", sessionId, userId, dto.getMessage());

            final String conversationId = sessionId;
            
            // 构建带记忆的ChatClient请求，使用会话ID隔离上下文
            String response = chatClient.prompt()
                    .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(dto.getMessage())
                    .call()
                    .content();

            log.info("AI回复: {}", response);

            // 构建响应
            return ChatResponseVO.builder()
                    .sessionId(sessionId)
                    .reply(response)
                    .patents(new ArrayList<>(searchResults.get()))
                    .toolCalls(new ArrayList<>(toolCallRecords.get()))
                    .suggestions(generateSuggestions(dto.getMessage(), response))
                    .timestamp(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("对话处理失败", e);
            return ChatResponseVO.builder()
                    .sessionId(sessionId)
                    .reply("抱歉，处理您的请求时出现了问题，请稍后重试。错误信息：" + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build();
        } finally {
            // 清理线程本地存储
            toolCallRecords.remove();
            searchResults.remove();
            MongoChatMemoryRepository.clearCurrentUserId();
        }
    }

    @Override
    public void clearSession(String sessionId) {
        log.info("清除会话请求: {}", sessionId);
        // 使用 MongoDB 实现的删除方法
        mongoChatMemoryRepository.deleteByConversationId(sessionId);
    }

    @Override
    public Flux<String> chatStream(ChatMessageDTO dto) {
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        final String conversationId = sessionId;

        // 获取当前登录用户ID并设置到 ThreadLocal
        Long userId = getCurrentUserId();
        MongoChatMemoryRepository.setCurrentUserId(userId);
        MongoChatMemoryRepository.registerSessionUser(conversationId, userId);

        // 清理线程本地存储
        toolCallRecords.get().clear();
        searchResults.get().clear();

        log.info("开始流式对话(非流式执行+模拟流输出): sessionId={}, userId={}, message={}", conversationId, userId, dto.getMessage());

        // 先发送 session 事件
        Flux<String> sessionEvent = Flux.just(formatSSEEvent("session",
                JSON.toJSONString(Map.of("sessionId", conversationId))));

        // 使用非流式 .call() 执行（含 Function Calling），规避通义千问流式 Function Calling toolName 为空的 bug
        Flux<String> mainFlux = Mono.fromCallable(() -> {
                    MongoChatMemoryRepository.setCurrentUserId(userId);
                    try {
                        String response = chatClient.prompt()
                                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                                .user(dto.getMessage())
                                .call()
                                .content();
                        log.info("流式对话(非流式执行)完成: sessionId={}, 响应长度={}", conversationId,
                                response != null ? response.length() : 0);
                        return response != null ? response : "";
                    } finally {
                        MongoChatMemoryRepository.clearCurrentUserId();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(fullText -> {
                    // 将完整文本拆分成 chunk，模拟流式打字效果（每次最多4个字符）
                    List<String> chunks = splitIntoChunks(fullText, 4);
                    List<String> contentEvents = chunks.stream()
                            .map(chunk -> formatSSEEvent("content", chunk))
                            .toList();

                    // 收集工具调用与检索结果
                    List<ToolCallInfo> tools = new ArrayList<>(toolCallRecords.get());
                    List<PatentSummaryVO> patents = new ArrayList<>(searchResults.get());
                    toolCallRecords.remove();
                    searchResults.remove();

                    List<String> tailEvents = new ArrayList<>();
                    if (!tools.isEmpty()) {
                        tailEvents.add(formatSSEEvent("tools", JSON.toJSONString(tools)));
                    }
                    if (!patents.isEmpty()) {
                        tailEvents.add(formatSSEEvent("patents", JSON.toJSONString(patents)));
                    }
                    tailEvents.add(formatSSEEvent("done", JSON.toJSONString(Map.of(
                            "timestamp", LocalDateTime.now().toString(),
                            "suggestions", generateSuggestions(dto.getMessage(), fullText)
                    ))));

                    // content chunk 之间加极短延迟模拟打字，tail 事件紧随其后
                    Flux<String> contentFlux = Flux.fromIterable(contentEvents)
                            .delayElements(Duration.ofMillis(20));
                    Flux<String> tailFlux = Flux.fromIterable(tailEvents);
                    return Flux.concat(contentFlux, tailFlux);
                })
                .doOnError(error -> log.error("流式对话(非流式执行)出错: sessionId={}", conversationId, error))
                .onErrorResume(error -> {
                    toolCallRecords.remove();
                    searchResults.remove();
                    MongoChatMemoryRepository.clearCurrentUserId();
                    String errorMessage = getReadableErrorMessage(error);
                    return Flux.just(formatSSEEvent("error", JSON.toJSONString(Map.of(
                            "message", errorMessage,
                            "timestamp", LocalDateTime.now().toString()
                    ))));
                });

        return Flux.concat(sessionEvent, mainFlux);
    }

    /**
     * 格式化SSE事件
     */
    private String formatSSEEvent(String event, String data) {
        return String.format("event:%s\ndata:%s\n\n", event, data);
    }

    @Override
    public Flux<ServerSentEvent<String>> chatStreamSSE(ChatMessageDTO dto) {
        String sessionId = dto.getSessionId();
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        final String conversationId = sessionId;

        // 获取当前登录用户ID并设置到 ThreadLocal
        Long userId = getCurrentUserId();
        MongoChatMemoryRepository.setCurrentUserId(userId);

        // 注册会话与用户的映射（解决WebFlux跨线程问题）
        MongoChatMemoryRepository.registerSessionUser(conversationId, userId);

        // 清理线程本地存储
        toolCallRecords.get().clear();
        searchResults.get().clear();

        log.info("开始SSE流式对话(非流式执行+模拟流输出): sessionId={}, userId={}, message={}", conversationId, userId, dto.getMessage());

        // 先发送 session 事件
        Flux<ServerSentEvent<String>> sessionEvent = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("session")
                        .data(JSON.toJSONString(Map.of("sessionId", conversationId)))
                        .build()
        );

        // 使用非流式 .call() 执行（含 Function Calling），规避通义千问流式 Function Calling toolName 为空的 bug
        Flux<ServerSentEvent<String>> mainFlux = Mono.fromCallable(() -> {
                    MongoChatMemoryRepository.setCurrentUserId(userId);
                    try {
                        String response = chatClient.prompt()
                                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                                .user(dto.getMessage())
                                .call()
                                .content();
                        log.info("SSE流式对话(非流式执行)完成: sessionId={}, 响应长度={}", conversationId,
                                response != null ? response.length() : 0);
                        return response != null ? response : "";
                    } finally {
                        MongoChatMemoryRepository.clearCurrentUserId();
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(fullText -> {
                    // 将完整文本拆分成 chunk，模拟流式打字效果（每次最多4个字符）
                    List<String> chunks = splitIntoChunks(fullText, 4);
                    List<ServerSentEvent<String>> contentEvents = chunks.stream()
                            .map(chunk -> ServerSentEvent.<String>builder()
                                    .event("content")
                                    .data(chunk)
                                    .build())
                            .toList();

                    // 收集工具调用与检索结果
                    List<ToolCallInfo> tools = new ArrayList<>(toolCallRecords.get());
                    List<PatentSummaryVO> patents = new ArrayList<>(searchResults.get());
                    toolCallRecords.remove();
                    searchResults.remove();

                    List<ServerSentEvent<String>> tailEvents = new ArrayList<>();
                    if (!tools.isEmpty()) {
                        tailEvents.add(ServerSentEvent.<String>builder()
                                .event("tools")
                                .data(JSON.toJSONString(tools))
                                .build());
                    }
                    if (!patents.isEmpty()) {
                        tailEvents.add(ServerSentEvent.<String>builder()
                                .event("patents")
                                .data(JSON.toJSONString(patents))
                                .build());
                    }
                    tailEvents.add(ServerSentEvent.<String>builder()
                            .event("done")
                            .data(JSON.toJSONString(Map.of(
                                    "timestamp", LocalDateTime.now().toString(),
                                    "suggestions", generateSuggestions(dto.getMessage(), fullText)
                            )))
                            .build());

                    // content chunk 之间加极短延迟模拟打字，tail 事件紧随其后
                    Flux<ServerSentEvent<String>> contentFlux = Flux.fromIterable(contentEvents)
                            .delayElements(Duration.ofMillis(20));
                    Flux<ServerSentEvent<String>> tailFlux = Flux.fromIterable(tailEvents);
                    return Flux.concat(contentFlux, tailFlux);
                })
                .doOnError(error -> log.error("SSE流式对话(非流式执行)出错: sessionId={}", conversationId, error))
                .onErrorResume(error -> {
                    toolCallRecords.remove();
                    searchResults.remove();
                    MongoChatMemoryRepository.clearCurrentUserId();
                    String errorMessage = getReadableErrorMessage(error);
                    return Flux.just(ServerSentEvent.<String>builder()
                            .event("error")
                            .data(JSON.toJSONString(Map.of(
                                    "message", errorMessage,
                                    "timestamp", LocalDateTime.now().toString()
                            )))
                            .build());
                });

        return Flux.concat(sessionEvent, mainFlux);
    }

    // ==================== 会话管理接口实现 ====================

    @Override
    public List<ChatSessionVO> getUserSessions(Long userId, Integer page, Integer size) {
        log.info("获取用户会话列表: userId={}, page={}, size={}", userId, page, size);
        
        // 参数校验
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;
        if (size > 100) size = 100;
        
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<ChatSession> sessionPage = sessionRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(
                userId, "active", pageRequest);
        
        return sessionPage.getContent().stream()
                .map(this::toChatSessionVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ChatMessageVO> getSessionMessages(String sessionId, Integer page, Integer size) {
        // 参数校验
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 50;
        if (size > 200) size = 200;
        
        // 验证会话存在
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        
        // 权限校验：只能查看自己的会话
        Long currentUserId = getCurrentUserId();
        if (!session.getUserId().equals(currentUserId)) {
            throw new BusinessException("无权访问此会话");
        }
        
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<ChatMessage> messagePage = messageRepository.findBySessionIdOrderBySequenceDesc(
                sessionId, pageRequest);
        
        // 按序号正序返回（最早的消息在前），并过滤掉 system 消息
        List<ChatMessageVO> messages = messagePage.getContent().stream()
                .filter(msg -> !"system".equals(msg.getRole())) // 过滤掉系统消息
                .map(this::toChatMessageVO)
                .collect(Collectors.toList());
        
        // 反转列表，使最早的消息在前
        Collections.reverse(messages);
        
        return messages;
    }

    @Override
    public void deleteSession(String sessionId, Long userId) {
        log.info("删除会话: sessionId={}, userId={}", sessionId, userId);
        
        // 验证会话存在且属于当前用户
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此会话");
        }
        
        // 使用 MongoChatMemoryRepository 的删除方法
        mongoChatMemoryRepository.deleteByConversationId(sessionId);
        
        log.info("会话删除成功: sessionId={}", sessionId);
    }

    @Override
    public void updateSessionTitle(String sessionId, String title, Long userId) {
        log.info("更新会话标题: sessionId={}, title={}, userId={}", sessionId, title, userId);
        
        if (title == null || title.isBlank()) {
            throw new BusinessException("标题不能为空");
        }
        
        if (title.length() > 100) {
            title = title.substring(0, 100);
        }
        
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权修改此会话");
        }
        
        session.setTitle(title);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        
        log.info("会话标题更新成功: sessionId={}", sessionId);
    }

    @Override
    public void archiveSession(String sessionId, Long userId) {
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权归档此会话");
        }
        
        session.setStatus("archived");
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    @Override
    public List<ChatSessionVO> getArchivedSessions(Long userId, Integer page, Integer size) {
        // 参数校验
        if (page == null || page < 1) page = 1;
        if (size == null || size < 1) size = 20;
        if (size > 100) size = 100;
        
        PageRequest pageRequest = PageRequest.of(page - 1, size);
        Page<ChatSession> sessionPage = sessionRepository.findByUserIdAndStatusOrderByUpdatedAtDesc(
                userId, "archived", pageRequest);
        
        return sessionPage.getContent().stream()
                .map(this::toChatSessionVO)
                .collect(Collectors.toList());
    }

    @Override
    public void restoreSession(String sessionId, Long userId) {
        ChatSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new BusinessException("会话不存在"));
        
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权恢复此会话");
        }
        
        if (!"archived".equals(session.getStatus())) {
            throw new BusinessException("该会话未被归档");
        }
        
        session.setStatus("active");
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    // ==================== Function Calling 工具函数 ====================

    /**
     * 专利关键词检索工具
     */
    @Tool(description = "根据关键词检索专利。当用户想要搜索特定关键词、技术术语、产品名称相关的专利时使用此工具。")
    public String searchPatents(
            @ToolParam(description = "检索关键词，如'深度学习'、'图像识别'等") String keyword,
            @ToolParam(description = "领域代码过滤，如'G06'表示计算机领域，可选") String domainCode,
            @ToolParam(description = "返回结果数量，默认10，最大50") Integer limit
    ) {
        log.info("工具调用 - searchPatents: keyword={}, domainCode={}, limit={}", keyword, domainCode, limit);

        try {
            int pageSize = (limit != null && limit > 0 && limit <= 50) ? limit : 10;

            PageResult<SearchResultVO> result;
            if (domainCode != null && !domainCode.isBlank()) {
                SearchDTO searchDTO = new SearchDTO();
                searchDTO.setKeyword(keyword);
                searchDTO.setDomainCode(domainCode);
                searchDTO.setPageNum(1);
                searchDTO.setPageSize(pageSize);
                result = searchService.advancedSearchWithHighlight(searchDTO);
            } else {
                result = searchService.quickSearch(keyword, 1, pageSize);
            }

            List<PatentSummaryVO> summaries = new ArrayList<>();
            if (result.getList() != null) {
                for (SearchResultVO vo : result.getList()) {
                    PatentSummaryVO summary = PatentSummaryVO.builder()
                            .id(vo.getId())
                            .publicationNo(vo.getPublicationNo())
                            .title(vo.getTitle())
                            .applicant(vo.getApplicant())
                            .abstractText(truncate(vo.getPatentAbstract(), 150))
                            .relevanceScore(vo.getScore() != null ? vo.getScore().doubleValue() : null)
                            .domainCodes(vo.getDomainCodes())
                            .entities(vo.getEntities() != null ? 
                                    vo.getEntities().stream().map(PatentListVO.EntityVO::getEntityName).toList() : null)
                            .build();
                    summaries.add(summary);
                }
            }
            searchResults.get().addAll(summaries);

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("searchPatents")
                    .parameters(String.format("keyword=%s, domainCode=%s, limit=%d", keyword, domainCode, pageSize))
                    .resultSummary(String.format("找到 %d 条专利", result.getTotal()))
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("检索完成，共找到 %d 条相关专利。\n\n", result.getTotal()));
            
            if (!summaries.isEmpty()) {
                sb.append("主要检索结果：\n");
                for (int i = 0; i < Math.min(5, summaries.size()); i++) {
                    PatentSummaryVO p = summaries.get(i);
                    sb.append(String.format("%d. 【%s】%s\n   申请人：%s\n", 
                            i + 1, 
                            p.getPublicationNo() != null ? p.getPublicationNo() : "ID:" + p.getId(),
                            p.getTitle(),
                            p.getApplicant() != null ? p.getApplicant() : "未知"));
                }
                if (summaries.size() > 5) {
                    sb.append(String.format("\n... 还有 %d 条结果未显示\n", summaries.size() - 5));
                }
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("searchPatents 执行失败", e);
            return "检索失败：" + e.getMessage();
        }
    }

    /**
     * 专利技术匹配工具
     */
    @Tool(description = "根据技术描述进行专利匹配分析。当用户提供详细的技术方案、想要查找相似专利、或进行技术对比分析时使用此工具。")
    public String matchPatents(
            @ToolParam(description = "技术描述文本，描述要匹配的技术方案或需求") String query,
            @ToolParam(description = "领域过滤代码，如'G'表示物理领域，可选") String domainFilter,
            @ToolParam(description = "返回结果数量，默认10") Integer topK
    ) {
        log.info("工具调用 - matchPatents: query长度={}, domainFilter={}, topK={}", 
                query != null ? query.length() : 0, domainFilter, topK);

        try {
            int k = (topK != null && topK > 0 && topK <= 30) ? topK : 10;

            MatchQueryDTO matchDTO = new MatchQueryDTO();
            matchDTO.setQuery(query);
            matchDTO.setDomainFilter(domainFilter);
            matchDTO.setTopK(k);

            MatchResultVO result = matchService.matchByText(matchDTO);

            List<PatentSummaryVO> summaries = new ArrayList<>();
            if (result.getMatches() != null) {
                for (MatchResultVO.MatchItemVO item : result.getMatches()) {
                    PatentSummaryVO summary = PatentSummaryVO.builder()
                            .id(item.getPatentId())
                            .publicationNo(item.getPublicationNo())
                            .title(item.getTitle())
                            .applicant(item.getApplicant())
                            .abstractText(truncate(item.getPatentAbstract(), 150))
                            .relevanceScore(item.getSimilarityScore() != null ? item.getSimilarityScore().doubleValue() : null)
                            .domainCodes(item.getDomainCodes())
                            .build();
                    summaries.add(summary);
                }
            }
            searchResults.get().addAll(summaries);

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("matchPatents")
                    .parameters(String.format("query长度=%d, domainFilter=%s, topK=%d", 
                            query != null ? query.length() : 0, domainFilter, k))
                    .resultSummary(String.format("匹配到 %d 条相似专利", summaries.size()))
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("技术匹配完成，找到 %d 条相似专利。\n\n", summaries.size()));

            if (!summaries.isEmpty()) {
                sb.append("匹配结果（按相似度排序）：\n");
                for (int i = 0; i < Math.min(5, summaries.size()); i++) {
                    PatentSummaryVO p = summaries.get(i);
                    sb.append(String.format("%d. 【相似度: %.1f%%】%s\n   %s\n",
                            i + 1,
                            p.getRelevanceScore() != null ? p.getRelevanceScore() * 100 : 0,
                            p.getTitle(),
                            p.getPublicationNo() != null ? p.getPublicationNo() : "ID:" + p.getId()));
                }
            }

            if (result.getQueryEntities() != null && !result.getQueryEntities().isEmpty()) {
                sb.append("\n识别的技术实体：");
                List<String> entityNames = result.getQueryEntities().stream()
                        .map(e -> e.getName() + "(" + e.getType() + ")")
                        .limit(5)
                        .toList();
                sb.append(String.join("、", entityNames));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("matchPatents 执行失败", e);
            return "匹配失败：" + e.getMessage();
        }
    }

    /**
     * 领域统计工具
     */
    @Tool(description = "获取专利领域分布统计数据。当用户询问某个关键词相关专利的领域分布、统计信息时使用此工具。")
    public String getDomainStats(
            @ToolParam(description = "统计的关键词范围，可选，为空则统计全部") String keyword
    ) {
        log.info("工具调用 - getDomainStats: keyword={}", keyword);

        try {
            Map<String, Object> stats = searchService.aggregateDomainStats(keyword);

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("getDomainStats")
                    .parameters("keyword=" + (keyword != null ? keyword : "全部"))
                    .resultSummary("获取领域统计数据")
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append("领域分布统计：\n");

            Long totalHits = (Long) stats.get("totalHits");
            if (totalHits != null) {
                sb.append(String.format("总专利数：%d\n\n", totalHits));
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> domainStats = (List<Map<String, Object>>) stats.get("domainSectionStats");
            if (domainStats != null && !domainStats.isEmpty()) {
                sb.append("按IPC部分类统计：\n");
                Map<String, String> sectionNames = Map.of(
                        "A", "人类生活必需",
                        "B", "作业；运输",
                        "C", "化学；冶金",
                        "D", "纺织；造纸",
                        "E", "固定建筑物",
                        "F", "机械工程",
                        "G", "物理",
                        "H", "电学"
                );
                for (Map<String, Object> item : domainStats) {
                    String section = (String) item.get("section");
                    Long count = (Long) item.get("count");
                    String name = sectionNames.getOrDefault(section, "其他");
                    sb.append(String.format("- %s (%s): %d 条\n", section, name, count));
                }
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("getDomainStats 执行失败", e);
            return "统计失败：" + e.getMessage();
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 将文本拆分成指定大小的 chunk 列表，用于模拟流式输出
     * 对中文友好：按字符数分割
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> chunks = new ArrayList<>();
        int len = text.length();
        for (int i = 0; i < len; i += chunkSize) {
            chunks.add(text.substring(i, Math.min(i + chunkSize, len)));
        }
        return chunks;
    }

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            return StpUtil.getLoginIdAsLong();
        } catch (Exception e) {
            log.warn("获取当前用户ID失败，可能未登录: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 将 ChatSession 转换为 VO
     */
    private ChatSessionVO toChatSessionVO(ChatSession session) {
        return ChatSessionVO.builder()
                .sessionId(session.getSessionId())
                .title(session.getTitle())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .messageCount(session.getMessageCount())
                .status(session.getStatus())
                .metadata(session.getMetadata())
                .build();
    }

    /**
     * 将 ChatMessage 转换为 VO
     */
    private ChatMessageVO toChatMessageVO(ChatMessage message) {
        return ChatMessageVO.builder()
                .id(message.getId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .sequence(message.getSequence())
                .metadata(message.getMetadata())
                .build();
    }

    /**
     * 获取可读的错误信息
     */
    private String getReadableErrorMessage(Throwable error) {
        if (error == null) {
            return "发生未知错误，请稍后重试";
        }
        
        String message = error.getMessage();
        Throwable cause = error.getCause();
        
        if (error instanceof java.net.SocketException 
                || (message != null && message.contains("Connection reset"))
                || (cause != null && cause instanceof java.net.SocketException)) {
            return "AI 服务连接中断，可能是网络波动，请稍后重试";
        }
        
        if (message != null && (message.contains("timeout") || message.contains("timed out"))) {
            return "AI 服务响应超时，请稍后重试";
        }
        
        if (message != null && message.contains("Connection refused")) {
            return "AI 服务暂时不可用，请稍后重试";
        }
        
        if (error.getClass().getName().contains("WebClientRequestException")) {
            return "AI 服务请求失败，请检查网络连接后重试";
        }
        
        if (message != null && (message.contains("401") || message.contains("403"))) {
            return "AI 服务认证失败，请联系管理员";
        }
        
        if (message != null && message.contains("429")) {
            return "请求过于频繁，请稍后再试";
        }
        
        return "处理请求时出现问题，请稍后重试";
    }

    /**
     * 生成后续建议问题
     */
    private List<String> generateSuggestions(String userMessage, String response) {
        List<String> suggestions = new ArrayList<>();

        if (userMessage.contains("检索") || userMessage.contains("搜索") || userMessage.contains("查找")) {
            suggestions.add("是否需要按申请日期筛选结果？");
            suggestions.add("要查看某项专利的详细信息吗？");
        } else if (userMessage.contains("匹配") || userMessage.contains("相似")) {
            suggestions.add("需要对比分析这些专利的技术方案吗？");
            suggestions.add("是否要查看某项专利的完整摘要？");
        } else if (userMessage.contains("统计") || userMessage.contains("分布")) {
            suggestions.add("要查看特定领域的专利详情吗？");
            suggestions.add("需要分析申请人排名吗？");
        } else {
            suggestions.add("还需要进一步检索其他技术方向吗？");
            suggestions.add("是否需要进行技术匹配分析？");
        }

        return suggestions.subList(0, Math.min(2, suggestions.size()));
    }

    /**
     * 截断文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
}
