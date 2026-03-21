package com.patent.model.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * Neo4j 申请人节点
 * 对应知识图谱中的 Applicant 节点类型
 */
@Data
@Node("Applicant")
public class ApplicantNode {

    /**
     * 申请人名称作为业务主键
     */
    @Id
    private String applicantName;
}
