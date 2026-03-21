package com.patent.repository;

import com.patent.model.graph.EntityNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 技术实体图谱节点 Repository
 */
@Repository
public interface EntityNodeRepository extends Neo4jRepository<EntityNode, String> {

    /**
     * 按实体名称查询实体节点（仅查节点本身，用于写入时判断是否已存在）
     * 使用 @Query 显式指定属性匹配，避免 SDN 7.x 将 @Id 字段映射为 elementId() 导致查询失效
     */
    @Query("MATCH (e:Entity {name: $name}) RETURN e")
    Optional<EntityNode> findByName(@Param("name") String name);

    /**
     * 查询与某实体关联的所有专利公开号
     * MENTIONS 关系方向：Patent -[:MENTIONS]-> Entity（写入时方向）
     * 同时兼容无方向查询，确保双向都能命中
     */
    @Query("MATCH (p:Patent)-[:MENTIONS]-(e:Entity {name: $entityName}) RETURN p.publicationNo")
    List<String> findRelatedPatentNos(@Param("entityName") String entityName);

    /**
     * 按实体类型查询
     */
    List<EntityNode> findByEntityType(String entityType);
}
