package com.patent.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson2.JSON;
import com.patent.common.PageResult;
import com.patent.common.exception.BusinessException;
import com.patent.config.MongoChatMemoryRepository;
import com.patent.model.dto.ChatMessageDTO;
import com.patent.model.dto.FavoriteDTO;
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
import com.patent.service.FavoriteService;
import com.patent.service.GraphService;
import com.patent.service.MatchService;
import com.patent.service.PatentService;
import com.patent.service.SearchService;
import com.patent.service.StatsService;
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
    private final PatentService patentService;
    private final StatsService statsService;
    private final GraphService graphService;
    private final FavoriteService favoriteService;
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
     * 图谱数据缓存（临时存储，用于返回给前端渲染）
     */
    private final ThreadLocal<ChatResponseVO.GraphData> graphDataHolder = new ThreadLocal<>();

    /**
     * 系统提示词
     */
    private static final String SYSTEM_PROMPT = """
            你是「PatentMind」（专利智能顾问）——部署在企业专利知识库上的专业智能助手，深度整合了专利检索、技术匹配、知识图谱和数据分析能力，帮助用户高效探索和分析专利信息。
            
            你的职责包括：
            1. 理解用户的专利检索需求
            2. 使用提供的工具函数执行检索操作
            3. 对检索结果进行分析和总结
            4. 提供专业的技术解读和建议

            能力边界：
            只处理与本系统专利知识库相关的问题：专利检索与分析、技术匹配、图谱探索、统计分析、收藏管理。对于无关问题，礼貌说明并引导用户回到专利分析场景。
            
            工具调用决策：
            **检索类工具：**
            - 用户搜索技术关键词（如"深度学习专利"、"找一下神经网络相关"）→ 使用 searchPatents
            - 用户提供详细技术描述（通常超过20字）想找相似专利 → 使用 matchPatents（向量语义匹配，精度更高）
            - 用户需要按申请人/日期/IPC代码精确筛选 → 使用 advancedSearchPatents
            - 用户想看某条专利完整内容（提供公开号或ID）→ 使用 getPatentDetail；两者都未提供则先询问

            **统计分析类工具：**
            - 询问系统规模（"有多少专利"、"知识库大小"）→ 使用 getSystemOverview
            - 询问领域分布（"哪些技术方向最多"、"专利分布"）→ 使用 getDomainStats，可传 keyword 缩小范围
            - 询问申请人排名（"谁的专利最多"、"哪个机构最活跃"）→ 使用 getTopApplicants
            - 询问年度趋势（"近几年申请量变化"、"哪年专利最多"）→ 使用 getPatentTrend
            - 询问热门技术词（"高频词"、"技术热点"、"常见技术实体"）→ 使用 getEntityWordCloud

            **图谱类工具：**
            - 用户探索知识图谱关系（"这个专利关联哪些技术"、"查图谱"）→ 使用 getPatentGraph，queryType 从以下选择：
            * patent：queryValue 填专利公开号，查该专利的技术实体和申请人关联
            * entity：queryValue 填技术词，查该词出现在哪些专利中
            * ipc：queryValue 填IPC代码，查该分类下的专利关联网络
            调用后前端自动渲染嵌入式图谱，回复重点是关系含义解读，不要逐节点描述

            **收藏类工具：**
            - 用户查询收藏（"我收藏了什么"、"查看收藏夹"）→ 使用 getMyFavorites
            - 用户要收藏专利 → 【必须执行三步】
            1. 展示将收藏的专利标题和ID
            2. 询问放入哪个分组（或默认分组）
            3. 获得用户明确确认（"确认"/"是"/"好的"）后，才调用 addToFavorite，绝不可未经确认直接调用此写操作工具

            工具使用策略：
            - 优先使用单个最匹配的工具，避免重复调用同类工具
            - 多维度问题可顺序调用多个工具（如先 searchPatents 再 getDomainStats）
            - 关键词尽量从上下文提取，有合理默认值的参数直接使用默认值
            - 需要多个缺失参数时，一次性询问全部，不要逐个问

            重要规则：
            - 【写操作安全】addToFavorite 是唯一的写操作，执行前必须完成：(1) 向用户展示将收藏的专利标题和ID；(2) 确认分组；(3) 用户明确回复确认后才能调用。用户仅说"收藏一下"不构成确认，必须等待明确的二次确认。
            - 【参数缺失处理】getPatentDetail 必须有专利ID（纯数字）或公开号（如CN202310001234A）之一，两者都没有时主动询问，不得猜测或使用其他字段替代。
            - 【模糊需求处理】用户说"帮我查一下专利"此类无具体关键词的请求，应追问查什么技术方向，而非直接调用工具返回无意义结果。
            - 【多轮对话记忆】对话中用户已提到的专利ID、公开号、申请人名称等信息，在后续轮次中直接使用，无需再次询问。
            - 【工具返回无数据】若工具返回0条结果，明确告知用户并给出可能原因（关键词过于精确、数据库暂无该数据等），建议放宽条件重试，不得编造结果。
            - 【并发工具调用】同一轮对话中最多调用3个不同工具，避免过度分析导致响应缓慢。

            禁止：
            - 编造任何不在工具返回结果中的专利信息
            - 未经用户明确确认就执行 addToFavorite
            - 图谱结果逐节点描述（只给关系洞察）
            - 超过5条的冗长列表（用"还有X条，可进一步筛选查看"代替）
            - 在结果明确的情况下反问用户（先执行，后提供追问）
            
            回复要求：
            1. 全程简体中文，直接给出结论或结果摘要（省略"我来帮你查询"等废话开场）
            2. 回复要简洁专业，避免过于冗长，用 **加粗** 突出关键数据和专利标题，专利公开号标注为代码格式，如 CN202310001234A
            3. 对检索结果进行简要总结，说明找到了多少专利、主要涉及哪些领域，数字用具体数值，不用"一些"、"多个"等模糊词
            4. 关键信息分条列出，控制在5条以内（更多内容引导用户查看前端卡片）
            5. 结果后可补充1-2句分析洞察（技术趋势、竞争格局）
            6. 结尾视情况提供1个有价值的后续追问建议

            """;

    public ChatServiceImpl(@Qualifier("chatChatModel") ChatModel chatModel,
                           SearchService searchService,
                           MatchService matchService,
                           PatentService patentService,
                           StatsService statsService,
                           GraphService graphService,
                           FavoriteService favoriteService,
                           MongoChatMemoryRepository mongoChatMemoryRepository,
                           ChatSessionRepository sessionRepository,
                           ChatMessageRepository messageRepository) {
        this.searchService = searchService;
        this.matchService = matchService;
        this.patentService = patentService;
        this.statsService = statsService;
        this.graphService = graphService;
        this.favoriteService = favoriteService;
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
        graphDataHolder.remove();

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
                    .graphData(graphDataHolder.get())
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
            graphDataHolder.remove();
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
        graphDataHolder.remove();

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
                    ChatResponseVO.GraphData graphData = graphDataHolder.get();
                    toolCallRecords.remove();
                    searchResults.remove();
                    graphDataHolder.remove();

                    List<String> tailEvents = new ArrayList<>();
                    if (!tools.isEmpty()) {
                        tailEvents.add(formatSSEEvent("tools", JSON.toJSONString(tools)));
                    }
                    if (!patents.isEmpty()) {
                        tailEvents.add(formatSSEEvent("patents", JSON.toJSONString(patents)));
                    }
                    if (graphData != null) {
                        tailEvents.add(formatSSEEvent("graph", JSON.toJSONString(graphData)));
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
        graphDataHolder.remove();

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
                    ChatResponseVO.GraphData graphData = graphDataHolder.get();
                    toolCallRecords.remove();
                    searchResults.remove();
                    graphDataHolder.remove();

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
                    if (graphData != null) {
                        tailEvents.add(ServerSentEvent.<String>builder()
                                .event("graph")
                                .data(JSON.toJSONString(graphData))
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

    // ==================== 第一批：只读核心工具 ====================

    /**
     * 工具1：查询专利详情
     */
    @Tool(description = "查询某条专利的完整详细信息，包括标题、申请人、公开日、摘要、技术实体、IPC领域分类等。当用户要查看专利全文、想了解某个专利号的具体内容时使用。")
    public String getPatentDetail(
            @ToolParam(description = "专利的数字ID（如：12345），与公开号二选一") Long patentId,
            @ToolParam(description = "专利公开号（如：CN202310001234A），与patentId二选一") String publicationNo
    ) {
        log.info("工具调用 - getPatentDetail: patentId={}, publicationNo={}", patentId, publicationNo);

        try {
            PatentVO patent = null;

            if (patentId != null) {
                patent = patentService.getPatentDetail(patentId);
            } else if (publicationNo != null && !publicationNo.isBlank()) {
                // 通过公开号先查ID
                PageResult<SearchResultVO> result = searchService.quickSearch(publicationNo, 1, 1);
                if (result.getList() != null && !result.getList().isEmpty()) {
                    patent = patentService.getPatentDetail(result.getList().get(0).getId());
                }
            }

            if (patent == null) {
                return "未找到对应专利，请确认专利ID或公开号是否正确。";
            }

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("getPatentDetail")
                    .parameters(String.format("patentId=%s, publicationNo=%s", patentId, publicationNo))
                    .resultSummary("成功获取专利详情: " + patent.getTitle())
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append("=== 专利详情 ===\n");
            sb.append(String.format("公开号：%s\n", patent.getPublicationNo() != null ? patent.getPublicationNo() : "未知"));
            sb.append(String.format("标题：%s\n", patent.getTitle() != null ? patent.getTitle() : "未知"));
            sb.append(String.format("申请人：%s\n", patent.getApplicant() != null ? patent.getApplicant() : "未知"));
            sb.append(String.format("公开日期：%s\n", patent.getPublicationDate() != null ? patent.getPublicationDate().toString() : "未知"));
            sb.append(String.format("解析状态：%s\n", patent.getParseStatus()));

            if (patent.getPatentAbstract() != null) {
                sb.append(String.format("\n摘要：\n%s\n", truncate(patent.getPatentAbstract(), 500)));
            }

            if (patent.getDomains() != null && !patent.getDomains().isEmpty()) {
                sb.append("\nIPC领域分类：");
                List<String> domainCodes = patent.getDomains().stream()
                        .map(PatentVO.DomainVO::getDomainCode)
                        .filter(c -> c != null)
                        .limit(5)
                        .toList();
                sb.append(String.join("、", domainCodes)).append("\n");
            }

            if (patent.getEntities() != null && !patent.getEntities().isEmpty()) {
                sb.append("\n技术实体：");
                List<String> entityNames = patent.getEntities().stream()
                        .map(e -> e.getEntityName() + "(" + e.getEntityType() + ")")
                        .limit(8)
                        .toList();
                sb.append(String.join("、", entityNames)).append("\n");
            }

            // 添加到检索结果展示
            PatentSummaryVO summary = PatentSummaryVO.builder()
                    .id(patent.getId())
                    .publicationNo(patent.getPublicationNo())
                    .title(patent.getTitle())
                    .applicant(patent.getApplicant())
                    .abstractText(truncate(patent.getPatentAbstract(), 150))
                    .domainCodes(patent.getDomains() != null ?
                            patent.getDomains().stream().map(PatentVO.DomainVO::getDomainCode).limit(5).toList() : null)
                    .entities(patent.getEntities() != null ?
                            patent.getEntities().stream().map(PatentVO.EntityVO::getEntityName).limit(5).toList() : null)
                    .build();
            searchResults.get().add(summary);

            return sb.toString();

        } catch (Exception e) {
            log.error("getPatentDetail 执行失败", e);
            return "查询专利详情失败：" + e.getMessage();
        }
    }

    /**
     * 工具2：高级多条件检索
     */
    @Tool(description = "多条件组合高级专利检索，支持按申请人、日期范围、IPC分类代码、标题、摘要关键词等组合过滤。当用户需要精确筛选，如'2022年后华为的通信专利'、'G06领域的图像识别相关专利'时使用。")
    public String advancedSearchPatents(
            @ToolParam(description = "关键词（同时检索标题和摘要）") String keyword,
            @ToolParam(description = "申请人名称，模糊匹配，如'华为'、'中国科学院'") String applicant,
            @ToolParam(description = "IPC领域代码，支持前缀匹配，如'G06F'、'H04L'") String domainCode,
            @ToolParam(description = "公开日期起始，格式 YYYY-MM-DD，如'2022-01-01'") String dateFrom,
            @ToolParam(description = "公开日期截止，格式 YYYY-MM-DD，如'2024-12-31'") String dateTo,
            @ToolParam(description = "返回结果数量，默认10，最大50") Integer limit
    ) {
        log.info("工具调用 - advancedSearchPatents: keyword={}, applicant={}, domainCode={}, dateFrom={}, dateTo={}, limit={}",
                keyword, applicant, domainCode, dateFrom, dateTo, limit);

        try {
            int pageSize = (limit != null && limit > 0 && limit <= 50) ? limit : 10;

            SearchDTO dto = new SearchDTO();
            dto.setKeyword(keyword);
            dto.setApplicantKeyword(applicant);
            dto.setDomainCode(domainCode);
            dto.setPageNum(1);
            dto.setPageSize(pageSize);
            dto.setEnableHighlight(false);

            if (dateFrom != null && !dateFrom.isBlank()) {
                try {
                    dto.setPublicationDateFrom(java.time.LocalDate.parse(dateFrom));
                } catch (Exception e) {
                    log.warn("日期格式解析失败: dateFrom={}", dateFrom);
                }
            }
            if (dateTo != null && !dateTo.isBlank()) {
                try {
                    dto.setPublicationDateTo(java.time.LocalDate.parse(dateTo));
                } catch (Exception e) {
                    log.warn("日期格式解析失败: dateTo={}", dateTo);
                }
            }

            PageResult<SearchResultVO> result = searchService.advancedSearchWithHighlight(dto);

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
                    .toolName("advancedSearchPatents")
                    .parameters(String.format("keyword=%s, applicant=%s, domainCode=%s, dateFrom=%s, dateTo=%s",
                            keyword, applicant, domainCode, dateFrom, dateTo))
                    .resultSummary(String.format("找到 %d 条专利（共 %d 条）", summaries.size(), result.getTotal()))
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("高级检索完成，共找到 %d 条专利。\n\n", result.getTotal()));

            if (!summaries.isEmpty()) {
                sb.append("检索结果：\n");
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
            } else {
                sb.append("未找到符合条件的专利，建议放宽筛选条件后重试。\n");
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("advancedSearchPatents 执行失败", e);
            return "高级检索失败：" + e.getMessage();
        }
    }

    /**
     * 工具3：获取系统知识库概览统计
     */
    @Tool(description = "获取专利知识库的整体概览统计，包括专利总数、技术实体总数、领域数量、用户数、匹配记录数。当用户询问'系统有多少专利'、'知识库规模'、'总共收录多少数据'时使用。")
    public String getSystemOverview() {
        log.info("工具调用 - getSystemOverview");

        try {
            StatsVO.OverviewVO overview = statsService.getOverviewStats();

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("getSystemOverview")
                    .parameters("无参数")
                    .resultSummary(String.format("专利总数=%d, 实体总数=%d",
                            overview.getTotalPatents(), overview.getTotalEntities()))
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append("=== 专利知识库概览 ===\n");
            sb.append(String.format("📄 专利总数：%d 条\n", overview.getTotalPatents()));
            sb.append(String.format("🔬 技术实体总数：%d 个\n", overview.getTotalEntities()));
            sb.append(String.format("📂 覆盖领域数：%d 个\n", overview.getTotalDomains()));
            sb.append(String.format("👥 注册用户数：%d 人\n", overview.getTotalUsers()));
            sb.append(String.format("🔍 累计匹配分析：%d 次\n", overview.getTotalMatches()));

            return sb.toString();

        } catch (Exception e) {
            log.error("getSystemOverview 执行失败", e);
            return "获取系统概览失败：" + e.getMessage();
        }
    }

    // ==================== 第二批：统计分析工具 ====================

    /**
     * 工具4：申请人排行查询
     */
    @Tool(description = "查询专利申请人排行榜，统计谁的专利数量最多。可以针对特定技术关键词进行范围统计，也可以统计全库排名。当用户问'哪些机构专利最多'、'人工智能领域谁申请专利最多'、'申请人排名'时使用。")
    public String getTopApplicants(
            @ToolParam(description = "可选：技术领域关键词，不填则统计全库，如'深度学习'、'新能源'") String keyword,
            @ToolParam(description = "返回前几名，默认10，最大50") Integer topN
    ) {
        log.info("工具调用 - getTopApplicants: keyword={}, topN={}", keyword, topN);

        try {
            int n = (topN != null && topN > 0 && topN <= 50) ? topN : 10;

            List<Map<String, Object>> applicants = searchService.aggregateTopApplicants(keyword, n);

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("getTopApplicants")
                    .parameters(String.format("keyword=%s, topN=%d", keyword != null ? keyword : "全库", n))
                    .resultSummary(String.format("获取前%d名申请人排行", applicants.size()))
                    .build());

            StringBuilder sb = new StringBuilder();
            if (keyword != null && !keyword.isBlank()) {
                sb.append(String.format("=== 「%s」相关专利申请人排行（前%d名）===\n\n", keyword, n));
            } else {
                sb.append(String.format("=== 全库专利申请人排行（前%d名）===\n\n", n));
            }

            if (applicants.isEmpty()) {
                sb.append("暂无数据。\n");
            } else {
                for (int i = 0; i < applicants.size(); i++) {
                    Map<String, Object> item = applicants.get(i);
                    String name = (String) item.get("applicant");
                    Object count = item.get("count");
                    sb.append(String.format("%d. %s — %s 件专利\n", i + 1, name, count));
                }
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("getTopApplicants 执行失败", e);
            return "获取申请人排行失败：" + e.getMessage();
        }
    }

    /**
     * 工具5：专利申请趋势分析
     */
    @Tool(description = "获取专利申请数量的年度趋势统计，展示各年份的专利数量变化。当用户询问'近几年专利申请趋势'、'专利数量是增长还是减少'、'哪年专利最多'时使用。")
    public String getPatentTrend(
            @ToolParam(description = "统计最近几年的数据，默认5年，最大20年") Integer years
    ) {
        log.info("工具调用 - getPatentTrend: years={}", years);

        try {
            int y = (years != null && years > 0 && years <= 20) ? years : 5;

            List<StatsVO.TrendStatVO> trend = statsService.getPatentTrend(y);

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("getPatentTrend")
                    .parameters(String.format("years=%d", y))
                    .resultSummary(String.format("获取近%d年专利趋势数据，共%d个年份", y, trend.size()))
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("=== 近%d年专利申请趋势 ===\n\n", y));

            if (trend.isEmpty()) {
                sb.append("暂无趋势数据。\n");
            } else {
                long maxCount = trend.stream().mapToLong(StatsVO.TrendStatVO::getCount).max().orElse(1);
                for (StatsVO.TrendStatVO item : trend) {
                    int barLen = maxCount > 0 ? (int) (item.getCount() * 20 / maxCount) : 0;
                    String bar = "█".repeat(Math.max(barLen, item.getCount() > 0 ? 1 : 0));
                    sb.append(String.format("%s: %s %d件\n", item.getYear(), bar, item.getCount()));
                }

                // 计算趋势
                if (trend.size() >= 2) {
                    long first = trend.get(0).getCount();
                    long last = trend.get(trend.size() - 1).getCount();
                    if (last > first) {
                        sb.append(String.format("\n📈 总体趋势：近%d年专利申请量呈增长态势\n", y));
                    } else if (last < first) {
                        sb.append(String.format("\n📉 总体趋势：近%d年专利申请量有所下降\n", y));
                    } else {
                        sb.append(String.format("\n➡️ 总体趋势：近%d年专利申请量基本持平\n", y));
                    }
                }
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("getPatentTrend 执行失败", e);
            return "获取专利趋势数据失败：" + e.getMessage();
        }
    }

    /**
     * 工具6：高频技术实体词云分析
     */
    @Tool(description = "获取专利库中出现频率最高的技术实体词汇，按类型分组展示（产品/方法/材料/组件/效果/应用场景）。当用户询问'最热门的技术词是什么'、'常见技术实体有哪些'、'高频词汇统计'时使用。")
    public String getEntityWordCloud(
            @ToolParam(description = "返回前几个高频实体，默认30，最大100") Integer topN
    ) {
        log.info("工具调用 - getEntityWordCloud: topN={}", topN);

        try {
            int n = (topN != null && topN > 0 && topN <= 100) ? topN : 30;

            StatsVO.WordCloudVO wordCloud = statsService.getEntityWordCloud(n);

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("getEntityWordCloud")
                    .parameters(String.format("topN=%d", n))
                    .resultSummary(String.format("获取前%d个高频技术实体", n))
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("=== 高频技术实体词云（前%d名）===\n\n", n));

            if (wordCloud.getData() == null || wordCloud.getData().isEmpty()) {
                sb.append("暂无实体数据。\n");
                return sb.toString();
            }

            // 按类型分组展示
            if (wordCloud.getByType() != null && !wordCloud.getByType().isEmpty()) {
                for (StatsVO.EntityTypeGroupVO group : wordCloud.getByType()) {
                    sb.append(String.format("【%s】\n", group.getDescription()));
                    List<String> topEntities = group.getEntities().stream()
                            .limit(5)
                            .map(e -> String.format("  %s(%d次)", e.getName(), e.getCount()))
                            .toList();
                    sb.append(String.join("、", topEntities)).append("\n");
                }
            }

            // 总排行前10
            sb.append("\n总体高频排行（前10）：\n");
            List<StatsVO.EntityStatVO> top10 = wordCloud.getData().stream().limit(10).toList();
            for (int i = 0; i < top10.size(); i++) {
                StatsVO.EntityStatVO e = top10.get(i);
                sb.append(String.format("%d. %s — %d次\n", i + 1, e.getName(), e.getCount()));
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("getEntityWordCloud 执行失败", e);
            return "获取技术实体词云失败：" + e.getMessage();
        }
    }

    // ==================== 第三批：图谱 + 收藏工具 ====================

    /**
     * 工具7：知识图谱关系查询
     */
    @Tool(description = "查询专利、技术实体或IPC分类的知识图谱关系。可以查看某专利关联了哪些技术实体、申请人；某技术词汇出现在哪些专利中；某IPC分类下有哪些相关专利。当用户询问'这个专利和哪些技术有关联'、'查一下深度学习这个词的图谱关系'、'G06F分类的专利关联'时使用。")
    public String getPatentGraph(
            @ToolParam(description = "查询类型：patent（按专利公开号查）/ entity（按技术实体名查）/ ipc（按IPC代码查）") String queryType,
            @ToolParam(description = "查询值：专利公开号（如CN202310001234A）、实体名称（如'深度学习'）或IPC代码（如G06F）") String queryValue
    ) {
        log.info("工具调用 - getPatentGraph: queryType={}, queryValue={}", queryType, queryValue);

        try {
            if (queryType == null || queryValue == null || queryValue.isBlank()) {
                return "请提供查询类型（patent/entity/ipc）和对应的查询值。";
            }

            GraphVO graph;
            switch (queryType.toLowerCase()) {
                case "patent" -> graph = graphService.getPatentGraph(queryValue);
                case "entity" -> graph = graphService.getEntityGraph(queryValue);
                case "ipc" -> graph = graphService.getIpcGraph(queryValue);
                default -> {
                    return "不支持的查询类型：" + queryType + "，请使用 patent、entity 或 ipc。";
                }
            }

            // 存储图谱数据供前端渲染
            ChatResponseVO.GraphData graphData = ChatResponseVO.GraphData.builder()
                    .queryType(queryType)
                    .queryValue(queryValue)
                    .nodes(graph.getNodes())
                    .links(graph.getLinks())
                    .build();
            graphDataHolder.set(graphData);

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("getPatentGraph")
                    .parameters(String.format("queryType=%s, queryValue=%s", queryType, queryValue))
                    .resultSummary(String.format("图谱查询完成：%d 个节点，%d 条关系",
                            graph.getNodes().size(), graph.getLinks().size()))
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("=== 「%s」知识图谱关系 ===\n\n", queryValue));
            sb.append(String.format("共发现 %d 个关联节点，%d 条关系边。\n\n",
                    graph.getNodes().size(), graph.getLinks().size()));

            if (!graph.getNodes().isEmpty()) {
                // 按节点类型分组展示
                Map<String, List<GraphVO.NodeVO>> byLabel = graph.getNodes().stream()
                        .collect(Collectors.groupingBy(GraphVO.NodeVO::getLabel));

                byLabel.forEach((label, nodes) -> {
                    String labelName = switch (label) {
                        case "Patent" -> "📄 关联专利";
                        case "Entity" -> "🔬 技术实体";
                        case "IPC" -> "📂 IPC分类";
                        case "Applicant" -> "🏢 申请人";
                        default -> label;
                    };
                    sb.append(String.format("%s（%d个）：\n", labelName, nodes.size()));
                    nodes.stream().limit(5).forEach(n ->
                            sb.append(String.format("  - %s\n", n.getName())));
                    if (nodes.size() > 5) {
                        sb.append(String.format("  ... 还有%d个\n", nodes.size() - 5));
                    }
                    sb.append("\n");
                });
            } else {
                sb.append("未找到图谱关系数据，请确认查询值是否正确，或该节点尚未建立图谱。\n");
            }

            sb.append("（图谱可视化数据已发送至前端渲染）\n");
            return sb.toString();

        } catch (Exception e) {
            log.error("getPatentGraph 执行失败", e);
            return "图谱查询失败：" + e.getMessage();
        }
    }

    /**
     * 工具8：查询我的收藏列表
     */
    @Tool(description = "查询当前登录用户的专利收藏列表，支持按关键词过滤和分组筛选。当用户说'我收藏了哪些专利'、'查看我的收藏'、'我收藏夹里有什么'时使用。")
    public String getMyFavorites(
            @ToolParam(description = "可选：在收藏中过滤关键词，如'深度学习'") String keyword,
            @ToolParam(description = "可选：收藏夹分组名称，如'重要专利'、'待研究'") String groupName,
            @ToolParam(description = "返回条数，默认10") Integer limit
    ) {
        log.info("工具调用 - getMyFavorites: keyword={}, groupName={}, limit={}", keyword, groupName, limit);

        try {
            int pageSize = (limit != null && limit > 0 && limit <= 50) ? limit : 10;

            PageResult<PatentListVO> result = favoriteService.getFavoriteList(1, pageSize, keyword, groupName);

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("getMyFavorites")
                    .parameters(String.format("keyword=%s, groupName=%s, limit=%d", keyword, groupName, pageSize))
                    .resultSummary(String.format("获取收藏列表，共%d条（总%d条）", result.getList().size(), result.getTotal()))
                    .build());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("=== 我的收藏专利（共%d条）===\n\n", result.getTotal()));

            if (result.getList().isEmpty()) {
                sb.append("暂无收藏记录。\n");
            } else {
                List<PatentSummaryVO> summaries = new ArrayList<>();
                for (int i = 0; i < result.getList().size(); i++) {
                    PatentListVO item = result.getList().get(i);
                    sb.append(String.format("%d. 【%s】%s\n   申请人：%s\n",
                            i + 1,
                            item.getPublicationNo() != null ? item.getPublicationNo() : "ID:" + item.getId(),
                            item.getTitle(),
                            item.getApplicant() != null ? item.getApplicant() : "未知"));
                    summaries.add(PatentSummaryVO.builder()
                            .id(item.getId())
                            .publicationNo(item.getPublicationNo())
                            .title(item.getTitle())
                            .applicant(item.getApplicant())
                            .build());
                }
                searchResults.get().addAll(summaries);
            }

            return sb.toString();

        } catch (Exception e) {
            log.error("getMyFavorites 执行失败", e);
            return "获取收藏列表失败：" + e.getMessage();
        }
    }

    /**
     * 工具9：收藏专利（写操作，调用前AI必须向用户二次确认）
     */
    @Tool(description = "将指定专利添加到当前用户的收藏夹。【重要】这是写操作，调用此工具前必须先向用户展示将要收藏的专利信息（标题、ID）和分组，并明确获得用户确认后才能执行。")
    public String addToFavorite(
            @ToolParam(description = "要收藏的专利ID（数字，必填）") Long patentId,
            @ToolParam(description = "收藏备注，可选，如'核心竞争对手专利'") String remark,
            @ToolParam(description = "收藏夹分组名称，可选，不填则放入默认分组") String groupName
    ) {
        log.info("工具调用 - addToFavorite: patentId={}, groupName={}", patentId, groupName);

        try {
            if (patentId == null) {
                return "收藏失败：专利ID不能为空，请提供要收藏的专利ID。";
            }

            FavoriteDTO dto = new FavoriteDTO();
            dto.setPatentId(patentId);
            dto.setRemark(remark);
            dto.setGroupName(groupName != null && !groupName.isBlank() ? groupName : "默认分组");

            Long favoriteId = favoriteService.addFavorite(dto);

            toolCallRecords.get().add(ToolCallInfo.builder()
                    .toolName("addToFavorite")
                    .parameters(String.format("patentId=%d, groupName=%s", patentId, dto.getGroupName()))
                    .resultSummary(String.format("收藏成功，收藏ID=%d", favoriteId))
                    .build());

            return String.format("✅ 收藏成功！专利（ID:%d）已添加到「%s」分组。收藏ID：%d",
                    patentId, dto.getGroupName(), favoriteId);

        } catch (BusinessException be) {
            log.warn("addToFavorite 业务异常: {}", be.getMessage());
            return "收藏失败：" + be.getMessage();
        } catch (Exception e) {
            log.error("addToFavorite 执行失败", e);
            return "收藏操作失败：" + e.getMessage();
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
