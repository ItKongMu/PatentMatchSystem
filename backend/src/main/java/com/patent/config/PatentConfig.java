package com.patent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 专利系统配置类
 * 支持在线模式（OpenAI兼容API）和离线模式（Ollama）三模型分工架构
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "patent")
public class PatentConfig {

    /**
     * LLM模式：online-在线API / offline-Ollama本地
     */
    private String llmMode = "offline";

    /**
     * 匹配结果数量
     */
    private Integer matchTopK = 10;

    /**
     * 相似度阈值
     */
    private Double similarityThreshold = 0.6;

    /**
     * 向量维度
     * text-embedding-v3 (通义千问) / bge-m3 (Ollama) 均为 1024 维
     * nomic-embed-text (Ollama) 为 768 维
     */
    private Integer vectorDimension = 1024;

    /**
     * Ollama 离线模式三模型配置
     */
    private OllamaConfig ollama = new OllamaConfig();

    /**
     * 在线 API 三模型配置（OpenAI 兼容）
     */
    private OnlineConfig online = new OnlineConfig();

    /**
     * Ollama 离线模式配置
     */
    @Data
    public static class OllamaConfig {
        /**
         * 对话模型（ChatService 使用）- 强推理能力
         */
        private String chatModel = "deepseek-r1:7b";

        /**
         * 分析模型（LlmService 使用）- 实体提取/匹配评估，高性价比
         */
        private String llmModel = "qwen2.5:7b";

        /**
         * 向量嵌入模型（VectorService 使用）- 1024维，与在线模式兼容
         */
        private String embedModel = "bge-m3";
    }

    /**
     * 在线 API 模式配置（OpenAI 兼容接口）
     */
    @Data
    public static class OnlineConfig {
        /**
         * API BaseURL，支持通义千问/DeepSeek/OpenAI等兼容提供商
         */
        private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode";

        /**
         * API Key（加密存储建议使用环境变量）
         */
        private String apiKey = "";

        /**
         * 对话模型（ChatService 使用）- 强推理
         */
        private String chatModel = "qwen-max";

        /**
         * 分析模型（LlmService 使用）- 高性价比
         */
        private String llmModel = "qwen-plus";

        /**
         * 向量嵌入模型（VectorService 使用）- 1024维
         */
        private String embedModel = "text-embedding-v3";
    }
}
