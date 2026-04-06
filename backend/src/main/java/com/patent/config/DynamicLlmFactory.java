package com.patent.config;

import com.patent.common.util.AesUtil;
import com.patent.mapper.LlmConfigMapper;
import com.patent.model.entity.SysLlmConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态 LLM 工厂（仅负责 ChatModel 动态路由）
 * <p>
 * 按 configId 缓存 ChatModel 实例，同一配置的 chat 和 llm（分析）场景各自独立缓存：
 * <ul>
 *   <li>key = configId（正数）：对话模型（使用 chatModel 字段）</li>
 *   <li>key = -configId（负数）：分析模型（使用 llmModel 字段，离线模式下可独立配置不同模型）</li>
 * </ul>
 * 每次请求时根据当前用户激活的配置动态获取对应的 ChatModel，
 * 无需重启服务即可在线切换不同 LLM 提供商（对话/分析模型）。
 * <p>
 * 注意：EmbeddingModel 不在此工厂管理，向量嵌入模型由
 * {@link LlmConfig#primaryEmbeddingModel} Bean 统一管理，
 * 固定使用 OpenAiEmbeddingModel，配置来源为 spring.ai.openai.embedding.options.model，
 * 保证 Qdrant 向量维度的全局一致性。
 * <p>
 * 缓存失效触发点：
 * <ul>
 *   <li>saveConfig() 修改配置后调用 {@link #invalidate(Long)}</li>
 *   <li>deleteConfig() 删除配置后调用 {@link #invalidate(Long)}</li>
 *   <li>管理员激活系统配置后调用 {@link #invalidateAll()}</li>
 * </ul>
 */
@Slf4j
@Component
public class DynamicLlmFactory {

    private final LlmConfigMapper llmConfigMapper;
    private final AesUtil aesUtil;
    private final PatentConfig patentConfig;

    /**
     * ChatModel 缓存：
     * - key = configId（正数）→ 对话模型（chatModel 字段）
     * - key = -configId（负数）→ 分析模型（llmModel 字段，离线模式下可与对话模型不同）
     */
    private final ConcurrentHashMap<Long, ChatModel> chatModelCache = new ConcurrentHashMap<>();

    /**
     * 已知不支持 Function Calling（tools）的 Ollama 模型名称前缀/全名集合。
     * deepseek-r1 系列、deepseek-v2 以及早期 llama2 均不支持 tools 协议。
     * 在线模式（OpenAI 兼容接口）通常由云厂商保证支持，不需要额外过滤。
     */
    private static final Set<String> NO_TOOL_MODELS = Set.of(
            "deepseek-r1",
            "deepseek-v2",
            "llama2",
            "mistral",
            "gemma",
            "phi3",
            "falcon"
    );

    public DynamicLlmFactory(LlmConfigMapper llmConfigMapper,
                              AesUtil aesUtil,
                              PatentConfig patentConfig) {
        this.llmConfigMapper = llmConfigMapper;
        this.aesUtil = aesUtil;
        this.patentConfig = patentConfig;
    }

    // ==================== 公共接口 ====================

    /**
     * 判断指定用户当前激活的对话模型是否支持 Function Calling（tools）。
     * <p>
     * 在线模式（OpenAI 兼容接口）默认视为支持；
     * 离线 Ollama 模式下，若模型名称前缀命中 {@link #NO_TOOL_MODELS}，则视为不支持。
     *
     * @param userId 当前登录用户 ID，null 时按系统默认配置判断
     * @return true = 支持工具调用；false = 不支持，应跳过 .defaultTools() 注册
     */
    public boolean supportsTools(Long userId) {
        SysLlmConfig config = resolveConfig(userId);
        if (config == null) {
            // 无配置时使用 yaml 兜底（OpenAI），默认支持
            return true;
        }
        if (!"offline".equals(config.getLlmMode())) {
            // 在线模式（OpenAI 兼容接口）：默认支持 tools
            return true;
        }
        // 离线 Ollama 模式：按模型名称前缀判断
        String modelName = (config.getChatModel() != null ? config.getChatModel() : "").toLowerCase();
        return NO_TOOL_MODELS.stream().noneMatch(modelName::startsWith);
    }

    /**
     * 获取指定用户的对话专用 ChatModel（ChatService 使用 chatModel 字段）
     * <p>
     * 优先查用户自己选择的配置，无选择则回退系统默认，
     * 系统默认也无时回退 yaml 兜底。
     *
     * @param userId 当前登录用户 ID，null 时直接回退系统默认
     * @return 对应配置的 ChatModel 实例（缓存命中则复用）
     */
    public ChatModel getChatModel(Long userId) {
        SysLlmConfig config = resolveConfig(userId);
        if (config == null) {
            log.warn("用户 {} 无可用 LLM 配置，使用 yaml 兜底（对话模型）", userId);
            return buildFallbackChatModel();
        }
        // 正数 key：对话模型
        return chatModelCache.computeIfAbsent(config.getId(), id -> {
            log.info("构建对话 ChatModel 缓存: configId={}, name={}, mode={}, model={}",
                    id, config.getConfigName(), config.getLlmMode(), config.getChatModel());
            return buildChatModel(config, false);
        });
    }

    /**
     * 获取指定用户的分析专用 ChatModel（LlmService 使用 llmModel 字段）
     * <p>
     * 离线模式下 llmModel 与 chatModel 可配置不同模型（如 chatModel=deepseek-r1:7b，
     * llmModel=qwen2.5:7b）。在线模式下 llmModel 与 chatModel 通常相同或共享同一接口。
     * <p>
     * 优先查用户自己选择的配置，无选择则回退系统默认。
     *
     * @param userId 当前登录用户 ID，null 时直接回退系统默认
     * @return 对应配置的分析 ChatModel 实例（缓存命中则复用）
     */
    public ChatModel getLlmModel(Long userId) {
        SysLlmConfig config = resolveConfig(userId);
        if (config == null) {
            log.warn("用户 {} 无可用 LLM 配置，使用 yaml 兜底（分析模型）", userId);
            return buildFallbackChatModel();
        }
        // 负数 key：分析模型，与对话模型缓存隔离
        long analysisCacheKey = -config.getId();
        return chatModelCache.computeIfAbsent(analysisCacheKey, id -> {
            log.info("构建分析 ChatModel 缓存: configId={}, name={}, mode={}, model={}",
                    config.getId(), config.getConfigName(), config.getLlmMode(), config.getLlmModel());
            return buildChatModel(config, true);
        });
    }

    /**
     * 按系统默认配置获取对话 ChatModel（用于启动时初始化 Spring Bean）
     *
     * @return 系统默认 ChatModel，无数据库配置则使用 yaml 兜底
     */
    public ChatModel getSystemDefaultChatModel() {
        SysLlmConfig config = llmConfigMapper.findSystemDefault();
        if (config == null) {
            log.info("数据库无系统默认 LLM 配置，使用 yaml 兜底");
            return buildFallbackChatModel();
        }
        log.info("使用数据库系统默认配置初始化 ChatModel: configId={}, name={}",
                config.getId(), config.getConfigName());
        return chatModelCache.computeIfAbsent(config.getId(), id -> buildChatModel(config, false));
    }

    /**
     * 使配置缓存失效（saveConfig / deleteConfig 后调用）
     * 同时清除该 configId 的对话模型缓存（正数 key）和分析模型缓存（负数 key）。
     *
     * @param configId 被修改或删除的配置 ID
     */
    public void invalidate(Long configId) {
        if (configId == null) return;
        ChatModel removedChat = chatModelCache.remove(configId);
        ChatModel removedLlm = chatModelCache.remove(-configId);
        log.info("LLM 缓存已失效: configId={}, chatModel={}, llmModel={}",
                configId,
                removedChat != null ? "已清除" : "未缓存",
                removedLlm != null ? "已清除" : "未缓存");
    }

    /**
     * 清空所有缓存（管理员全局重置时使用）
     */
    public void invalidateAll() {
        int count = chatModelCache.size();
        chatModelCache.clear();
        log.info("已清空所有 LLM 缓存: {} 个条目", count);
    }

    /**
     * 获取当前系统默认配置的模式（online/offline），用于 UI 展示。
     * 结果直接从数据库读取，不走 ChatModel 缓存（配置元数据层）。
     */
    public String getSystemDefaultMode() {
        SysLlmConfig config = llmConfigMapper.findSystemDefault();
        if (config != null) return config.getLlmMode();
        return patentConfig.getLlmMode();
    }

    // ==================== 内部逻辑 ====================

    /**
     * 解析当前用户的激活配置：
     * 1. 优先查用户自己选择的配置（user_llm_selection）
     * 2. 无则回退系统默认（is_active=1 的 user_id=0 配置）
     */
    private SysLlmConfig resolveConfig(Long userId) {
        if (userId != null) {
            SysLlmConfig userConfig = llmConfigMapper.findSelectedByUserId(userId);
            if (userConfig != null) {
                return userConfig;
            }
        }
        return llmConfigMapper.findSystemDefault();
    }

    // ==================== 模型构建 ====================

    /**
     * 根据数据库配置构建 ChatModel。
     *
     * @param config      LLM 配置实体
     * @param useAnalysis true 时使用 llmModel 字段（分析场景），false 使用 chatModel 字段（对话场景）
     */
    private ChatModel buildChatModel(SysLlmConfig config, boolean useAnalysis) {
        if ("offline".equals(config.getLlmMode())) {
            String ollamaUrl = StringUtils.hasText(config.getOllamaUrl())
                    ? config.getOllamaUrl() : "http://localhost:11434";
            // 分析场景优先用 llmModel，回退 chatModel，再回退默认值
            String modelName;
            if (useAnalysis) {
                modelName = StringUtils.hasText(config.getLlmModel())
                        ? config.getLlmModel()
                        : (StringUtils.hasText(config.getChatModel()) ? config.getChatModel() : "qwen2.5:7b");
            } else {
                modelName = StringUtils.hasText(config.getChatModel())
                        ? config.getChatModel() : "deepseek-r1:7b";
            }
            return buildOllamaChat(ollamaUrl, modelName);
        } else {
            String baseUrl = StringUtils.hasText(config.getBaseUrl())
                    ? config.getBaseUrl() : "https://dashscope.aliyuncs.com/compatible-mode";
            String apiKey = decryptApiKey(config.getApiKey());
            // 在线模式：分析场景优先用 llmModel，回退 chatModel
            String modelName;
            if (useAnalysis) {
                modelName = StringUtils.hasText(config.getLlmModel())
                        ? config.getLlmModel()
                        : (StringUtils.hasText(config.getChatModel()) ? config.getChatModel() : "qwen-plus");
            } else {
                modelName = StringUtils.hasText(config.getChatModel())
                        ? config.getChatModel() : "qwen-plus";
            }
            return buildOpenAiChat(baseUrl, apiKey, modelName);
        }
    }

    /**
     * 数据库无任何可用配置时的兜底处理。
     * <p>
     * LLM 配置全部由管理员在前端配置并激活，此处仅作安全兜底，
     * 返回一个无效占位模型并打印警告（实际调用时会因参数无效而报错，
     * 提示管理员在系统设置中添加并激活一个 LLM 配置）。
     */
    private ChatModel buildFallbackChatModel() {
        log.error("数据库中无任何 LLM 配置，请管理员在「系统设置 → LLM配置」中添加并激活一个配置！");
        // 返回一个占位 OpenAI 模型，实际请求时会因 api-key 无效而失败，触发前端错误提示
        return buildOpenAiChat(
                "https://dashscope.aliyuncs.com/compatible-mode",
                "placeholder",
                "qwen-plus"
        );
    }

    // ==================== 底层构建方法 ====================

    private OllamaChatModel buildOllamaChat(String baseUrl, String modelName) {
        OllamaApi api = OllamaApi.builder().baseUrl(baseUrl).build();
        return OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(OllamaOptions.builder()
                        .model(modelName)
                        .temperature(0.7)
                        .build())
                .build();
    }

    private OpenAiChatModel buildOpenAiChat(String baseUrl, String apiKey, String modelName) {
        String effectiveBase = StringUtils.hasText(baseUrl) ? baseUrl
                : "https://dashscope.aliyuncs.com/compatible-mode";
        String effectiveKey = StringUtils.hasText(apiKey) ? apiKey : "placeholder";
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(effectiveBase)
                .apiKey(effectiveKey)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(modelName)
                        .temperature(0.7)
                        .build())
                .build();
    }

    /**
     * 解密 API Key（数据库中 AES 加密存储）
     * null 或空则返回 "placeholder"（离线模式不需要 key）
     */
    private String decryptApiKey(String encryptedKey) {
        if (!StringUtils.hasText(encryptedKey)) return "placeholder";
        try {
            return aesUtil.decrypt(encryptedKey);
        } catch (Exception e) {
            log.warn("API Key 解密失败，使用 placeholder: {}", e.getMessage());
            return "placeholder";
        }
    }
}
