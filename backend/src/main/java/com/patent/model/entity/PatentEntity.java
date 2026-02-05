package com.patent.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专利实体表（LLM提取的技术实体）
 */
@Data
@TableName("patent_entity")
public class PatentEntity {

    /**
     * 实体ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 专利ID
     */
    private Long patentId;

    /**
     * 实体名称
     */
    private String entityName;

    /**
     * 实体类型：PRODUCT/METHOD/MATERIAL/COMPONENT/EFFECT/APPLICATION
     */
    private String entityType;

    /**
     * 重要性：high/medium/low
     */
    private String importance;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
