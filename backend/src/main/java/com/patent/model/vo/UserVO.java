package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户信息VO（含统计数据）
 */
@Data
@Schema(description = "用户信息")
public class UserVO {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "角色：admin/user")
    private String role;

    @Schema(description = "状态：1-启用，0-禁用")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    // ========== 管理员列表扩展字段 ==========

    @Schema(description = "是否在线（当前已登录）")
    private Boolean online;

    @Schema(description = "Token剩余有效时间（秒），-1表示未登录或已过期")
    private Long tokenTimeout;

    @Schema(description = "上传专利数")
    private Long patentCount;

    @Schema(description = "匹配历史次数")
    private Long matchCount;

    @Schema(description = "收藏专利数")
    private Long favoriteCount;
}
