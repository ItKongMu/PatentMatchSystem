package com.patent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * LLM模型配置
 * 支持通义千问（在线，通过OpenAI兼容接口）和Ollama（离线）双模式
 */
@Slf4j
@Configuration
public class LlmConfig {

    /**
     * 主ChatModel - 根据配置自动选择可用的模型
     */
    @Bean("primaryChatModel")
    @Primary
    public ChatModel primaryChatModel(
            @Autowired(required = false) OpenAiChatModel openAiChatModel,
            @Autowired(required = false) OllamaChatModel ollamaChatModel,
            PatentConfig patentConfig) {
        
        String llmMode = patentConfig.getLlmMode();
        
        // offline模式优先使用Ollama
        if ("offline".equals(llmMode)) {
            if (ollamaChatModel != null) {
                log.info("使用Ollama作为主ChatModel");
                return ollamaChatModel;
            }
            log.warn("Ollama不可用，尝试回退到OpenAI兼容接口");
        }
        
        // online模式优先使用OpenAI兼容接口
        if ("online".equals(llmMode) && openAiChatModel != null) {
            log.info("使用OpenAI兼容接口(通义千问)作为主ChatModel");
            return openAiChatModel;
        }
        
        // 回退逻辑
        if (ollamaChatModel != null) {
            log.info("回退使用Ollama作为ChatModel");
            return ollamaChatModel;
        } else if (openAiChatModel != null) {
            log.info("回退使用OpenAI兼容接口作为ChatModel");
            return openAiChatModel;
        }
        
        throw new IllegalStateException("没有可用的ChatModel，请检查OpenAI/DashScope或Ollama配置");
    }

    /**
     * 主EmbeddingModel - 根据配置自动选择可用的嵌入模型
     * 解决Qdrant VectorStore自动配置时多个EmbeddingModel Bean冲突的问题
     */
    @Bean("primaryEmbeddingModel")
    @Primary
    public EmbeddingModel primaryEmbeddingModel(
            @Autowired(required = false) OpenAiEmbeddingModel openAiEmbeddingModel,
            @Autowired(required = false) OllamaEmbeddingModel ollamaEmbeddingModel,
            PatentConfig patentConfig) {
        
        String llmMode = patentConfig.getLlmMode();
        log.info("当前LLM模式: {}", llmMode);
        
        // offline模式优先使用Ollama
        if ("offline".equals(llmMode)) {
            if (ollamaEmbeddingModel != null) {
                log.info("使用Ollama作为主EmbeddingModel");
                return ollamaEmbeddingModel;
            }
            log.warn("Ollama不可用，尝试回退到OpenAI兼容接口");
        }
        
        // online模式优先使用OpenAI兼容接口
        if ("online".equals(llmMode) && openAiEmbeddingModel != null) {
            log.info("使用OpenAI兼容接口(通义千问)作为主EmbeddingModel");
            return openAiEmbeddingModel;
        }
        
        // 回退逻辑
        if (ollamaEmbeddingModel != null) {
            log.info("回退使用Ollama作为EmbeddingModel");
            return ollamaEmbeddingModel;
        } else if (openAiEmbeddingModel != null) {
            log.info("回退使用OpenAI兼容接口作为EmbeddingModel");
            return openAiEmbeddingModel;
        }
        
        throw new IllegalStateException("没有可用的EmbeddingModel，请检查OpenAI/DashScope或Ollama配置");
    }
}
