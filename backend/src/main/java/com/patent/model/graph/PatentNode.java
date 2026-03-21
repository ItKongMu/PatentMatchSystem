package com.patent.model.graph;

import com.patent.model.graph.rel.MentionedEntityRel;
import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j 专利节点
 * 对应知识图谱中的 Patent 节点类型
 */
@Data
@Node("Patent")
public class PatentNode {

    /**
     * 公开号作为业务主键（唯一标识）
     */
    @Id
    private String publicationNo;

    /**
     * 专利标题
     */
    private String title;

    /**
     * 专利摘要
     */
    @Property("abstractText")
    private String abstractText;

    /**
     * 申请人
     */
    private String applicant;

    /**
     * 公开日期（字符串格式 yyyy-MM-dd）
     */
    private String publicationDate;

    /**
     * 领域代码列表（冗余存储，便于快速过滤）
     */
    private List<String> domainCodes = new ArrayList<>();

    /**
     * 来源类型：FILE/TEXT/CSV
     */
    private String sourceType;

    /**
     * 创建时间
     */
    private String createdAt;

    // ==================== 关系 ====================

    /**
     * 专利包含的技术实体（MENTIONS 关系，带置信度属性）
     */
    @Relationship(type = "MENTIONS", direction = Relationship.Direction.OUTGOING)
    private List<MentionedEntityRel> mentionedEntities = new ArrayList<>();

    /**
     * 专利所属 IPC 分类（HAS_IPC 关系）
     */
    @Relationship(type = "HAS_IPC", direction = Relationship.Direction.OUTGOING)
    private List<IpcNode> ipcNodes = new ArrayList<>();

    /**
     * 申请人节点（FILED_BY 关系）
     */
    @Relationship(type = "FILED_BY", direction = Relationship.Direction.OUTGOING)
    private ApplicantNode applicantNode;

    /**
     * 应用场景节点（APPLIED_IN 关系）
     */
    @Relationship(type = "APPLIED_IN", direction = Relationship.Direction.OUTGOING)
    private List<ConceptNode> applications = new ArrayList<>();

    /**
     * 引用的专利（CITES 关系）
     */
    @Relationship(type = "CITES", direction = Relationship.Direction.OUTGOING)
    private List<PatentNode> citedPatents = new ArrayList<>();
}
