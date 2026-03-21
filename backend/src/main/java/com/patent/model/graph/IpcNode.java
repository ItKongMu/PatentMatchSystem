package com.patent.model.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

/**
 * Neo4j IPC 分类节点
 * 对应知识图谱中的 IPC 节点类型，支持层次结构
 */
@Data
@Node("IPC")
public class IpcNode {

    /**
     * IPC 编码作为业务主键（如 G06F16/30）
     */
    @Id
    private String ipcCode;

    /**
     * IPC 名称/描述
     */
    private String name;

    /**
     * 层级：1-部/2-大类/3-小类/4-主组/5-分组
     */
    private Integer level;

    /**
     * 父节点编码（冗余存储，便于查询）
     */
    private String parentCode;

    /**
     * 父节点关系（PARENT_OF 方向：子 -> 父，即当前节点的父节点）
     * 注意：方向为 OUTGOING 表示 (this)-[:PARENT_OF]->(parent)
     */
    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.OUTGOING)
    private IpcNode parent;

    /**
     * 子节点列表（PARENT_OF 方向：父 -> 子，即当前节点的子节点）
     */
    @Relationship(type = "PARENT_OF", direction = Relationship.Direction.INCOMING)
    private List<IpcNode> children = new ArrayList<>();
}
