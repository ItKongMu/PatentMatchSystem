package com.patent.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.patent.config.PatentConfig;
import com.patent.model.entity.Patent;
import com.patent.service.LlmService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM服务实现
 * 支持在线模式（通义千问）和离线模式（Ollama）
 */
@Slf4j
@Service
public class LlmServiceImpl implements LlmService {

    private final ChatClient chatClient;
    private final PatentConfig patentConfig;

    /**
     * 实体提取Prompt模板
     */
    private static final String ENTITY_EXTRACTION_PROMPT = """
            你是专利技术分析专家，请从以下专利文本中提取实体和领域信息。

            专利文本：
            %s

            请按JSON格式输出（只输出JSON，不要其他解释）：
            {
              "entities": [
                {"text": "实体名称", "type": "PRODUCT|METHOD|MATERIAL|COMPONENT|EFFECT|APPLICATION", "importance": "high|medium|low"}
              ],
              "domain": {
                "section": "IPC部(如G)",
                "mainClass": "IPC大类(如06)",
                "subclass": "IPC小类(如F)",
                "fullCode": "完整IPC编码(如G06F16/30)",
                "description": "领域描述"
              },
              "keywords": ["关键词1", "关键词2"]
            }

            实体类型说明：
            - PRODUCT: 产品/设备/装置
            - METHOD: 方法/工艺/流程
            - MATERIAL: 材料/物质
            - COMPONENT: 组件/部件
            - EFFECT: 效果/性能
            - APPLICATION: 应用场景/用途
            """;

    /**
     * 匹配评估Prompt模板 - 增强版，生成详细匹配报告
     */
    private static final String MATCH_EVALUATION_PROMPT = """
            你是专利技术分析专家，请详细评估以下专利与查询需求的技术匹配度。

            【查询需求】
            %s

            【查询实体】
            %s

            【候选专利】
            标题：%s
            摘要：%s

            请进行深入分析并返回JSON格式（只输出JSON，不要其他解释）：
            {
              "score": 85,
              "reason": "一句话概括匹配原因",
              "matchedEntities": ["匹配实体1", "匹配实体2"],
              "domainMatched": true,
              "explanation": {
                "overallAnalysis": "整体匹配分析，说明为什么这两者技术上相关（2-3句话）",
                "entityMatches": [
                  {
                    "queryEntity": "查询中的实体名",
                    "matchedEntity": "专利中匹配的实体名",
                    "entityType": "PRODUCT|METHOD|MATERIAL|COMPONENT|EFFECT|APPLICATION",
                    "similarity": 90,
                    "matchReason": "为什么这两个实体匹配"
                  }
                ],
                "technicalSimilarity": {
                  "methodSimilarity": 80,
                  "structureSimilarity": 75,
                  "effectSimilarity": 85,
                  "keyDifference": "技术方案的关键差异点"
                },
                "innovationPoint": "该专利相对于查询需求的创新点或独特之处",
                "applicationScenario": "两者在应用场景上的相似性分析"
              }
            }

            评分标准：
            - 90-100：技术方案高度相似，核心实体完全匹配
            - 70-89：技术方案相似，主要实体匹配
            - 50-69：技术方案部分相关，存在一定关联
            - 30-49：技术方案关联较弱
            - 0-29：技术方案基本无关
            """;

    public LlmServiceImpl(@Qualifier("llmChatModel") ChatModel chatModel,
                          PatentConfig patentConfig) {
        this.patentConfig = patentConfig;
        this.chatClient = ChatClient.builder(chatModel).build();
        log.info("LLM分析服务初始化完成，模式: {}，分析模型: {}",
                patentConfig.getLlmMode(),
                "offline".equals(patentConfig.getLlmMode())
                        ? patentConfig.getOllama().getLlmModel()
                        : patentConfig.getOnline().getLlmModel());
    }

    @Override
    public PatentAnalysisResult extractEntitiesAndDomain(String patentText) {
        // 限制输入文本长度，避免超时
        String truncatedText = truncateText(patentText, 4000);
        
        try {
            String prompt = String.format(ENTITY_EXTRACTION_PROMPT, truncatedText);
            
            log.info("开始调用LLM进行实体提取，文本长度: {}", truncatedText.length());
            long startTime = System.currentTimeMillis();
            
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("LLM实体提取完成，耗时: {}ms", elapsed);
            log.debug("LLM实体提取响应: {}", response);
            
            // 解析JSON响应
            return parseAnalysisResult(response);
            
        } catch (Exception e) {
            log.error("LLM实体提取失败: {}", e.getMessage());
            // 返回空结果而不是抛出异常，保证流程继续
            return new PatentAnalysisResult(List.of(), null, List.of());
        }
    }
    
