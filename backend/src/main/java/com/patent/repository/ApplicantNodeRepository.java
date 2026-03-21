package com.patent.repository;

import com.patent.model.graph.ApplicantNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 申请人图谱节点 Repository
 */
@Repository
public interface ApplicantNodeRepository extends Neo4jRepository<ApplicantNode, String> {

    /**
     * 按申请人名称查询节点
     * 使用 @Query 显式属性匹配，避免 SDN 7.x 将 @Id 字段转为 elementId() 查询
     */
    @Query("MATCH (a:Applicant {applicantName: $applicantName}) RETURN a")
    Optional<ApplicantNode> findByApplicantName(@Param("applicantName") String applicantName);

    /**
     * 查询某申请人的所有专利公开号
     */
    @Query("MATCH (p:Patent)-[:FILED_BY]->(a:Applicant {applicantName: $applicantName}) RETURN p.publicationNo")
    List<String> findPatentNosByApplicant(@Param("applicantName") String applicantName);
}
