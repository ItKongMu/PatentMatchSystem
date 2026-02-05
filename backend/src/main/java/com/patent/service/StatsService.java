package com.patent.service;

import com.patent.model.vo.StatsVO;

import java.util.List;

/**
 * 统计分析服务接口
 */
public interface StatsService {

    /**
     * 获取系统概览统计
     */
    StatsVO.OverviewVO getOverviewStats();

    /**
     * 获取实体词云数据
     *
     * @param topN 返回前N个高频实体
     * @return 词云数据
     */
    StatsVO.WordCloudVO getEntityWordCloud(int topN);

    /**
     * 获取实体类型分布
     *
     * @return 各实体类型的数量统计
     */
    List<StatsVO.EntityTypeStatVO> getEntityTypeStats();

    /**
     * 获取领域分布统计（IPC部级）
     *
     * @return 各领域的专利数量统计
     */
    List<StatsVO.DomainStatVO> getDomainSectionStats();

    /**
     * 获取专利申请趋势（按年份）
     *
     * @param years 统计的年数
     * @return 各年份的专利数量
     */
    List<StatsVO.TrendStatVO> getPatentTrend(int years);

    /**
     * 获取申请人排行
     *
     * @param topN 返回前N名
     * @return 申请人及其专利数量
     */
    List<StatsVO.EntityStatVO> getTopApplicants(int topN);

    /**
     * 清除所有统计缓存
     * 当专利数据发生变化时调用
     */
    void evictAllStatsCache();
}
