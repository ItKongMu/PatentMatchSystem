package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 检索结果VO（支持高亮）
 */
@Data
@Schema(description = "检索结果项")
public class SearchResultVO {

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

    @Schema(description = "技术实体列表")
    private List<PatentListVO.EntityVO> entities;

    @Schema(description = "技术领域列表")
    private List<PatentListVO.DomainVO> domains;

    @Schema(description = "领域代码列表")
    private List<String> domainCodes;

    @Schema(description = "相关性评分")
    private Float score;

    @Schema(description = "高亮结果（字段名 -> 高亮片段列表）")
    private Map<String, List<String>> highlights;

    @Schema(description = "排序值（用于search_after分页）")
    private List<Object> sortValues;
}
