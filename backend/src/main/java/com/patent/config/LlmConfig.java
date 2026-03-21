package com.patent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LLM 模型配置 - 三模型分工架构
 * <p>
 * 基于 Spring AI 官方推荐的 Builder 模式（Context7 验证）：
 * - Ollama Chat:     {@code OllamaApi.builder().baseUrl(url).build()} + {@code OllamaChatModel.builder()}
 * - Ollama Embed:   {@code OllamaApi.builder().baseUrl(url).build()} + {@code new OllamaEmbeddingModel(api, options)}
 * - OpenAI Chat:    {@code OpenAiApi.builder().baseUrl(url).apiKey(key).build()} + {@code OpenAiChatModel.builder()}
 * - OpenAI Embed:   {@code OpenAiApi.builder().baseUrl(url).apiKey(key).build()} + {@code new OpenAiEmbeddingModel(api, mode, options)}
 * <p>
 * 三模型分工：
 * <ul>
 *   <li>{@code chatChatModel}          - 对话专用（ChatService），强推理：在线=qwen-max，离线=deepseek-r1:7b</li>
 *   <li>{@code llmChatModel}           - 分析专用（LlmService），高性价比：在线=qwen-plus，离线=qwen2.5:7b</li>
 *   <li>{@code primaryEmbeddingModel}  - 向量嵌入（VectorService），1024维统一：在线=text-embedding-v3，离线=bge-m3</li>
 * </ul>
 */
@Slf4j
@Configuration
public class LlmConfig {

    /** Ollama 服务地址（来自 spring.ai.ollama.base-url） */
    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    /** OpenAI 兼容接口地址（来自 spring.ai.openai.base-url） */
    @Value("${spring.ai.openai.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String openAiBaseUrl;

    /** OpenAI API Key（来自 spring.ai.openai.api-key） */
    @Value("${spring.ai.openai.api-key:placeholder}")
    private String openAiApiKey;

    /**
     * 对话专用 ChatModel（ChatService 注入，强推理）
     * - online: 使用 {@code patent.online.chatModel}（qwen-max / deepseek-chat）
     * - offline: 使用 {@code patent.ollama.chatModel}（deepseek-r1:7b）
     */
    @Bean("chatChatModel")
    @Primary
    public ChatModel chatChatModel(PatentConfig config) {
        String mode = config.getLlmMode();
        log.info("初始化 chatChatModel，模式: {}", mode);

        if ("offline".equals(mode)) {
            String model = config.getOllama().getChatModel();
            log.info("chatChatModel -> Ollama 对话模型: {}", model);
            return buildOllamaChat(model);
        }

        String model = config.getOnline().getChatModel();
        log.info("chatChatModel -> OpenAI 兼容对话模型: {}", model);
        return buildOpenAiChat(
                config.getOnline().getBaseUrl(),
                config.getOnline().getApiKey(),
                model
        );
    }

    /**
     * 分析专用 ChatModel（LlmService 注入，实体提取/匹配评估）
     * - online: 使用 {@code patent.online.llmModel}（qwen-plus，高性价比）
     * - offline: 使用 {@code patent.ollama.llmModel}（qwen2.5:7b，中文理解佳）
     */
    @Bean("llmChatModel")
    public ChatModel llmChatModel(PatentConfig config) {
        String mode = config.getLlmMode();
        log.info("初始化 llmChatModel，模式: {}", mode);

        if ("offline".equals(mode)) {
            String model = config.getOllama().getLlmModel();
            log.info("llmChatModel -> Ollama 分析模型: {}", model);
            return buildOllamaChat(model);
        }

        String model = config.getOnline().getLlmModel();
        log.info("llmChatModel -> OpenAI 兼容分析模型: {}", model);
        return buildOpenAiChat(
                config.getOnline().getBaseUrl(),
                config.getOnline().getApiKey(),
                model
        );
    }

    /**
     * 主 EmbeddingModel（VectorService / Qdrant VectorStore 注入）
     * <p>
     * 基于 Spring AI 官方文档（Context7 验证）直接构建，无需依赖 AutoConfiguration 注入：
     * - online:  使用 {@code new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options)}
     * - offline: 使用 {@code new OllamaEmbeddingModel(api, options)}
     * <p>
     * 两种模式均使用 1024 维，切换时无需重建 Qdrant Collection：
     * - online:  text-embedding-v3（1024维，通义千问/DashScope）
     * - offline: bge-m3（1024维，Ollama 本地）
     */
    @Bean("primaryEmbeddingModel")
    @Primary
    public EmbeddingModel primaryEmbeddingModel(PatentConfig config) {
        String mode = config.getLlmMode();
        log.info("初始化 primaryEmbeddingModel，模式: {}", mode);

        if ("offline".equals(mode)) {
            String embedModel = config.getOllama().getEmbedModel();
            log.info("primaryEmbeddingModel -> Ollama Embedding: {}", embedModel);
            return buildOllamaEmbedding(embedModel);
        }

        String embedModel = config.getOnline().getEmbedModel();
        log.info("primaryEmbeddingModel -> OpenAI Embedding: {}", embedModel);
        return buildOpenAiEmbedding(
                config.getOnline().getBaseUrl(),
                config.getOnline().getApiKey(),
                embedModel
        );
    }

    // ==================== 私有工厂方法（基于 Spring AI 官方 Builder 模式）====================

    /**
     * 构建 Ollama ChatModel
     * <p>
     * 参考官方文档（Context7 验证）：
     * {@code OllamaApi.builder().baseUrl(url).build()} +
     * {@code OllamaChatModel.builder().ollamaApi(api).defaultOptions(...).build()}
     *
     * @param modelName Ollama 模型名称（如 deepseek-r1:7b、qwen2.5:7b）
     */
    private OllamaChatModel buildOllamaChat(String modelName) {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(ollamaBaseUrl)
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder()
                        .model(modelName)
                        .temperature(0.7)
                        .build())
                .build();
    }

