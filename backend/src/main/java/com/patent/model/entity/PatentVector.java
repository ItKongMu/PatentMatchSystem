package com.patent.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专利向量映射表（与Qdrant的关联）
 */
@Data
@TableName("patent_vector")
public class PatentVector {

    /**
     * 专利ID
     */
    @TableId(type = IdType.INPUT)
    private Long patentId;

    /**
     * Qdrant向量ID（UUID）
     */
    private String vectorId;

    /**
     * 向量模型：text-embedding-v3/nomic-embed-text
     */
    private String embeddingModel;

    /**
     * 向量维度
     */
    private Integer vectorDim;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
