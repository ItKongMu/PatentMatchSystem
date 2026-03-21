package com.patent.model.graph.rel;

import com.patent.model.graph.EntityNode;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.RelationshipId;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * MENTIONS 关系属性类
 * Patent -> Entity，带置信度属性
 */
@Data
@RelationshipProperties
public class MentionedEntityRel {

    /**
     * 关系内部 ID（Spring Data Neo4j 要求）
     */
    @RelationshipId
    private Long id;

    /**
     * 目标实体节点
     */
    @TargetNode
    private EntityNode entity;

    /**
     * 实体抽取置信度（0.0 ~ 1.0）
     */
    private Double confidence;
}
