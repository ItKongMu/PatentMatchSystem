package com.patent.service;

import com.patent.model.dto.GraphQueryDTO;
import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentDomain;
import com.patent.model.entity.PatentEntity;
import com.patent.model.vo.GraphVO;

import java.util.List;

/**
 * 知识图谱服务接口
 * 负责图谱节点/关系的增量写入、查询和评分增强
 */
public interface GraphService {

    /**
     * 将专利及其实体、领域信息写入 Neo4j 图谱（幂等 upsert）
     *
     * @param patent   专利基础信息
     * @param entities 专利实体列表（来自 patent_entity 表）
     * @param domains  专利领域列表（来自 patent_domain 表）
     */
    void upsertPatentGraph(Patent patent, List<PatentEntity> entities, List<PatentDomain> domains);

    /**
     * 删除专利图谱节点及其所有关系
     *
     * @param publicationNo 公开号
     */
    void deletePatentGraph(String publicationNo);

    /**
     * 查询某专利的邻接图谱（节点 + 关系）
     *
     * @param publicationNo 公开号
     * @return 图谱 VO（nodes + links）
     */
    GraphVO getPatentGraph(String publicationNo);

    /**
     * 查询某实体的关联图谱
     *
     * @param entityName 实体名称
     * @return 图谱 VO
     */
    GraphVO getEntityGraph(String entityName);

    /**
     * 查询某 IPC 分类的层次图谱及关联专利数
     *
     * @param ipcCode IPC 编码
     * @return 图谱 VO
     */
    GraphVO getIpcGraph(String ipcCode);

    /**
     * 通用图谱查询
     *
     * @param dto 查询参数
     * @return 图谱 VO
     */
    GraphVO queryGraph(GraphQueryDTO dto);

    /**
     * 计算图谱路径增强评分 S_graph
     * 公式：S_graph = 0.7 * N_2hop/(N_2hop+1) + 0.3 * N_3hop/(N_3hop+1)
     *
     * @param publicationNo    候选专利公开号
     * @param queryEntityNames 查询实体名称列表
     * @return 图谱路径分（0.0 ~ 1.0），Neo4j 不可用时返回 0.0
     */
    double calculateGraphScore(String publicationNo, List<String> queryEntityNames);
}
