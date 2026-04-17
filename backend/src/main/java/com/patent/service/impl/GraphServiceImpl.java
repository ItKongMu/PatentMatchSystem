package com.patent.service.impl;

import com.patent.model.dto.GraphQueryDTO;
import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentDomain;
import com.patent.model.entity.PatentEntity;
import com.patent.model.graph.ApplicantNode;
import com.patent.model.graph.EntityNode;
import com.patent.model.graph.IpcNode;
import com.patent.model.graph.PatentNode;
import com.patent.model.graph.rel.MentionedEntityRel;
import com.patent.model.vo.GraphVO;
import com.patent.repository.ApplicantNodeRepository;
import com.patent.repository.EntityNodeRepository;
import com.patent.repository.IpcNodeRepository;
import com.patent.repository.PatentNodeRepository;
import com.patent.service.GraphService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
// Neo4j 事务管理器 bean 名称为 "neo4jTransactionManager"（Spring Boot 自动配置）

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 知识图谱服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphServiceImpl implements GraphService {

    private final PatentNodeRepository patentNodeRepository;
    private final IpcNodeRepository ipcNodeRepository;
    private final EntityNodeRepository entityNodeRepository;
    private final ApplicantNodeRepository applicantNodeRepository;
    private final Neo4jClient neo4jClient;

    // ==================== 写入 ====================

    @Override
    @Transactional("neo4jTransactionManager")
    public void upsertPatentGraph(Patent patent, List<PatentEntity> entities, List<PatentDomain> domains) {
        if (patent == null || !StringUtils.hasText(patent.getPublicationNo())) {
            log.warn("专利公开号为空，跳过图谱写入");
            return;
        }

        String pubNo = patent.getPublicationNo();
        log.info("开始写入专利图谱: {}", pubNo);

        // 1. 构建或更新 PatentNode
        PatentNode patentNode = patentNodeRepository.findByPublicationNo(pubNo)
                .orElse(new PatentNode());
        patentNode.setPublicationNo(pubNo);
        patentNode.setTitle(patent.getTitle());
        patentNode.setAbstractText(patent.getPatentAbstract());
        patentNode.setApplicant(patent.getApplicant());
        patentNode.setSourceType(patent.getSourceType());
        if (patent.getPublicationDate() != null) {
            patentNode.setPublicationDate(patent.getPublicationDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
        }

        // 2. 处理申请人节点（FILED_BY）
        if (StringUtils.hasText(patent.getApplicant())) {
            ApplicantNode applicantNode = applicantNodeRepository
                    .findByApplicantName(patent.getApplicant())
                    .orElse(new ApplicantNode());
            applicantNode.setApplicantName(patent.getApplicant());
            applicantNodeRepository.save(applicantNode);
            patentNode.setApplicantNode(applicantNode);
        }

        // 3. 处理 IPC 节点（HAS_IPC）
        List<IpcNode> ipcNodes = new ArrayList<>();
        List<String> domainCodes = new ArrayList<>();
        for (PatentDomain domain : domains) {
            if (!StringUtils.hasText(domain.getDomainCode())) continue;
            String ipcCode = domain.getDomainCode().trim();
            domainCodes.add(ipcCode);

            IpcNode ipcNode = ipcNodeRepository.findByIpcCode(ipcCode)
                    .orElse(new IpcNode());
            ipcNode.setIpcCode(ipcCode);
            ipcNode.setLevel(domain.getDomainLevel());
            // 组合 code + desc，如 "A61K9/70 医药、牙科或梳妆..."，确保节点名称完整可识别
            String combinedName = ipcCode;
            if (StringUtils.hasText(domain.getDomainDesc())) {
                combinedName = ipcCode + " " + domain.getDomainDesc().trim();
            }
            ipcNode.setName(combinedName);
            ipcNode.setParentCode(resolveParentCode(ipcCode));
            ipcNodeRepository.save(ipcNode);
            ipcNodes.add(ipcNode);

            // 建立 IPC 层次关系（PARENT_OF）
            buildIpcHierarchy(ipcNode);
        }
        patentNode.setDomainCodes(domainCodes);
        patentNode.setIpcNodes(ipcNodes);

        // 4. 处理实体节点（MENTIONS）
        List<MentionedEntityRel> mentionedRels = new ArrayList<>();
        for (PatentEntity entity : entities) {
            if (!StringUtils.hasText(entity.getEntityName())) continue;

            EntityNode entityNode = entityNodeRepository.findByName(entity.getEntityName())
                    .orElse(new EntityNode());
            entityNode.setName(entity.getEntityName());
            entityNode.setEntityType(entity.getEntityType());
            entityNode.setSource("LLM");
            entityNodeRepository.save(entityNode);

            MentionedEntityRel rel = new MentionedEntityRel();
            rel.setEntity(entityNode);
            rel.setConfidence(importanceToConfidence(entity.getImportance()));
            mentionedRels.add(rel);
        }
        patentNode.setMentionedEntities(mentionedRels);

        // 5. 保存专利节点（含所有关系）
        patentNodeRepository.save(patentNode);
        log.info("专利图谱写入完成: {}, 实体数: {}, IPC数: {}", pubNo, mentionedRels.size(), ipcNodes.size());
    }

    @Override
    @Transactional("neo4jTransactionManager")
    public void deletePatentGraph(String publicationNo) {
        if (!StringUtils.hasText(publicationNo)) return;
        patentNodeRepository.deleteByPublicationNo(publicationNo);
        log.info("专利图谱节点已删除: {}", publicationNo);
    }

    // ==================== 查询 ====================

    @Override
    public GraphVO getPatentGraph(String publicationNo) {
        GraphVO vo = new GraphVO();
        if (!StringUtils.hasText(publicationNo)) return vo;

        log.info("查询专利图谱: {}", publicationNo);

        // 使用 Neo4jClient 直接执行 Cypher，绕过 SDN OGM 映射问题
        try {
            Collection<Map<String, Object>> results = neo4jClient.query(
                    "MATCH (p:Patent {publicationNo: $pubNo}) " +
                    "OPTIONAL MATCH (p)-[r1:MENTIONS]->(e:Entity) " +
                    "OPTIONAL MATCH (p)-[r2:HAS_IPC]->(i:IPC) " +
                    "OPTIONAL MATCH (p)-[r3:FILED_BY]->(a:Applicant) " +
                    "RETURN p.publicationNo AS pubNo, p.title AS title, " +
                    "       collect(DISTINCT {name: e.name, type: e.entityType, conf: r1.confidence}) AS entities, " +
                    "       collect(DISTINCT {code: i.ipcCode, name: i.name}) AS ipcs, " +
                    "       collect(DISTINCT a.applicantName) AS applicants"
            ).bind(publicationNo).to("pubNo")
             .fetch().all();

            if (results.isEmpty()) {
                log.warn("图谱中未找到专利: {}", publicationNo);
                return vo;
            }

            for (Map<String, Object> row : results) {
                String pubNo = toString(row.get("pubNo"));
                String title = toString(row.get("title"));
                if (pubNo == null) continue;

                String patentNodeId = "Patent:" + pubNo;
                vo.getNodes().add(GraphVO.NodeVO.of("Patent", pubNo,
                        title != null ? title : pubNo));

                // 实体节点
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> entities = (List<Map<String, Object>>) row.get("entities");
                if (entities != null) {
                    for (Map<String, Object> e : entities) {
                        String eName = toString(e.get("name"));
                        if (eName == null) continue;
                        String eType = toString(e.get("type"));
                        String conf = e.get("conf") != null ? String.format("%.2f", ((Number) e.get("conf")).doubleValue()) : null;
                        String entityNodeId = "Entity:" + eName;
                        if (!containsNode(vo.getNodes(), entityNodeId)) {
                            vo.getNodes().add(GraphVO.NodeVO.of("Entity", eName, eName, eType));
                        }
                        vo.getLinks().add(GraphVO.LinkVO.of(patentNodeId, entityNodeId, "MENTIONS", conf));
                    }
                }

                // IPC 节点
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> ipcs = (List<Map<String, Object>>) row.get("ipcs");
                if (ipcs != null) {
                    for (Map<String, Object> ipc : ipcs) {
                        String ipcCode = toString(ipc.get("code"));
                        if (ipcCode == null) continue;
                        String ipcName = toString(ipc.get("name"));
                        String ipcNodeId = "IPC:" + ipcCode;
                        if (!containsNode(vo.getNodes(), ipcNodeId)) {
                            vo.getNodes().add(GraphVO.NodeVO.of("IPC", ipcCode,
                                    ipcName != null ? ipcName : ipcCode));
                        }
                        vo.getLinks().add(GraphVO.LinkVO.of(patentNodeId, ipcNodeId, "HAS_IPC"));
                    }
                }

                // 申请人节点
                @SuppressWarnings("unchecked")
                List<String> applicants = (List<String>) row.get("applicants");
                if (applicants != null) {
                    for (String applicantName : applicants) {
                        if (applicantName == null) continue;
                        String applicantNodeId = "Applicant:" + applicantName;
                        if (!containsNode(vo.getNodes(), applicantNodeId)) {
                            vo.getNodes().add(GraphVO.NodeVO.of("Applicant", applicantName, applicantName));
                        }
                        vo.getLinks().add(GraphVO.LinkVO.of(patentNodeId, applicantNodeId, "FILED_BY"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("专利图谱查询失败: {}", publicationNo, e);
        }

        log.info("专利图谱查询完成: {}, 节点数: {}, 关系数: {}", publicationNo, vo.getNodes().size(), vo.getLinks().size());
        return vo;
    }

    @Override
    public GraphVO getEntityGraph(String entityName) {
        GraphVO vo = new GraphVO();
        if (!StringUtils.hasText(entityName)) return vo;

        log.info("查询实体图谱: {}", entityName);

        try {
            Collection<Map<String, Object>> results = neo4jClient.query(
                    "MATCH (e:Entity {name: $entityName}) " +
                    "OPTIONAL MATCH (p:Patent)-[r:MENTIONS]->(e) " +
                    "OPTIONAL MATCH (e)-[:SAME_AS]->(s:Entity) " +
                    "RETURN e.name AS eName, e.entityType AS eType, " +
                    "       collect(DISTINCT p.publicationNo) AS patentNos, " +
                    "       collect(DISTINCT s.name) AS sameAs"
            ).bind(entityName).to("entityName")
             .fetch().all();

            if (results.isEmpty()) {
                log.warn("图谱中未找到实体: {}", entityName);
                return vo;
            }

            for (Map<String, Object> row : results) {
                String eName = toString(row.get("eName"));
                if (eName == null) continue;
                String eType = toString(row.get("eType"));
                String entityNodeId = "Entity:" + eName;
                vo.getNodes().add(GraphVO.NodeVO.of("Entity", eName, eName, eType));

                // 关联专利
                @SuppressWarnings("unchecked")
                List<String> patentNos = (List<String>) row.get("patentNos");
                if (patentNos != null) {
                    for (String pubNo : patentNos) {
                        if (pubNo == null) continue;
                        String patentNodeId = "Patent:" + pubNo;
                        if (!containsNode(vo.getNodes(), patentNodeId)) {
                            vo.getNodes().add(GraphVO.NodeVO.of("Patent", pubNo, pubNo));
                        }
                        vo.getLinks().add(GraphVO.LinkVO.of(patentNodeId, entityNodeId, "MENTIONS"));
                    }
                }

                // 同义词
                @SuppressWarnings("unchecked")
                List<String> sameAsList = (List<String>) row.get("sameAs");
                if (sameAsList != null) {
                    for (String sName : sameAsList) {
                        if (sName == null) continue;
                        String sameNodeId = "Entity:" + sName;
                        if (!containsNode(vo.getNodes(), sameNodeId)) {
                            vo.getNodes().add(GraphVO.NodeVO.of("Entity", sName, sName));
                        }
                        vo.getLinks().add(GraphVO.LinkVO.of(entityNodeId, sameNodeId, "SAME_AS"));
                    }
                }
            }
        } catch (Exception e) {
            log.error("实体图谱查询失败: {}", entityName, e);
        }

        log.info("实体图谱查询完成: {}, 节点数: {}, 关系数: {}", entityName, vo.getNodes().size(), vo.getLinks().size());
        return vo;
    }

    @Override
    public GraphVO getIpcGraph(String ipcCode) {
        GraphVO vo = new GraphVO();
        if (!StringUtils.hasText(ipcCode)) return vo;

        log.info("查询IPC图谱: {}", ipcCode);

        try {
            // 查询当前节点及其父节点、子节点
            Collection<Map<String, Object>> results = neo4jClient.query(
                    "MATCH (i:IPC {ipcCode: $ipcCode}) " +
                    "OPTIONAL MATCH (child:IPC)-[:PARENT_OF]->(i) " +
                    "OPTIONAL MATCH (i)-[:PARENT_OF]->(parent:IPC) " +
                    "OPTIONAL MATCH (p:Patent)-[:HAS_IPC]->(i) " +
                    "RETURN i.ipcCode AS code, i.name AS name, " +
                    "       count(DISTINCT p) AS patentCount, " +
                    "       collect(DISTINCT {code: child.ipcCode, name: child.name}) AS children, " +
                    "       parent.ipcCode AS parentCode, parent.name AS parentName"
            ).bind(ipcCode).to("ipcCode")
             .fetch().all();

            if (results.isEmpty()) {
                log.warn("图谱中未找到IPC节点: {}", ipcCode);
                return vo;
            }

            for (Map<String, Object> row : results) {
                String code = toString(row.get("code"));
                if (code == null) continue;
                String name = toString(row.get("name"));
                long patentCount = row.get("patentCount") != null ? ((Number) row.get("patentCount")).longValue() : 0;

                String ipcNodeId = "IPC:" + code;
                vo.getNodes().add(GraphVO.NodeVO.of("IPC", code,
                        name != null ? name : code, "专利数:" + patentCount));

                // 子节点
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) row.get("children");
                if (children != null) {
                    for (Map<String, Object> child : children) {
                        String childCode = toString(child.get("code"));
                        if (childCode == null) continue;
                        String childName = toString(child.get("name"));
                        String childNodeId = "IPC:" + childCode;
                        if (!containsNode(vo.getNodes(), childNodeId)) {
                            vo.getNodes().add(GraphVO.NodeVO.of("IPC", childCode,
                                    childName != null ? childName : childCode));
                        }
                        vo.getLinks().add(GraphVO.LinkVO.of(childNodeId, ipcNodeId, "PARENT_OF"));
                    }
                }

                // 父节点
                String parentCode = toString(row.get("parentCode"));
                if (parentCode != null) {
                    String parentName = toString(row.get("parentName"));
                    String parentNodeId = "IPC:" + parentCode;
                    if (!containsNode(vo.getNodes(), parentNodeId)) {
                        vo.getNodes().add(GraphVO.NodeVO.of("IPC", parentCode,
                                parentName != null ? parentName : parentCode));
                    }
                    vo.getLinks().add(GraphVO.LinkVO.of(ipcNodeId, parentNodeId, "PARENT_OF"));
                }
            }
        } catch (Exception e) {
            log.error("IPC图谱查询失败: {}", ipcCode, e);
        }

        log.info("IPC图谱查询完成: {}, 节点数: {}, 关系数: {}", ipcCode, vo.getNodes().size(), vo.getLinks().size());
        return vo;
    }

    @Override
    public GraphVO queryGraph(GraphQueryDTO dto) {
        GraphVO vo = new GraphVO();
        if (dto == null) return vo;

        String nodeType    = dto.getNodeType();
        String effectiveKey = dto.getEffectiveKey();
        boolean hasType = StringUtils.hasText(nodeType);
        boolean hasKey  = StringUtils.hasText(effectiveKey);

        // nodeType 和 key 都为空时无法查询
        if (!hasType && !hasKey) {
            return vo;
        }

        int depth = dto.getDepth() != null ? Math.min(dto.getDepth(), 3) : 2;
        // 无关键词时（按类型浏览）默认 limit=25，有关键词时 limit=50
        int defaultLimit = hasKey ? 50 : 25;
        int limit = dto.getLimit() != null ? Math.min(dto.getLimit(), 200) : defaultLimit;

        log.info("通用图谱查询: nodeType={}, key={}, depth={}, limit={}", nodeType, effectiveKey, depth, limit);

        try {
            Collection<Map<String, Object>> results;

            if (!hasKey) {
                // ---- 浏览模式：仅按类型返回节点（不做路径遍历，避免全量扫描） ----
                String cypher = buildBrowseCypher(nodeType, depth, limit);
                results = neo4jClient.query(cypher).fetch().all();
            } else {
                // ---- 搜索模式：按关键词做路径遍历 ----
                String cypher = buildGenericCypher(nodeType, depth, limit);
                results = neo4jClient.query(cypher)
                        .bind(effectiveKey).to("nodeKey")
                        .fetch()
                        .all();
            }

            log.info("通用图谱查询结果行数: {}", results.size());

            for (Map<String, Object> row : results) {
                String startLabel = toString(row.get("startLabel"));
                String startKey   = toString(row.get("startKey"));
                String startName  = toString(row.get("startName"));
                String endLabel   = toString(row.get("endLabel"));
                String endKey     = toString(row.get("endKey"));
                String endName    = toString(row.get("endName"));
                String relType    = toString(row.get("relType"));

                if (startLabel != null && startKey != null) {
                    String nodeId = startLabel + ":" + startKey;
                    String nodeName = startName != null ? startName : startKey;
                    if (!containsNode(vo.getNodes(), nodeId)) {
                        vo.getNodes().add(GraphVO.NodeVO.of(startLabel, startKey, nodeName));
                    }
                }
                if (endLabel != null && endKey != null && relType != null) {
                    String srcId   = startLabel + ":" + startKey;
                    String dstId   = endLabel   + ":" + endKey;
                    String dstName = endName != null ? endName : endKey;
                    if (!containsNode(vo.getNodes(), dstId)) {
                        vo.getNodes().add(GraphVO.NodeVO.of(endLabel, endKey, dstName));
                    }
                    vo.getLinks().add(GraphVO.LinkVO.of(srcId, dstId, relType));
                }
            }
        } catch (Exception e) {
            log.error("通用图谱查询失败: nodeType={}, nodeKey={}", dto.getNodeType(), dto.getNodeKey(), e);
        }

        return vo;
    }

    // ==================== 评分 ====================

    @Override
    public double calculateGraphScore(String publicationNo, List<String> queryEntityNames) {
        if (!StringUtils.hasText(publicationNo)) return 0.0;

        try {
            int n2hop = patentNodeRepository.countTwoHopRelatedPatents(publicationNo);
            int n3hop = patentNodeRepository.countThreeHopRelatedPatents(publicationNo);

            // S_graph = 0.7 * N_2hop/(N_2hop+1) + 0.3 * N_3hop/(N_3hop+1)
            double s2 = (double) n2hop / (n2hop + 1);
            double s3 = (double) n3hop / (n3hop + 1);
            double score = 0.7 * s2 + 0.3 * s3;

            log.debug("图谱评分: pubNo={}, n2hop={}, n3hop={}, score={}", publicationNo, n2hop, n3hop, score);
            return score;
        } catch (Exception e) {
            log.warn("图谱评分计算失败，降级为 0.0: pubNo={}, error={}", publicationNo, e.getMessage());
            return 0.0;
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 根据 IPC 编码推断父节点编码
     */
    private String resolveParentCode(String ipcCode) {
        if (!StringUtils.hasText(ipcCode)) return null;
        String code = ipcCode.trim();
        if (code.contains("/")) {
            return code.substring(0, code.indexOf("/"));
        }
        if (code.length() > 4) return code.substring(0, 4);
        if (code.length() == 4) return code.substring(0, 3);
        if (code.length() == 3) return code.substring(0, 1);
        return null;
    }

    /**
     * 建立 IPC 层次关系（PARENT_OF）
     */
    private void buildIpcHierarchy(IpcNode ipcNode) {
        String parentCode = ipcNode.getParentCode();
        if (!StringUtils.hasText(parentCode)) return;

        IpcNode parent = ipcNodeRepository.findByIpcCode(parentCode)
                .orElse(new IpcNode());
        if (!StringUtils.hasText(parent.getIpcCode())) {
            parent.setIpcCode(parentCode);
            parent.setParentCode(resolveParentCode(parentCode));
        }
        ipcNodeRepository.save(parent);
        ipcNode.setParent(parent);
    }

    /**
     * 重要性转置信度
     */
    private double importanceToConfidence(String importance) {
        if (importance == null) return 0.5;
        return switch (importance.toLowerCase()) {
            case "high"   -> 0.9;
            case "medium" -> 0.7;
            case "low"    -> 0.5;
            default       -> 0.6;
        };
    }

    /**
     * 构建通用图谱查询 Cypher（有关键词时，做路径遍历）
     * nodeType 为空时匹配所有节点类型
     */
    private String buildGenericCypher(String nodeType, int depth, int limit) {
        // 节点标签部分：有类型则加标签过滤，否则不加
        String nodeLabel = StringUtils.hasText(nodeType) ? ":" + nodeType : "";
        return String.format(
                "MATCH path = (n%s)-[r*1..%d]-(m) " +
                "WHERE (n.publicationNo = $nodeKey OR n.ipcCode = $nodeKey " +
                "   OR n.name = $nodeKey OR n.applicantName = $nodeKey) " +
                "WITH nodes(path) as ns, relationships(path) as rels " +
                "UNWIND range(0, size(ns)-2) as i " +
                "WITH ns[i] as sn, ns[i+1] as en, rels[i] as rel " +
                "RETURN labels(sn)[0] as startLabel, " +
                "       coalesce(sn.publicationNo, sn.ipcCode, sn.name, sn.applicantName) as startKey, " +
                "       coalesce(sn.title, sn.name, sn.ipcCode, sn.applicantName) as startName, " +
                "       labels(en)[0] as endLabel, " +
                "       coalesce(en.publicationNo, en.ipcCode, en.name, en.applicantName) as endKey, " +
                "       coalesce(en.title, en.name, en.ipcCode, en.applicantName) as endName, " +
                "       type(rel) as relType " +
                "LIMIT %d",
                nodeLabel, depth, limit
        );
    }

    /**
     * 构建浏览模式 Cypher（无关键词，仅按类型返回节点及其直接关系）
     * 避免全量路径遍历导致性能问题
     */
    private String buildBrowseCypher(String nodeType, int depth, int limit) {
        String nodeLabel = StringUtils.hasText(nodeType) ? ":" + nodeType : "";
        // 浏览模式：先取样本节点，再展开1跳邻居，控制数量
        return String.format(
                "MATCH (n%s) " +
                "WITH n LIMIT %d " +
                "OPTIONAL MATCH (n)-[r]->(m) " +
                "RETURN labels(n)[0] as startLabel, " +
                "       coalesce(n.publicationNo, n.ipcCode, n.name, n.applicantName) as startKey, " +
                "       coalesce(n.title, n.name, n.ipcCode, n.applicantName) as startName, " +
                "       labels(m)[0] as endLabel, " +
                "       coalesce(m.publicationNo, m.ipcCode, m.name, m.applicantName) as endKey, " +
                "       coalesce(m.title, m.name, m.ipcCode, m.applicantName) as endName, " +
                "       type(r) as relType",
                nodeLabel, limit
        );
    }

    /**
     * 检查节点列表中是否已包含指定 ID 的节点
     */
    private boolean containsNode(List<GraphVO.NodeVO> nodes, String id) {
        return nodes.stream().anyMatch(n -> id.equals(n.getId()));
    }

    /**
     * 安全转 String
     */
    private String toString(Object obj) {
        return obj != null ? obj.toString() : null;
    }
}
