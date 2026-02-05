package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 专利列表项VO
 */
@Data
@Schema(description = "专利列表项")
public class PatentListVO {

    @Schema(description = "专利ID")
    private Long id;

    @Schema(description = "公开号/专利号")
    private String publicationNo;

    @Schema(description = "专利名称")
    private String title;

    @Schema(description = "申请人")
    private String applicant;

    @Schema(description = "专利摘要")
    private String patentAbstract;

    @Schema(description = "公开日期")
    private LocalDate publicationDate;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "解析状态")
    private String parseStatus;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "收藏备注")
    private String favoriteRemark;

    @Schema(description = "收藏分组")
    private String favoriteGroup;

    @Schema(description = "收藏时间")
    private LocalDateTime favoriteTime;

    @Schema(description = "技术实体列表")
    private List<EntityVO> entities;

    @Schema(description = "技术领域列表")
    private List<DomainVO> domains;

    /**
     * 实体VO
     */
    @Data
    public static class EntityVO {
        private Long id;
        private String entityName;
        private String entityType;
    }

    /**
     * 领域VO
     */
    @Data
    public static class DomainVO {
        private Long id;
        private String domainCode;
        private String domainDesc;
        private Integer domainLevel;
    }
}
