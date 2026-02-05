package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 高级检索DTO
 */
@Data
@Schema(description = "高级检索请求")
public class SearchDTO {

    @Schema(description = "关键词（快速检索时使用）")
    private String keyword;

    @Schema(description = "专利标题")
    private String title;

    @Schema(description = "摘要关键词")
    private String abstractKeyword;

    @Schema(description = "领域代码（如G06F），支持前缀匹配")
    private String domainCode;

    @Schema(description = "领域部（如G、H），支持多选")
    private List<String> domainSections;

    @Schema(description = "申请人（精确匹配）")
    private String applicant;

    @Schema(description = "申请人关键词（模糊匹配）")
    private String applicantKeyword;

    @Schema(description = "专利号")
    private String publicationNo;

    @Schema(description = "实体关键词")
    private String entityKeyword;

    @Schema(description = "实体类型（PRODUCT/METHOD/MATERIAL/COMPONENT/EFFECT/APPLICATION）")
    private String entityType;

    @Schema(description = "实体类型列表（多选）")
    private List<String> entityTypes;

    @Schema(description = "公开日期起始")
    private LocalDate publicationDateFrom;

    @Schema(description = "公开日期截止")
    private LocalDate publicationDateTo;

    @Schema(description = "是否启用高亮", defaultValue = "true")
    private Boolean enableHighlight = true;

    @Schema(description = "排序字段（_score/publication_date/created_at）", defaultValue = "_score")
    private String sortField = "_score";

    @Schema(description = "排序方向（asc/desc）", defaultValue = "desc")
    private String sortOrder = "desc";

    @Schema(description = "页码", defaultValue = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", defaultValue = "10")
    private Integer pageSize = 10;

    @Schema(description = "search_after参数（用于深度分页）")
    private List<Object> searchAfter;

    @Schema(description = "是否使用深度分页模式", defaultValue = "false")
    private Boolean useSearchAfter = false;
}
