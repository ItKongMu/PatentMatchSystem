package com.patent.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * CSV导入专利DTO
 */
@Data
@Schema(description = "CSV导入专利数据")
public class PatentCsvDTO {

    @Schema(description = "公开号/专利号", example = "CN123456789A")
    private String publicationNo;

    @Schema(description = "专利标题", example = "一种智能检测方法")
    private String title;

    @Schema(description = "申请人", example = "华为技术有限公司")
    private String applicant;

    @Schema(description = "公开日期（格式：yyyy-MM-dd）", example = "2024-01-15")
    private String publicationDate;

    @Schema(description = "IPC分类号（多个用逗号分隔）", example = "G06F 16/30, G06N 3/08")
    private String ipcClassification;

    @Schema(description = "专利摘要")
    private String patentAbstract;

    @Schema(description = "专利正文（可选，如有则与摘要合并用于分析）")
    private String fullText;

    @Schema(description = "行号（CSV中的行）")
    private Integer rowNum;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "是否有效")
    private Boolean valid = true;

    /**
     * 校验数据有效性
     * 验证规则：
     * 1. 标题不能为空且长度应在5-200个字符之间
     * 2. 摘要不能为空且长度应在10-5000个字符之间
     * 3. 公开号格式校验（可选字段）
     */
    public void validate() {
        // 标题校验
        if (title == null || title.trim().isEmpty()) {
            this.valid = false;
            this.errorMessage = "专利标题不能为空";
            return;
        }
        String trimmedTitle = title.trim();
        if (trimmedTitle.length() < 5 || trimmedTitle.length() > 200) {
            this.valid = false;
            this.errorMessage = "专利标题长度应在5-200个字符之间";
            return;
        }

        // 摘要校验
        if (patentAbstract == null || patentAbstract.trim().isEmpty()) {
            this.valid = false;
            this.errorMessage = "专利摘要不能为空";
            return;
        }
        String trimmedAbstract = patentAbstract.trim();
        if (trimmedAbstract.length() < 10) {
            this.valid = false;
            this.errorMessage = "专利摘要长度不能少于10个字符";
            return;
        }
        if (trimmedAbstract.length() > 5000) {
            this.valid = false;
            this.errorMessage = "专利摘要长度不能超过5000个字符";
            return;
        }

        // 公开号格式校验（可选字段，如果有值则验证格式）
        if (publicationNo != null && !publicationNo.trim().isEmpty()) {
            String trimmedNo = publicationNo.trim();
            if (!trimmedNo.matches("^[A-Z]{2}\\d+[A-Z]?\\d*$")) {
                this.valid = false;
                this.errorMessage = "公开号格式不正确，示例：CN123456789A";
                return;
            }
        }

        // 正文长度校验（可选字段）。修复：先 trim 后再校验长度，避免纯空白内容占用长度配额。
        if (fullText != null && !fullText.trim().isEmpty() && fullText.trim().length() > 100000) {
            this.valid = false;
            this.errorMessage = "专利正文长度不能超过10万个字符";
            return;
        }

        // 申请人长度校验（trim 后校验）
        if (applicant != null && applicant.trim().length() > 200) {
            this.valid = false;
            this.errorMessage = "申请人长度不能超过200个字符";
            return;
        }

        // IPC分类号长度校验（trim 后校验）
        if (ipcClassification != null && ipcClassification.trim().length() > 500) {
            this.valid = false;
            this.errorMessage = "IPC分类号长度不能超过500个字符";
        }
    }
}
