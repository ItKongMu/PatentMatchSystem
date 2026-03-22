package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户管理统计面板VO
 */
@Data
@Schema(description = "用户管理统计面板")
public class UserStatsVO {

    @Schema(description = "总用户数")
    private Long totalUsers;

    @Schema(description = "当前在线人数（SaToken会话数）")
    private Integer onlineUsers;

    @Schema(description = "今日新增用户数")
    private Long todayNewUsers;

    @Schema(description = "禁用账号数")
    private Long disabledUsers;
}
