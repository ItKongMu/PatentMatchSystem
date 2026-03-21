package com.patent.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 图谱查询返回结构
 * 兼容 ECharts graph 组件直接消费
 */
@Data
public class GraphVO {

    /**
     * 节点列表
     */
    private List<NodeVO> nodes = new ArrayList<>();

    /**
     * 关系列表
     */
    private List<LinkVO> links = new ArrayList<>();

    /**
     * 图谱节点
     */
    @Data
    public static class NodeVO {
        /** 节点唯一 ID，格式：Label:primaryKey，如 Patent:CN123456A */
        private String id;
        /** 节点标签（类型），如 Patent/IPC/Entity/Applicant/Concept */
        private String label;
        /** 节点显示名称 */
        private String name;
        /** 节点附加属性（可选，用于前端 tooltip） */
        private String extra;

        public static NodeVO of(String label, String key, String name) {
            NodeVO vo = new NodeVO();
            vo.setId(label + ":" + key);
            vo.setLabel(label);
            vo.setName(name);
            return vo;
        }

        public static NodeVO of(String label, String key, String name, String extra) {
            NodeVO vo = of(label, key, name);
            vo.setExtra(extra);
            return vo;
        }
    }

    /**
     * 图谱关系
     */
    @Data
    public static class LinkVO {
        /** 起点节点 ID */
        private String source;
        /** 终点节点 ID */
        private String target;
        /** 关系类型，如 MENTIONS/HAS_IPC/FILED_BY */
        private String type;
        /** 关系属性（可选，如置信度） */
        private String value;

        public static LinkVO of(String source, String target, String type) {
            LinkVO vo = new LinkVO();
            vo.setSource(source);
            vo.setTarget(target);
            vo.setType(type);
            return vo;
        }

        public static LinkVO of(String source, String target, String type, String value) {
            LinkVO vo = of(source, target, type);
            vo.setValue(value);
            return vo;
        }
    }
}
