package com.patent.model.dto;

import lombok.Data;

/**
 * 通用图谱查询 DTO
 */
@Data
public class GraphQueryDTO {

    /**
     * 起始节点类型：Patent/IPC/Entity/Applicant/Concept
     */
    private String nodeType;

    /**
     * 起始节点主键值（publicationNo / ipcCode / name 等）
     */
    private String nodeKey;

    /**
     * 关键词（前端通用查询传入，等同于 nodeKey，优先级低于 nodeKey）
     */
    private String keyword;

    /**
     * 查询深度（1~3，默认 2）
     */
    private Integer depth = 2;

    /**
     * 关系类型过滤（逗号分隔，为空则不过滤）
     * 如：MENTIONS,HAS_IPC
     */
    private String relationTypes;

    /**
     * 返回节点数量上限（默认 50）
     */
    private Integer limit = 50;

    /**
     * 获取有效的节点键值：nodeKey 优先，其次 keyword
     */
    public String getEffectiveKey() {
        if (nodeKey != null && !nodeKey.isBlank()) return nodeKey;
        if (keyword != null && !keyword.isBlank()) return keyword;
        return null;
    }
}
