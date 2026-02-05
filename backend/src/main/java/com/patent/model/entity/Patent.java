package com.patent.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 专利基础信息表
 */
@Data
@TableName("patent")
public class Patent {

    /**
     * 专利ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 公开号/专利号
     */
    private String publicationNo;

    /**
     * 专利名称
     */
    private String title;

    /**
     * 申请人
     */
    private String applicant;

    /**
     * 公开日期
     */
    private LocalDate publicationDate;

    /**
     * 专利摘要
     */
    @TableField("`abstract`")
    private String patentAbstract;

    /**
     * MinIO文件路径
     */
    private String filePath;

    /**
     * 来源类型：FILE-PDF上传/TEXT-文本录入
     */
    private String sourceType;

    /**
     * 解析状态：PENDING/PARSING/EXTRACTING/VECTORIZING/SUCCESS/FAILED
     */
    private String parseStatus;

    /**
     * 解析错误信息
     */
    private String parseError;

    /**
     * 创建用户ID
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
