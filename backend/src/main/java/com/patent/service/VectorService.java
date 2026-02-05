package com.patent.service;

import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentDomain;
import com.patent.model.entity.PatentEntity;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * 向量服务接口（Qdrant）
 */
public interface VectorService {

    /**
     * 存储专利向量
     *
     * @param patent   专利信息
     * @param entities 实体列表
     * @param domains  领域列表
     * @return Qdrant向量ID
     */
    String storePatentVector(Patent patent, List<PatentEntity> entities, List<PatentDomain> domains);

    /**
     * 语义检索
     *
     * @param queryText    查询文本
     * @param domainFilter 领域过滤（IPC部，可选）
     * @param topK         返回数量
     * @return 相似文档列表
     */
    List<Document> semanticSearch(String queryText, String domainFilter, int topK);

    /**
     * 删除专利向量
     *
     * @param vectorId Qdrant向量ID
     */
    void deleteVector(String vectorId);

    /**
     * 批量存储专利向量
     *
     * @param patentDataList 专利数据列表（Patent, entities, domains）
     * @return 向量ID映射（patentId -> vectorId）
     */
    java.util.Map<Long, String> batchStorePatentVectors(List<PatentVectorData> patentDataList);

    /**
     * 专利向量数据封装类
     */
    record PatentVectorData(Patent patent, List<PatentEntity> entities, List<PatentDomain> domains) {}
}
