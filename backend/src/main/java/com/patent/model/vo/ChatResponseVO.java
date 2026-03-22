package com.patent.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 对话响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "对话响应")
public class ChatResponseVO {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "AI回复内容")
    private String reply;

    @Schema(description = "检索到的专利列表")
    private List<PatentSummaryVO> patents;

    @Schema(description = "后续建议问题")
    private List<String> suggestions;

    @Schema(description = "执行的工具调用信息")
    private List<ToolCallInfo> toolCalls;

    @Schema(description = "图谱数据（getPatentGraph 工具调用时返回）")
    private GraphData graphData;

    @Schema(description = "响应时间")
    private LocalDateTime timestamp;

    /**
     * 专利摘要信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "专利摘要")
    public static class PatentSummaryVO {
        @Schema(description = "专利ID")
        private Long id;

        @Schema(description = "公开号")
        private String publicationNo;

        @Schema(description = "专利标题")
        private String title;

        @Schema(description = "申请人")
        private String applicant;

        @Schema(description = "摘要（截断）")
        private String abstractText;

        @Schema(description = "相关度评分")
        private Double relevanceScore;

        @Schema(description = "领域代码")
        private List<String> domainCodes;

        @Schema(description = "主要实体")
        private List<String> entities;
    }

    /**
     * 工具调用信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "工具调用信息")
    public static class ToolCallInfo {
        @Schema(description = "工具名称")
        private String toolName;

        @Schema(description = "工具参数")
        private String parameters;

        @Schema(description = "执行结果摘要")
        private String resultSummary;
    }

    /**
     * 图谱数据（供前端 ECharts 直接渲染）
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "图谱数据")
    public static class GraphData {
        @Schema(description = "查询类型：patent/entity/ipc")
        private String queryType;

        @Schema(description = "查询值")
        private String queryValue;

        @Schema(description = "节点列表")
        private java.util.List<GraphVO.NodeVO> nodes;

        @Schema(description = "关系列表")
        private java.util.List<GraphVO.LinkVO> links;
    }
}
