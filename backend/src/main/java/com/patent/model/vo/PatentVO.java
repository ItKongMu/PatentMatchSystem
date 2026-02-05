package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 专利详情VO
 */
@Data
@Schema(description = "专利详情")
public class PatentVO {

    @Schema(description = "专利ID")
    private Long id;

    @Schema(description = "公开号/专利号")
    private String publicationNo;

    @Schema(description = "专利名称")
    private String title;

    @Schema(description = "申请人")
    private String applicant;

    @Schema(description = "公开日期")
    private LocalDate publicationDate;

    @Schema(description = "专利摘要")
    private String patentAbstract;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "解析状态")
    private String parseStatus;

    @Schema(description = "解析错误信息")
    private String parseError;

    @Schema(description = "实体列表")
    private List<EntityVO> entities;

    @Schema(description = "领域列表")
    private List<DomainVO> domains;

    @Schema(description = "向量信息")
    private VectorVO vector;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    /**
     * 实体VO
     */
    @Data
    @Schema(description = "技术实体")
    public static class EntityVO {
        @Schema(description = "实体ID")
        private Long id;

        @Schema(description = "实体名称")
        private String entityName;

        @Schema(description = "实体类型")
        private String entityType;

        @Schema(description = "重要性")
        private String importance;
    }

    /**
     * 领域VO
     */
    @Data
    @Schema(description = "技术领域")
    public static class DomainVO {
        @Schema(description = "领域代码")
        private String domainCode;

        @Schema(description = "领域层级")
        private Integer domainLevel;

        @Schema(description = "领域描述")
        private String domainDesc;
    }

    /**
     * 向量VO
     */
    @Data
    @Schema(description = "向量信息")
    public static class VectorVO {
        @Schema(description = "向量ID")
        private String vectorId;

        @Schema(description = "嵌入模型")
        private String embeddingModel;

        @Schema(description = "向量维度")
        private Integer vectorDim;
    }
}
