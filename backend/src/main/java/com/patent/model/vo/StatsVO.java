package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 统计数据VO
 */
@Data
@Schema(description = "统计数据")
public class StatsVO {

    /**
     * 概览统计
     */
    @Data
    @Schema(description = "概览统计")
    public static class OverviewVO {
        @Schema(description = "专利总数")
        private Long totalPatents;

        @Schema(description = "实体总数")
        private Long totalEntities;

        @Schema(description = "领域总数")
        private Long totalDomains;

        @Schema(description = "用户总数")
        private Long totalUsers;

        @Schema(description = "匹配记录总数")
        private Long totalMatches;
    }

    /**
     * 实体统计项
     */
    @Data
    @Schema(description = "实体统计项")
    public static class EntityStatVO {
        @Schema(description = "实体名称")
        private String name;

        @Schema(description = "实体类型")
        private String type;

        @Schema(description = "出现次数")
        private Long count;
    }

    /**
     * 领域统计项
     */
    @Data
    @Schema(description = "领域统计项")
    public static class DomainStatVO {
        @Schema(description = "领域代码")
        private String code;

        @Schema(description = "领域描述")
        private String description;

        @Schema(description = "专利数量")
        private Long count;
    }

    /**
     * 专利趋势统计项
     */
    @Data
    @Schema(description = "专利趋势统计项")
    public static class TrendStatVO {
        @Schema(description = "年份")
        private String year;

        @Schema(description = "专利数量")
        private Long count;
    }

    /**
     * 实体类型统计项
     */
    @Data
    @Schema(description = "实体类型统计项")
    public static class EntityTypeStatVO {
        @Schema(description = "实体类型")
        private String type;

        @Schema(description = "类型描述")
        private String description;

        @Schema(description = "数量")
        private Long count;
    }

    /**
     * 实体词云数据
     */
    @Data
    @Schema(description = "实体词云数据")
    public static class WordCloudVO {
        @Schema(description = "词云数据列表")
        private List<EntityStatVO> data;

        @Schema(description = "按类型分组的词云数据")
        private List<EntityTypeGroupVO> byType;
    }

    /**
     * 按类型分组的实体数据
     */
    @Data
    @Schema(description = "按类型分组的实体数据")
    public static class EntityTypeGroupVO {
        @Schema(description = "实体类型")
        private String type;

        @Schema(description = "类型描述")
        private String description;

        @Schema(description = "该类型下的实体列表")
        private List<EntityStatVO> entities;
    }
}
