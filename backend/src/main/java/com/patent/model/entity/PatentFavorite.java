package com.patent.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 专利收藏表
 */
@Data
@TableName("patent_favorite")
public class PatentFavorite {

    /**
     * 收藏ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 专利ID
     */
    private Long patentId;

    /**
     * 收藏备注
     */
    private String remark;

    /**
     * 收藏夹分组（可选）
     */
    private String groupName;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
