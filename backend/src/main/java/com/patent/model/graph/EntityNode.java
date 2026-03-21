package com.patent.model.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j 技术实体节点
 * 对应知识图谱中的 Entity 节点类型
 * 包含：产品/方法/组件/材料/应用场景等技术实体
 */
@Data
@Node("Entity")
public class EntityNode {

    /**
     * 实体名称作为业务主键
     */
    @Id
    private String name;

    /**
     * 实体类型：PRODUCT/METHOD/MATERIAL/COMPONENT/EFFECT/APPLICATION
     */
    private String entityType;

    /**
     * 别名（逗号分隔）
     */
    private String alias;

    /**
     * 来源：LLM/MANUAL/IMPORT
     */
    private String source;

    /**
     * 同义词/别名关系（SAME_AS）
     */
    @Relationship(type = "SAME_AS", direction = Relationship.Direction.OUTGOING)
    private List<EntityNode> sameAs = new ArrayList<>();

    /**
     * 语义关联概念（RELATED_TO）
     */
    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.OUTGOING)
    private List<ConceptNode> relatedConcepts = new ArrayList<>();
}
