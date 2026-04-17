package com.patent.service.impl;

import com.patent.config.PatentConfig;
import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentDomain;
import com.patent.model.entity.PatentEntity;
import com.patent.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingProperties;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Qdrant 向量服务实现
 * <p>
 * VectorStore（Qdrant）使用 {@code primaryEmbeddingModel} Bean（由 {@link com.patent.config.LlmConfig} 从
 * application.yml 初始化）。向量存储和检索的实际 embedding 操作由 Spring AI 自动完成，此处无需手动调用 EmbeddingModel。
 * <p>
 * 向量嵌入模型固定使用 OpenAiEmbeddingModel，由 spring.ai.openai.embedding.options.model 统一配置，
 * 不受用户动态配置影响，保证 Qdrant 全局向量维度一致性。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

    private final VectorStore vectorStore;
    private final PatentConfig patentConfig;
    private final OpenAiEmbeddingProperties openAiEmbeddingProperties;

    @Override
    public String storePatentVector(Patent patent, List<PatentEntity> entities, List<PatentDomain> domains) {
        try {
            // 构造增强文本（标题 + 摘要 + 实体关键词）
            List<String> entityNames = entities.stream()
                    .map(PatentEntity::getEntityName)
                    .toList();
            List<String> entityTypes = entities.stream()
                    .map(PatentEntity::getEntityType)
                    .distinct()
                    .toList();

            String enhancedText = String.format(
                    "标题：%s\n摘要：%s\n关键技术：%s",
                    patent.getTitle(),
                    patent.getPatentAbstract() != null ? patent.getPatentAbstract() : "",
                    String.join("、", entityNames)
            );

            // 获取领域信息
            String domainSection = domains.stream()
                    .filter(d -> d.getDomainLevel() == 1)
                    .map(PatentDomain::getDomainCode)
                    .findFirst().orElse("");
            List<String> domainCodes = domains.stream()
                    .map(PatentDomain::getDomainCode)
                    .toList();

            // 创建Document对象（含元数据用于Qdrant过滤）
            String vectorId = UUID.randomUUID().toString();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("patent_id", patent.getId());
            metadata.put("publication_no", patent.getPublicationNo() != null ? patent.getPublicationNo() : "");
            metadata.put("title", patent.getTitle());
            metadata.put("applicant", patent.getApplicant() != null ? patent.getApplicant() : "");
            metadata.put("domain_section", domainSection);
            metadata.put("domain_codes", String.join(",", domainCodes));
            metadata.put("entities", String.join(",", entityNames));
            metadata.put("entity_types", String.join(",", entityTypes));
            metadata.put("embedding_model", getEmbeddingModelName());

            Document document = new Document(vectorId, enhancedText, metadata);

            // 存储到Qdrant
            vectorStore.add(List.of(document));

            log.info("专利向量存储成功, patentId: {}, vectorId: {}", patent.getId(), vectorId);
            return vectorId;

        } catch (Exception e) {
            log.error("专利向量存储失败, patentId: {}", patent.getId(), e);
            throw new RuntimeException("向量存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Document> semanticSearch(String queryText, String domainFilter, int topK) {
        return semanticSearch(queryText, domainFilter, topK, null);
    }

    @Override
    public List<Document> semanticSearch(String queryText, String domainFilter, int topK, Long excludePatentId) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            FilterExpressionBuilder.Op filterExpr = null;

            // 构造过滤条件
            if (domainFilter != null && !domainFilter.isEmpty() && excludePatentId != null) {
                // 同时过滤领域和排除自身
                filterExpr = b.and(
                        b.eq("domain_section", domainFilter),
                        b.ne("patent_id", excludePatentId)
                );
            } else if (domainFilter != null && !domainFilter.isEmpty()) {
                filterExpr = b.eq("domain_section", domainFilter);
            } else if (excludePatentId != null) {
                filterExpr = b.ne("patent_id", excludePatentId);
            }

            SearchRequest searchRequest;
            if (filterExpr != null) {
                searchRequest = SearchRequest.builder()
                        .query(queryText)
                        .topK(topK)
                        .similarityThreshold(patentConfig.getSimilarityThreshold())
                        .filterExpression(filterExpr.build())
                        .build();
            } else {
                searchRequest = SearchRequest.builder()
                        .query(queryText)
                        .topK(topK)
                        .similarityThreshold(patentConfig.getSimilarityThreshold())
                        .build();
            }

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("语义检索完成，查询: {}, 结果数: {}, 阈值: {}, 排除专利ID: {}",
                    queryText.substring(0, Math.min(50, queryText.length())),
                    results.size(),
                    patentConfig.getSimilarityThreshold(),
                    excludePatentId);

            return results;

        } catch (Exception e) {
            log.error("语义检索失败", e);
            return List.of();
        }
    }

    @Override
    public void deleteVector(String vectorId) {
        try {
            vectorStore.delete(List.of(vectorId));
            log.info("向量删除成功: {}", vectorId);
        } catch (Exception e) {
            log.error("向量删除失败: {}", vectorId, e);
        }
    }

    @Override
    public Map<Long, String> batchStorePatentVectors(List<PatentVectorData> patentDataList) {
        if (patentDataList == null || patentDataList.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> resultMap = new HashMap<>();
        List<Document> documents = new java.util.ArrayList<>();

        try {
            for (PatentVectorData data : patentDataList) {
                Patent patent = data.patent();
                List<PatentEntity> entities = data.entities();
                List<PatentDomain> domains = data.domains();

                // 构造增强文本
                List<String> entityNames = entities.stream()
                        .map(PatentEntity::getEntityName)
                        .toList();
                List<String> entityTypes = entities.stream()
                        .map(PatentEntity::getEntityType)
                        .distinct()
                        .toList();

                String enhancedText = String.format(
                        "标题：%s\n摘要：%s\n关键技术：%s",
                        patent.getTitle(),
                        patent.getPatentAbstract() != null ? patent.getPatentAbstract() : "",
                        String.join("、", entityNames)
                );

                // 获取领域信息
                String domainSection = domains.stream()
                        .filter(d -> d.getDomainLevel() == 1)
                        .map(PatentDomain::getDomainCode)
                        .findFirst().orElse("");
                List<String> domainCodes = domains.stream()
                        .map(PatentDomain::getDomainCode)
                        .toList();

                // 创建Document对象
                String vectorId = UUID.randomUUID().toString();
                Map<String, Object> metadata = new HashMap<>();
                metadata.put("patent_id", patent.getId());
                metadata.put("publication_no", patent.getPublicationNo() != null ? patent.getPublicationNo() : "");
                metadata.put("title", patent.getTitle());
                metadata.put("applicant", patent.getApplicant() != null ? patent.getApplicant() : "");
                metadata.put("domain_section", domainSection);
                metadata.put("domain_codes", String.join(",", domainCodes));
                metadata.put("entities", String.join(",", entityNames));
                metadata.put("entity_types", String.join(",", entityTypes));
                metadata.put("embedding_model", getEmbeddingModelName());

                documents.add(new Document(vectorId, enhancedText, metadata));
                resultMap.put(patent.getId(), vectorId);
            }

            // 批量存储到Qdrant
            vectorStore.add(documents);
            log.info("批量向量存储成功, 数量: {}", documents.size());

            return resultMap;

        } catch (Exception e) {
            log.error("批量向量存储失败, 数量: {}", patentDataList.size(), e);
            throw new RuntimeException("批量向量存储失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取当前系统使用的嵌入模型名称（用于记录向量元数据）
     * <p>
     * 固定从 spring.ai.openai.embedding.options.model 读取，
     * 与 primaryEmbeddingModel Bean（OpenAiEmbeddingModel）来源完全一致。
     */
    private String getEmbeddingModelName() {
        return openAiEmbeddingProperties.getOptions().getModel();
    }
}
