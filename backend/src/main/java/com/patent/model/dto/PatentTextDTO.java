package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 专利文本录入DTO
 */
@Data
@Schema(description = "专利文本录入请求")
public class PatentTextDTO {

    @Schema(description = "公开号/专利号")
    private String publicationNo;

    @NotBlank(message = "专利名称不能为空")
    @Schema(description = "专利名称", required = true)
    private String title;

    @Schema(description = "申请人")
    private String applicant;

    @Schema(description = "公开日期")
    private LocalDate publicationDate;

    @NotBlank(message = "专利摘要不能为空")
    @Schema(description = "专利摘要", required = true)
    private String patentAbstract;
}
