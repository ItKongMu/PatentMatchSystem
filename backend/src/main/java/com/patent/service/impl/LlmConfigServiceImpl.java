package com.patent.service.impl;

import com.patent.common.exception.BusinessException;
import com.patent.config.PatentConfig;
import com.patent.mapper.LlmConfigMapper;
import com.patent.model.dto.LlmConfigDTO;
import com.patent.model.entity.SysLlmConfig;
import com.patent.model.vo.LlmConfigVO;
import com.patent.service.LlmConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * LLM 配置管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmConfigServiceImpl implements LlmConfigService {

    private final LlmConfigMapper llmConfigMapper;
    private final PatentConfig patentConfig;

    // API Key 脱敏：只保留前4位和后4位
    private static final int KEY_MASK_KEEP = 4;

    @Override
    public List<LlmConfigVO> listConfigs(Long userId) {
        // 合并返回：系统默认配置（user_id=0）+ 用户自定义配置
        // 系统配置排前面，用户配置排后面
        List<SysLlmConfig> configs = llmConfigMapper.findAllWithSystem(userId);
        return configs.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public LlmConfigVO getActiveConfig(Long userId) {
        SysLlmConfig config = llmConfigMapper.findActiveByUserId(userId);
        if (config == null) {
            // 回退到系统默认配置
            config = llmConfigMapper.findActiveByUserId(0L);
        }
        return config != null ? toVO(config) : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LlmConfigVO saveConfig(Long userId, boolean isAdmin, LlmConfigDTO dto) {
        validateConfig(dto);

        SysLlmConfig config;
        if (dto.getId() != null) {
            // 更新操作
            config = llmConfigMapper.selectById(dto.getId());
            if (config == null || config.getDeleted() == 1) {
                throw new BusinessException("配置不存在");
            }
            // 系统默认配置（user_id=0）仅管理员可修改
            if (config.getUserId() == 0L && !isAdmin) {
                throw new BusinessException("系统默认配置不允许修改，请新增自定义配置");
            }
            // 非管理员只允许操作自己的配置
            if (!isAdmin && !config.getUserId().equals(userId)) {
                throw new BusinessException("无权修改此配置");
            }
        } else {
            // 新增操作：用户可以新增离线或在线配置
            // 管理员新增时若指定为系统配置则 userId=0，否则为自己
            config = new SysLlmConfig();
            // isSystemConfig=true 且管理员 → userId=0
            if (Boolean.TRUE.equals(dto.getIsSystemConfig()) && isAdmin) {
                config.setUserId(0L);
            } else {
                config.setUserId(userId);
            }
            config.setIsActive(0);
            config.setCreatedAt(LocalDateTime.now());
        }

        // 填充字段（API Key 为空时保留原值）
        config.setConfigName(dto.getConfigName());
        config.setLlmMode(dto.getLlmMode());
        config.setBaseUrl(dto.getBaseUrl());
        config.setChatModel(dto.getChatModel());
        config.setLlmModel(dto.getLlmModel());
        config.setEmbedModel(dto.getEmbedModel());
        config.setOllamaUrl(dto.getOllamaUrl());
        config.setRemark(dto.getRemark());
        config.setUpdatedAt(LocalDateTime.now());

        // API Key：空字符串表示清空，null 表示不修改
        if (dto.getApiKey() != null) {
            config.setApiKey(dto.getApiKey().isBlank() ? null : dto.getApiKey());
        }

        if (dto.getId() != null) {
            llmConfigMapper.updateById(config);
        } else {
            llmConfigMapper.insert(config);
        }

        log.info("保存 LLM 配置成功: userId={}, configId={}, configName={}", userId, config.getId(), config.getConfigName());
        return toVO(config);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateConfig(Long userId, boolean isAdmin, Long configId) {
        SysLlmConfig config = llmConfigMapper.selectById(configId);
        if (config == null || config.getDeleted() == 1) {
            throw new BusinessException("配置不存在");
        }
        // 系统配置由管理员激活（userId=0维度），用户配置由用户激活（userId维度）
        boolean isSystemConfig = config.getUserId() == 0L;
        if (isSystemConfig) {
            if (!isAdmin) {
                throw new BusinessException("无权激活系统默认配置");
            }
            // 管理员激活系统配置：先取消所有系统配置的激活状态
            llmConfigMapper.deactivateAllByUserId(0L);
        } else {
            if (!isAdmin && !config.getUserId().equals(userId)) {
                throw new BusinessException("无权激活此配置");
            }
            // 取消当前用户所有激活状态
            llmConfigMapper.deactivateAllByUserId(config.getUserId());
        }

        // 再激活目标配置
        config.setIsActive(1);
        config.setUpdatedAt(LocalDateTime.now());
        llmConfigMapper.updateById(config);

        log.info("激活 LLM 配置: userId={}, configId={}, configName={}", userId, configId, config.getConfigName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(Long userId, boolean isAdmin, Long configId) {
        SysLlmConfig config = llmConfigMapper.selectById(configId);
        if (config == null || config.getDeleted() == 1) {
            throw new BusinessException("配置不存在");
        }
        // 系统默认配置仅管理员可删除
        if (config.getUserId() == 0L && !isAdmin) {
            throw new BusinessException("系统默认配置不允许删除");
        }
        // 非管理员只能删除自己的配置
        if (!isAdmin && !config.getUserId().equals(userId)) {
            throw new BusinessException("无权删除此配置");
        }
        if (config.getIsActive() == 1) {
            throw new BusinessException("当前启用的配置不能删除，请先切换到其他配置");
        }

        config.setDeleted(1);
        config.setUpdatedAt(LocalDateTime.now());
        llmConfigMapper.updateById(config);

        log.info("删除 LLM 配置: userId={}, configId={}", userId, configId);
    }

    @Override
    public Map<String, Object> testConnection(LlmConfigDTO dto) {
        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            String testPrompt = "请回复'连接测试成功'，不要输出其他内容。";
            String response;

            if ("offline".equals(dto.getLlmMode())) {
                // 测试 Ollama 连接
                String ollamaUrl = StringUtils.hasText(dto.getOllamaUrl())
                        ? dto.getOllamaUrl()
                        : "http://localhost:11434";
                String chatModel = StringUtils.hasText(dto.getChatModel())
                        ? dto.getChatModel()
                        : patentConfig.getOllama().getChatModel();

                // 使用 Spring AI 官方推荐的 Builder 模式（Context7 验证）
                OllamaApi ollamaApi = OllamaApi.builder()
                        .baseUrl(ollamaUrl)
                        .build();
                OllamaChatModel ollamaChatModel = OllamaChatModel.builder()
                        .ollamaApi(ollamaApi)
                        .defaultOptions(OllamaOptions.builder()
                                .model(chatModel)
                                .temperature(0.1)
                                .build())
                        .build();

                response = ChatClient.builder(ollamaChatModel)
                        .build()
                        .prompt()
                        .user(testPrompt)
                        .call()
                        .content();

            } else {
                // 测试在线 API 连接
                String baseUrl = StringUtils.hasText(dto.getBaseUrl())
                        ? dto.getBaseUrl()
                        : patentConfig.getOnline().getBaseUrl();
                String apiKey = StringUtils.hasText(dto.getApiKey())
                        ? dto.getApiKey()
                        : patentConfig.getOnline().getApiKey();
                String chatModel = StringUtils.hasText(dto.getChatModel())
                        ? dto.getChatModel()
                        : patentConfig.getOnline().getChatModel();

                if (!StringUtils.hasText(apiKey)) {
                    throw new BusinessException("在线模式需要提供 API Key");
                }

                OpenAiApi openAiApi = OpenAiApi.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .build();
                OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .defaultOptions(OpenAiChatOptions.builder()
                                .model(chatModel)
                                .temperature(0.1)
                                .build())
                        .build();

                response = ChatClient.builder(openAiChatModel)
                        .build()
                        .prompt()
                        .user(testPrompt)
                        .call()
                        .content();
            }

            long elapsed = System.currentTimeMillis() - startTime;
            result.put("success", true);
            result.put("message", "连接测试成功");
            result.put("response", response);
            result.put("responseTimeMs", elapsed);
            log.info("LLM 连接测试成功: mode={}, elapsed={}ms", dto.getLlmMode(), elapsed);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            result.put("success", false);
            result.put("message", "连接测试失败: " + getReadableError(e));
            result.put("responseTimeMs", elapsed);
            log.warn("LLM 连接测试失败: mode={}, error={}", dto.getLlmMode(), e.getMessage());
        }

        return result;
    }

    @Override
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        String mode = patentConfig.getLlmMode();
        status.put("currentMode", mode);
        status.put("vectorDimension", patentConfig.getVectorDimension());

        if ("offline".equals(mode)) {
            status.put("chatModel", patentConfig.getOllama().getChatModel());
            status.put("llmModel", patentConfig.getOllama().getLlmModel());
            status.put("embedModel", patentConfig.getOllama().getEmbedModel());
            status.put("description", "离线模式 - Ollama 本地推理");
        } else {
            status.put("baseUrl", patentConfig.getOnline().getBaseUrl());
            status.put("chatModel", patentConfig.getOnline().getChatModel());
            status.put("llmModel", patentConfig.getOnline().getLlmModel());
            status.put("embedModel", patentConfig.getOnline().getEmbedModel());
            status.put("hasApiKey", StringUtils.hasText(patentConfig.getOnline().getApiKey()));
            status.put("description", "在线模式 - OpenAI 兼容 API");
        }

        return status;
    }

    // ==================== 私有方法 ====================

    /**
     * 将实体转换为 VO（API Key 脱敏）
     */
    private LlmConfigVO toVO(SysLlmConfig config) {
        return LlmConfigVO.builder()
                .id(config.getId())
                .userId(config.getUserId())
                .configName(config.getConfigName())
                .llmMode(config.getLlmMode())
                .baseUrl(config.getBaseUrl())
                .apiKeyMasked(maskApiKey(config.getApiKey()))
                .hasApiKey(StringUtils.hasText(config.getApiKey()))
                .chatModel(config.getChatModel())
                .llmModel(config.getLlmModel())
                .embedModel(config.getEmbedModel())
                .ollamaUrl(config.getOllamaUrl())
                .isActive(config.getIsActive() == 1)
                .isSystemConfig(config.getUserId() == 0L)
                .remark(config.getRemark())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }

    /**
     * API Key 脱敏处理
     * 如 sk-abcdefgh1234 → sk-****1234
     */
    private String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return null;
        }
        if (apiKey.length() <= KEY_MASK_KEEP * 2) {
            return "****";
        }
        String prefix = apiKey.substring(0, KEY_MASK_KEEP);
        String suffix = apiKey.substring(apiKey.length() - KEY_MASK_KEEP);
        return prefix + "****" + suffix;
    }

    /**
     * 配置校验
     */
    private void validateConfig(LlmConfigDTO dto) {
        if ("online".equals(dto.getLlmMode())) {
            if (!StringUtils.hasText(dto.getBaseUrl())) {
                throw new BusinessException("在线模式需要填写 API BaseURL");
            }
            if (!StringUtils.hasText(dto.getChatModel())) {
                throw new BusinessException("在线模式需要填写对话模型名称");
            }
        } else if ("offline".equals(dto.getLlmMode())) {
            if (!StringUtils.hasText(dto.getChatModel()) && !StringUtils.hasText(dto.getLlmModel())) {
                throw new BusinessException("离线模式至少需要填写一个模型名称");
            }
        }
    }

    /**
     * 将异常转换为可读错误信息
     */
    private String getReadableError(Exception e) {
        String msg = e.getMessage();
        if (msg == null) return "未知错误";
        if (msg.contains("Connection refused")) return "连接被拒绝，请检查服务地址是否正确";
        if (msg.contains("timeout") || msg.contains("timed out")) return "连接超时，请检查网络或服务状态";
        if (msg.contains("401") || msg.contains("Unauthorized")) return "API Key 无效，请检查认证信息";
        if (msg.contains("403") || msg.contains("Forbidden")) return "API Key 权限不足";
        if (msg.contains("404")) return "API 地址不存在，请检查 BaseURL 是否正确";
        if (msg.contains("model")) return "模型名称不存在或未部署: " + msg;
        return msg.length() > 100 ? msg.substring(0, 100) + "..." : msg;
    }
}

