package com.patent.repository;

import com.patent.model.graph.IpcNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * IPC 分类图谱节点 Repository
 */
@Repository
public interface IpcNodeRepository extends Neo4jRepository<IpcNode, String> {

    /**
     * 按 IPC 编码查询节点（仅查节点本身，用于写入时判断是否已存在）
     * 使用 @Query 显式属性匹配，避免 SDN 7.x 将 @Id 字段转为 elementId() 查询
     */
    @Query("MATCH (i:IPC {ipcCode: $ipcCode}) RETURN i")
    Optional<IpcNode> findByIpcCode(@Param("ipcCode") String ipcCode);

    /**
     * 查询某 IPC 节点的所有子节点（一级）
     */
    @Query("MATCH (child:IPC)-[:PARENT_OF]->(parent:IPC {ipcCode: $ipcCode}) RETURN child")
    List<IpcNode> findChildren(@Param("ipcCode") String ipcCode);

    /**
     * 查询某 IPC 节点下关联的专利数量
     */
    @Query("MATCH (p:Patent)-[:HAS_IPC]->(i:IPC {ipcCode: $ipcCode}) RETURN count(p)")
    int countRelatedPatents(@Param("ipcCode") String ipcCode);
}
