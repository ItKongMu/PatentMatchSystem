package com.patent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LLM 模型配置 - 动态路由架构
 * <p>
 * 三模型分工：
 * <ul>
 *   <li>{@code chatChatModel}         - 对话专用（ChatService），运行时由 DynamicLlmFactory 按用户配置动态切换</li>
 *   <li>{@code llmChatModel}          - 分析专用（LlmService），运行时由 DynamicLlmFactory 按用户配置动态切换</li>
 *   <li>{@code primaryEmbeddingModel} - 向量嵌入（VectorStore/Qdrant），固定使用 OpenAI 兼容接口</li>
 * </ul>
 * <p>
 * EmbeddingModel 设计原则：
 * <ul>
 *   <li>向量嵌入统一使用 {@link OpenAiEmbeddingModel}（OpenAI 兼容接口）</li>
 *   <li>离线场景：Ollama 提供 OpenAI 兼容端口（默认 http://localhost:11434/v1），
 *       在 spring.ai.openai.base-url 中配置即可，无需额外 OllamaEmbeddingModel</li>
 *   <li>在线场景：直接调用 OpenAI / 通义千问 / DeepSeek 等兼容 API</li>
 *   <li>模型名由 spring.ai.openai.embedding.options.model 统一配置</li>
 *   <li>向量维度与 Qdrant Collection 强绑定，切换模型需重建向量库</li>
 * </ul>
 */
@Slf4j
@Configuration
public class LlmConfig {

    /**
     * 对话专用 ChatModel Bean（Spring 容器注册用）
     * <p>
     * 启动时从数据库系统默认配置初始化，无数据库配置则 yaml 兜底。
     * 运行时 ChatService 通过 DynamicLlmFactory.getChatModel(userId) 动态获取用户专属实例。
     */
    @Bean("chatChatModel")
    @Primary
    public ChatModel chatChatModel(DynamicLlmFactory factory) {
        log.info("初始化 chatChatModel Bean（从数据库系统默认配置或 yaml 兜底）");
        ChatModel model = factory.getSystemDefaultChatModel();
        log.info("chatChatModel Bean 初始化完成，运行时将由 DynamicLlmFactory 按用户配置动态切换");
        return model;
    }

    /**
     * 分析专用 ChatModel Bean（Spring 容器注册用）
     * <p>
     * 运行时 LlmService 通过 DynamicLlmFactory.getChatModel(userId) 动态获取用户专属实例。
     */
    @Bean("llmChatModel")
    public ChatModel llmChatModel(DynamicLlmFactory factory) {
        log.info("初始化 llmChatModel Bean（从数据库系统默认配置或 yaml 兜底）");
        ChatModel model = factory.getSystemDefaultChatModel();
        log.info("llmChatModel Bean 初始化完成");
        return model;
    }

    /**
     * 主 EmbeddingModel Bean（VectorStore/Qdrant 专用，系统级固定）
     * <p>
     * 固定使用 Spring AI 根据 spring.ai.openai.embedding 自动装配的 {@link OpenAiEmbeddingModel}：
     * <ul>
     *   <li>在线模式：直接调用 OpenAI 兼容 API（通义千问/DeepSeek/OpenAI 等）</li>
     *   <li>离线模式：将 spring.ai.openai.base-url 指向 Ollama OpenAI 兼容端口
     *       （如 http://localhost:11434/v1），无需单独配置 OllamaEmbeddingModel</li>
     * </ul>
     * 模型名和接入地址均来自 yaml，启动后不可动态修改（维护 Qdrant 向量维度一致性）。
     */
    @Bean("primaryEmbeddingModel")
    @Primary
    public EmbeddingModel primaryEmbeddingModel(OpenAiEmbeddingModel openAiEmbeddingModel) {
        log.info("初始化 primaryEmbeddingModel Bean（固定使用 OpenAiEmbeddingModel，配置来自 spring.ai.openai.embedding）");
        log.info("primaryEmbeddingModel Bean 初始化完成（系统级固定，确保 Qdrant 向量维度一致性）");
        return openAiEmbeddingModel;
    }
}
