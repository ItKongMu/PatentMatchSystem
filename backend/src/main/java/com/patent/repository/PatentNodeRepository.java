package com.patent.repository;

import com.patent.model.graph.PatentNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 专利图谱节点 Repository
 */
@Repository
public interface PatentNodeRepository extends Neo4jRepository<PatentNode, String> {

    /**
     * 按公开号查询专利节点（仅查节点本身，用于写入时判断是否已存在）
     * 使用 @Query 显式指定属性匹配，避免 SDN 7.x 将 @Id 字段转为 elementId() 导致查询失效
     */
    @Query("MATCH (p:Patent {publicationNo: $publicationNo}) RETURN p")
    Optional<PatentNode> findByPublicationNo(@Param("publicationNo") String publicationNo);

    /**
     * 统计二跳关联专利数量
     * 通过共享实体/方法/组件关联的专利
     */
    @Query("MATCH (p:Patent {publicationNo: $pubNo})" +
           "-[:MENTIONS|USES_METHOD|HAS_COMPONENT]->(e)" +
           "<-[:MENTIONS|USES_METHOD|HAS_COMPONENT]-(related:Patent) " +
           "WHERE related.publicationNo <> $pubNo " +
           "RETURN count(DISTINCT related)")
    int countTwoHopRelatedPatents(@Param("pubNo") String pubNo);

    /**
     * 统计三跳关联专利数量
     */
    @Query("MATCH (p:Patent {publicationNo: $pubNo})" +
           "-[:MENTIONS|USES_METHOD|HAS_COMPONENT]->(e)" +
           "-[:RELATED_TO|SAME_AS]->(e2)" +
           "<-[:MENTIONS|USES_METHOD|HAS_COMPONENT]-(related:Patent) " +
           "WHERE related.publicationNo <> $pubNo " +
           "RETURN count(DISTINCT related)")
    int countThreeHopRelatedPatents(@Param("pubNo") String pubNo);

    /**
     * 删除专利节点及其所有关系，并同步清理孤儿节点。
     *
     * <p>策略说明：
     * <ul>
     *   <li>先收集与该专利关联的 Entity / Applicant 候选节点</li>
     *   <li>使用 DETACH DELETE 删除专利节点及其所有关联关系（MENTIONS / HAS_IPC / FILED_BY）</li>
     *   <li>删除后检查候选节点是否已成为孤儿节点（无任何剩余关系），若是则一并删除</li>
     *   <li>IPC 节点属于共享分类体系（可能被多个专利引用，也可能有层级关系），保留不删除</li>
     * </ul>
     */
    @Query("MATCH (p:Patent {publicationNo: $pubNo}) " +
           "OPTIONAL MATCH (p)-[:MENTIONS]->(e:Entity) " +
           "OPTIONAL MATCH (p)-[:FILED_BY]->(a:Applicant) " +
           "WITH p, collect(DISTINCT e) AS entityCandidates, collect(DISTINCT a) AS applicantCandidates " +
           "DETACH DELETE p " +
           "WITH entityCandidates, applicantCandidates " +
           "CALL { " +
           "  WITH entityCandidates " +
           "  UNWIND entityCandidates AS e " +
           "  WITH e WHERE e IS NOT NULL AND NOT (e)--() " +
           "  DELETE e " +
           "} " +
           "CALL { " +
           "  WITH applicantCandidates " +
           "  UNWIND applicantCandidates AS a " +
           "  WITH a WHERE a IS NOT NULL AND NOT (a)--() " +
           "  DELETE a " +
           "}")
    void deleteByPublicationNo(@Param("pubNo") String pubNo);
}
