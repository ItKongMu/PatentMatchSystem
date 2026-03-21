package com.patent.model.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j 领域概念节点
 * 对应知识图谱中的 Concept 节点类型
 * 用于表示应用场景、技术领域等抽象概念
 */
@Data
@Node("Concept")
public class ConceptNode {

    /**
     * 概念名称作为业务主键
     */
    @Id
    private String name;

    /**
     * 概念描述
     */
    private String description;

    /**
     * 关联概念（RELATED_TO 关系，带权重）
     */
    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.OUTGOING)
    private List<ConceptNode> relatedConcepts = new ArrayList<>();
}
