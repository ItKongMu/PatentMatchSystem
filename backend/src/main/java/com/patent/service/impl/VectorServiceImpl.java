package com.patent.service.impl;

import com.patent.config.PatentConfig;
import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentDomain;
import com.patent.model.entity.PatentEntity;
import com.patent.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * Qdrant向量服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorServiceImpl implements VectorService {

    private final VectorStore vectorStore;
    private final PatentConfig patentConfig;

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
        try {
            SearchRequest searchRequest;
            
            // 可选：领域过滤
            if (domainFilter != null && !domainFilter.isEmpty()) {
                FilterExpressionBuilder b = new FilterExpressionBuilder();
                searchRequest = SearchRequest.builder()
                        .query(queryText)
                        .topK(topK)
                        .similarityThreshold(patentConfig.getSimilarityThreshold())
                        .filterExpression(b.eq("domain_section", domainFilter).build())
                        .build();
            } else {
                searchRequest = SearchRequest.builder()
                        .query(queryText)
                        .topK(topK)
                        .similarityThreshold(patentConfig.getSimilarityThreshold())
                        .build();
            }

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            log.info("语义检索完成，查询: {}, 结果数: {}", 
                    queryText.substring(0, Math.min(50, queryText.length())), results.size());
            
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

    /**
     * 获取当前使用的嵌入模型名称
     */
    private String getEmbeddingModelName() {
        return "online".equals(patentConfig.getLlmMode()) 
                ? "text-embedding-v3" 
                : "nomic-embed-text";
    }
}
