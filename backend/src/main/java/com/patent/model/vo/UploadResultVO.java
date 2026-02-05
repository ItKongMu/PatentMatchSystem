package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 上传结果VO
 */
@Data
@Schema(description = "上传结果")
public class UploadResultVO {

    @Schema(description = "专利ID")
    private Long patentId;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "解析状态")
    private String parseStatus;

    @Schema(description = "消息")
    private String message;
}
