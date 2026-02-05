package com.patent.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专利技术领域表（IPC/CPC分类，支持层次化）
 */
@Data
@TableName("patent_domain")
public class PatentDomain {

    /**
     * 领域ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 专利ID
     */
    private Long patentId;

    /**
     * 领域代码（如G06F16/30）
     */
    private String domainCode;

    /**
     * 领域层级：1-部/2-大类/3-小类/4-主组/5-分组
     */
    private Integer domainLevel;

    /**
     * 领域描述
     */
    private String domainDesc;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
