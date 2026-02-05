package com.patent.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.patent.common.Result;
import com.patent.model.dto.ChatMessageDTO;
import com.patent.model.vo.ChatMessageVO;
import com.patent.model.vo.ChatResponseVO;
import com.patent.model.vo.ChatSessionVO;
import com.patent.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 对话式检索控制器
 * 基于 LLM Function Calling 实现智能专利检索对话
 */
@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "对话式检索", description = "基于大语言模型的智能对话检索接口")
public class ChatController {

    private final ChatService chatService;

    /**
     * 发送对话消息（同步模式）
     */
    @PostMapping
    @Operation(summary = "发送对话消息", description = "发送消息给AI助手，进行专利检索对话（同步模式）")
    public Result<ChatResponseVO> chat(@Valid @RequestBody ChatMessageDTO dto) {
        log.info("收到对话请求: sessionId={}, message={}", dto.getSessionId(), dto.getMessage());
        ChatResponseVO response = chatService.chat(dto);
        return Result.success(response);
    }

    /**
     * 发送对话消息（流式模式 SSE）
     * 使用 ServerSentEvent 类型确保正确的 SSE 格式
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式对话", description = "发送消息给AI助手，流式返回对话结果（SSE）")
    public Flux<ServerSentEvent<String>> chatStream(
            @Valid @RequestBody ChatMessageDTO dto,
            jakarta.servlet.http.HttpServletResponse response) {
        log.info("收到流式对话请求: sessionId={}, message={}", dto.getSessionId(), dto.getMessage());
        // 设置 SSE 响应头，禁用缓冲
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Connection", "keep-alive");
        return chatService.chatStreamSSE(dto);
    }

    /**
     * 清除会话历史
     */
    @DeleteMapping("/session/{sessionId}")
    @Operation(summary = "清除会话", description = "清除指定会话的历史记录")
    public Result<Void> clearSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        chatService.clearSession(sessionId);
        return Result.success(null);
    }

    /**
     * 获取会话建议
     */
    @GetMapping("/suggestions")
    @Operation(summary = "获取建议问题", description = "获取对话初始建议问题")
    public Result<List<String>> getSuggestions() {
        List<String> suggestions = List.of(
                "帮我搜索关于深度学习的专利",
                "查找图像识别相关的技术方案",
                "有哪些人工智能领域的专利？",
                "统计一下计算机视觉专利的领域分布",
                "帮我匹配一个技术方案：基于卷积神经网络的图像分类方法"
        );
        return Result.success(suggestions);
    }

    // ==================== 会话管理接口 ====================

    /**
     * 获取用户会话列表
     */
    @GetMapping("/sessions")
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有聊天会话")
    public Result<List<ChatSessionVO>> getSessions(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.debug("获取用户会话列表: userId={}, page={}, size={}", userId, page, size);
        List<ChatSessionVO> sessions = chatService.getUserSessions(userId, page, size);
        return Result.success(sessions);
    }

    /**
     * 获取会话消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    @Operation(summary = "获取会话消息", description = "获取指定会话的消息历史")
    public Result<List<ChatMessageVO>> getSessionMessages(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "50") Integer size) {
        log.debug("获取会话消息: sessionId={}, page={}, size={}", sessionId, page, size);
        List<ChatMessageVO> messages = chatService.getSessionMessages(sessionId, page, size);
        return Result.success(messages);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/sessions/{sessionId}")
    @Operation(summary = "删除会话", description = "删除指定的聊天会话及其所有消息")
    public Result<Void> deleteSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        log.info("删除会话: sessionId={}, userId={}", sessionId, userId);
        chatService.deleteSession(sessionId, userId);
        return Result.success(null);
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/sessions/{sessionId}/title")
    @Operation(summary = "更新会话标题", description = "更新指定会话的标题")
    public Result<Void> updateSessionTitle(
            @Parameter(description = "会话ID") @PathVariable String sessionId,
            @RequestBody Map<String, String> body) {
        Long userId = StpUtil.getLoginIdAsLong();
        String title = body.get("title");
        log.info("更新会话标题: sessionId={}, title={}, userId={}", sessionId, title, userId);
        chatService.updateSessionTitle(sessionId, title, userId);
        return Result.success(null);
    }

    /**
     * 归档会话
     */
    @PutMapping("/sessions/{sessionId}/archive")
    @Operation(summary = "归档会话", description = "将指定会话归档")
    public Result<Void> archiveSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatService.archiveSession(sessionId, userId);
        return Result.success(null);
    }

    /**
     * 获取已归档会话列表
     */
    @GetMapping("/sessions/archived")
    @Operation(summary = "获取已归档会话", description = "获取当前用户的已归档会话列表")
    public Result<List<ChatSessionVO>> getArchivedSessions(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer size) {
        Long userId = StpUtil.getLoginIdAsLong();
        List<ChatSessionVO> sessions = chatService.getArchivedSessions(userId, page, size);
        return Result.success(sessions);
    }

    /**
     * 恢复已归档会话
     */
    @PutMapping("/sessions/{sessionId}/restore")
    @Operation(summary = "恢复会话", description = "将已归档的会话恢复为活跃状态")
    public Result<Void> restoreSession(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        Long userId = StpUtil.getLoginIdAsLong();
        chatService.restoreSession(sessionId, userId);
        return Result.success(null);
    }
}
