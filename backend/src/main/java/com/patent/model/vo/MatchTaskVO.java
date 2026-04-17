package com.patent.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 异步匹配任务状态VO
 */
@Data
public class MatchTaskVO {

    /**
     * 会话ID（任务唯一标识）
     */
    private String sessionId;

    /**
     * 任务状态：RUNNING/COMPLETED/FAILED
     */
    private String status;

    /**
     * 进度（0-100）
     */
    private Integer progress;

    /**
     * 已处理数量
     */
    private Integer processedCount;

    /**
     * 总候选数量
     */
    private Integer totalCount;

    /**
     * 匹配结果（COMPLETED时有值）
     */
    private MatchResultVO result;

    /**
     * 错误信息（FAILED时有值）
     */
    private String errorMsg;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;
}