    /**
     * 截断文本到指定长度
     */
    private String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...（已截断）";
    }

    @Override
    public MatchScoreResult evaluateMatch(String query, String queryEntities, Patent candidatePatent) {
        try {
            String prompt = String.format(MATCH_EVALUATION_PROMPT, 
                    query, 
                    queryEntities,
                    candidatePatent.getTitle(),
                    candidatePatent.getPatentAbstract());
            
            String response = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.debug("LLM匹配评估响应: {}", response);
            
            return parseMatchResult(response);
            
        } catch (Exception e) {
            log.error("LLM匹配评估失败", e);
            // 返回默认评分
            return new MatchScoreResult(50, "评估失败，使用默认评分", List.of(), false);
        }
    }

    /**
     * 解析实体分析结果
     */
    private PatentAnalysisResult parseAnalysisResult(String jsonStr) {
        try {
            // 清理JSON字符串（移除markdown代码块标记）
            String cleanJson = cleanJsonResponse(jsonStr);
            JSONObject json = JSON.parseObject(cleanJson);
            
            // 解析实体
            List<EntityInfo> entities = new ArrayList<>();
            JSONArray entitiesArray = json.getJSONArray("entities");
            if (entitiesArray != null) {
                for (int i = 0; i < entitiesArray.size(); i++) {
                    JSONObject entity = entitiesArray.getJSONObject(i);
                    entities.add(new EntityInfo(
                            entity.getString("text"),
                            entity.getString("type"),
                            entity.getString("importance")
                    ));
                }
            }
            
            // 解析领域
            DomainInfo domain = null;
            JSONObject domainObj = json.getJSONObject("domain");
            if (domainObj != null) {
                domain = new DomainInfo(
                        domainObj.getString("section"),
                        domainObj.getString("mainClass"),
                        domainObj.getString("subclass"),
                        domainObj.getString("fullCode"),
                        domainObj.getString("description")
                );
            }
            
            // 解析关键词
            List<String> keywords = new ArrayList<>();
            JSONArray keywordsArray = json.getJSONArray("keywords");
            if (keywordsArray != null) {
                keywords = keywordsArray.toJavaList(String.class);
            }
            
            return new PatentAnalysisResult(entities, domain, keywords);
            
        } catch (Exception e) {
            log.error("解析LLM响应失败: {}", jsonStr, e);
            return new PatentAnalysisResult(List.of(), null, List.of());
        }
    }

    /**
     * 解析匹配评分结果 - 增强版
     */
    private MatchScoreResult parseMatchResult(String jsonStr) {
        try {
            String cleanJson = cleanJsonResponse(jsonStr);
            JSONObject json = JSON.parseObject(cleanJson);
            
            int score = json.getIntValue("score", 50);
            String reason = json.getString("reason");
            boolean domainMatched = json.getBooleanValue("domainMatched", false);
            
            List<String> matchedEntities = new ArrayList<>();
            JSONArray entitiesArray = json.getJSONArray("matchedEntities");
            if (entitiesArray != null) {
                matchedEntities = entitiesArray.toJavaList(String.class);
            }
            
            // 解析详细解释（增强功能）
            MatchExplanation explanation = parseExplanation(json.getJSONObject("explanation"));
            
            return new MatchScoreResult(score, reason, matchedEntities, domainMatched, explanation);
            
        } catch (Exception e) {
            log.error("解析匹配结果失败: {}", jsonStr, e);
            return new MatchScoreResult(50, "解析失败", List.of(), false, null);
        }
    }

    /**
     * 解析详细匹配解释
     */
    private MatchExplanation parseExplanation(JSONObject explanationObj) {
        if (explanationObj == null) {
            return null;
        }
        
        try {
            String overallAnalysis = explanationObj.getString("overallAnalysis");
            String innovationPoint = explanationObj.getString("innovationPoint");
            String applicationScenario = explanationObj.getString("applicationScenario");
            
            // 解析实体匹配详情
            List<EntityMatchDetail> entityMatches = new ArrayList<>();
            JSONArray entityMatchesArray = explanationObj.getJSONArray("entityMatches");
            if (entityMatchesArray != null) {
                for (int i = 0; i < entityMatchesArray.size(); i++) {
                    JSONObject em = entityMatchesArray.getJSONObject(i);
                    entityMatches.add(new EntityMatchDetail(
                            em.getString("queryEntity"),
                            em.getString("matchedEntity"),
                            em.getString("entityType"),
                            em.getIntValue("similarity", 0),
                            em.getString("matchReason")
                    ));
                }
            }
            
            // 解析技术相似性
            TechnicalSimilarity technicalSimilarity = null;
            JSONObject techObj = explanationObj.getJSONObject("technicalSimilarity");
            if (techObj != null) {
                technicalSimilarity = new TechnicalSimilarity(
                        techObj.getIntValue("methodSimilarity", 0),
                        techObj.getIntValue("structureSimilarity", 0),
                        techObj.getIntValue("effectSimilarity", 0),
                        techObj.getString("keyDifference")
                );
            }
            
            return new MatchExplanation(
                    overallAnalysis,
                    entityMatches,
                    technicalSimilarity,
                    innovationPoint,
                    applicationScenario
            );
            
        } catch (Exception e) {
            log.warn("解析详细解释失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 清理JSON响应（移除markdown代码块等）
     */
    private String cleanJsonResponse(String response) {
        if (response == null) return "{}";
        
        String cleaned = response.trim();
        // 移除markdown代码块
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }
        return cleaned.trim();
    }
}
