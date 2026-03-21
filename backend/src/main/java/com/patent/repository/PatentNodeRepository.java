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
     * 删除专利节点及其所有关系
     */
    @Query("MATCH (p:Patent {publicationNo: $pubNo}) DETACH DELETE p")
    void deleteByPublicationNo(@Param("pubNo") String pubNo);
}
