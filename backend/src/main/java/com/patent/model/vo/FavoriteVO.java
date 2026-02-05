package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 收藏响应VO
 */
@Data
@Schema(description = "专利收藏响应")
public class FavoriteVO {

    @Schema(description = "收藏ID")
    private Long id;

    @Schema(description = "专利ID")
    private Long patentId;

    @Schema(description = "专利标题")
    private String patentTitle;

    @Schema(description = "公开号")
    private String publicationNo;

    @Schema(description = "收藏备注")
    private String remark;

    @Schema(description = "收藏夹分组")
    private String groupName;

    @Schema(description = "收藏时间")
    private LocalDateTime createdAt;
}