    /**
     * 构建 Ollama EmbeddingModel
     * <p>
     * 参考官方文档（Context7 验证）：
     * {@code OllamaApi.builder().baseUrl(url).build()} +
     * {@code OllamaEmbeddingModel.builder().ollamaApi(api).defaultOptions(...).build()}
     * <p>
     * Spring AI 1.0.3 中 {@code OllamaOptions} 同时适用于 Chat 和 Embedding
     *
     * @param modelName Ollama 嵌入模型名称（如 bge-m3，1024维）
     */
    private OllamaEmbeddingModel buildOllamaEmbedding(String modelName) {
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(ollamaBaseUrl)
                .build();
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaOptions.builder()
                        .model(modelName)
                        .build())
                .build();
    }

    /**
     * 构建 OpenAI 兼容 ChatModel
     * <p>
     * 参考官方文档（Context7 验证）：
     * {@code OpenAiApi.builder().baseUrl(url).apiKey(key).build()} +
     * {@code OpenAiChatModel.builder().openAiApi(api).defaultOptions(...).build()}
     * <p>
     * 支持通义千问、DeepSeek、OpenAI 等所有 OpenAI 兼容提供商
     *
     * @param baseUrl   API BaseURL
     * @param apiKey    API Key
     * @param modelName 模型名称
     */
    private OpenAiChatModel buildOpenAiChat(String baseUrl, String apiKey, String modelName) {
        String effectiveBaseUrl = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl : openAiBaseUrl;
        String effectiveApiKey = (apiKey != null && !apiKey.isBlank())
                ? apiKey : openAiApiKey;

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(effectiveBaseUrl)
                .apiKey(effectiveApiKey)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(0.7)
                        .build())
                .build();
    }

    /**
     * 构建 OpenAI 兼容 EmbeddingModel
     * <p>
     * 参考官方文档（Context7 验证）：
     * {@code OpenAiApi.builder().baseUrl(url).apiKey(key).build()} +
     * {@code new OpenAiEmbeddingModel(api, MetadataMode.EMBED, OpenAiEmbeddingOptions.builder().model(...).build())}
     * <p>
     * 支持通义千问 text-embedding-v3（1024维）等 OpenAI 兼容嵌入服务
     *
     * @param baseUrl   API BaseURL
     * @param apiKey    API Key
     * @param modelName 嵌入模型名称（如 text-embedding-v3，1024维）
     */
    private OpenAiEmbeddingModel buildOpenAiEmbedding(String baseUrl, String apiKey, String modelName) {
        String effectiveBaseUrl = (baseUrl != null && !baseUrl.isBlank())
                ? baseUrl : openAiBaseUrl;
        String effectiveApiKey = (apiKey != null && !apiKey.isBlank())
                ? apiKey : openAiApiKey;

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(effectiveBaseUrl)
                .apiKey(effectiveApiKey)
                .build();
        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(modelName)
                        .build()
        );
    }
}
