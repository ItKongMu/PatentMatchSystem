package com.patent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.patent.mapper.*;
import com.patent.model.entity.Patent;
import com.patent.model.entity.PatentDomain;
import com.patent.model.entity.PatentEntity;
import com.patent.model.vo.StatsVO;
import com.patent.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 统计分析服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final PatentMapper patentMapper;
    private final PatentEntityMapper patentEntityMapper;
    private final PatentDomainMapper patentDomainMapper;
    private final SysUserMapper sysUserMapper;
    private final MatchRecordMapper matchRecordMapper;

    /**
     * 实体类型中文描述映射
     */
    private static final Map<String, String> ENTITY_TYPE_DESC = Map.of(
            "PRODUCT", "产品/设备",
            "METHOD", "方法/工艺",
            "MATERIAL", "材料/物质",
            "COMPONENT", "组件/部件",
            "EFFECT", "效果/性能",
            "APPLICATION", "应用场景"
    );

    /**
     * IPC领域部描述映射
     */
    private static final Map<String, String> IPC_SECTION_DESC = Map.of(
            "A", "人类生活必需",
            "B", "作业/运输",
            "C", "化学/冶金",
            "D", "纺织/造纸",
            "E", "固定建筑物",
            "F", "机械工程/照明/加热",
            "G", "物理",
            "H", "电学"
    );

    @Override
    @Cacheable(value = "stats", key = "'overview'")
    public StatsVO.OverviewVO getOverviewStats() {
        StatsVO.OverviewVO vo = new StatsVO.OverviewVO();

        // 统计已成功解析的专利数
        LambdaQueryWrapper<Patent> patentWrapper = new LambdaQueryWrapper<>();
        patentWrapper.eq(Patent::getParseStatus, "SUCCESS");
        vo.setTotalPatents(patentMapper.selectCount(patentWrapper));

        // 统计实体总数
        vo.setTotalEntities(patentEntityMapper.selectCount(null));

        // 统计领域数（去重）
        LambdaQueryWrapper<PatentDomain> domainWrapper = new LambdaQueryWrapper<>();
        domainWrapper.select(PatentDomain::getDomainCode);
        domainWrapper.groupBy(PatentDomain::getDomainCode);
        vo.setTotalDomains((long) patentDomainMapper.selectList(domainWrapper).size());

        // 统计用户数
        vo.setTotalUsers(sysUserMapper.selectCount(null));

        // 统计匹配记录数
        vo.setTotalMatches(matchRecordMapper.selectCount(null));

        return vo;
    }

    @Override
    @Cacheable(value = "wordcloud", key = "'entity:' + #topN")
    public StatsVO.WordCloudVO getEntityWordCloud(int topN) {
        StatsVO.WordCloudVO vo = new StatsVO.WordCloudVO();

        // 查询所有实体
        List<PatentEntity> allEntities = patentEntityMapper.selectList(null);

        if (allEntities.isEmpty()) {
            vo.setData(Collections.emptyList());
            vo.setByType(Collections.emptyList());
            return vo;
        }

        // 过滤掉名称为空的实体，并按实体名称分组统计频次
        Map<String, List<PatentEntity>> nameGrouped = allEntities.stream()
                .filter(e -> e.getEntityName() != null && !e.getEntityName().isBlank())
                .collect(Collectors.groupingBy(PatentEntity::getEntityName));

        // 转换为统计项并排序
        List<StatsVO.EntityStatVO> entityStats = nameGrouped.entrySet().stream()
                .map(entry -> {
                    StatsVO.EntityStatVO stat = new StatsVO.EntityStatVO();
                    stat.setName(entry.getKey());
                    stat.setCount((long) entry.getValue().size());
                    // 取第一个实体的类型作为代表类型
                    String type = entry.getValue().get(0).getEntityType();
                    stat.setType(type != null ? type : "UNKNOWN");
                    return stat;
                })
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(topN)
                .collect(Collectors.toList());

        vo.setData(entityStats);

        // 按类型分组（过滤null类型）
        Map<String, List<StatsVO.EntityStatVO>> typeGrouped = entityStats.stream()
                .filter(e -> e.getType() != null)
                .collect(Collectors.groupingBy(StatsVO.EntityStatVO::getType));

        List<StatsVO.EntityTypeGroupVO> byType = typeGrouped.entrySet().stream()
                .map(entry -> {
                    StatsVO.EntityTypeGroupVO group = new StatsVO.EntityTypeGroupVO();
                    group.setType(entry.getKey());
                    group.setDescription(ENTITY_TYPE_DESC.getOrDefault(entry.getKey(), entry.getKey()));
                    group.setEntities(entry.getValue());
                    return group;
                })
                .sorted((a, b) -> Integer.compare(
                        b.getEntities().stream().mapToInt(e -> e.getCount().intValue()).sum(),
                        a.getEntities().stream().mapToInt(e -> e.getCount().intValue()).sum()
                ))
                .collect(Collectors.toList());

        vo.setByType(byType);

        return vo;
    }

    @Override
    @Cacheable(value = "stats", key = "'entityTypes'")
    public List<StatsVO.EntityTypeStatVO> getEntityTypeStats() {
        // 查询所有实体
        List<PatentEntity> allEntities = patentEntityMapper.selectList(null);

        if (allEntities.isEmpty()) {
            return Collections.emptyList();
        }

        // 按类型分组统计
        Map<String, Long> typeCount = allEntities.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getEntityType() != null ? e.getEntityType() : "UNKNOWN",
                        Collectors.counting()
                ));

        return typeCount.entrySet().stream()
                .map(entry -> {
                    StatsVO.EntityTypeStatVO stat = new StatsVO.EntityTypeStatVO();
                    stat.setType(entry.getKey());
                    stat.setDescription(ENTITY_TYPE_DESC.getOrDefault(entry.getKey(), entry.getKey()));
                    stat.setCount(entry.getValue());
                    return stat;
                })
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "stats", key = "'domainSections'")
    public List<StatsVO.DomainStatVO> getDomainSectionStats() {
        // 查询所有领域部级数据（level=1）
        LambdaQueryWrapper<PatentDomain> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PatentDomain::getDomainLevel, 1);
        List<PatentDomain> sectionDomains = patentDomainMapper.selectList(wrapper);

        if (sectionDomains.isEmpty()) {
            return Collections.emptyList();
        }

        // 按领域代码分组统计
        Map<String, Long> sectionCount = sectionDomains.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getDomainCode() != null ? d.getDomainCode() : "未知",
                        Collectors.counting()
                ));

        return sectionCount.entrySet().stream()
                .map(entry -> {
                    StatsVO.DomainStatVO stat = new StatsVO.DomainStatVO();
                    stat.setCode(entry.getKey());
                    stat.setDescription(IPC_SECTION_DESC.getOrDefault(entry.getKey(), entry.getKey()));
                    stat.setCount(entry.getValue());
                    return stat;
                })
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "trend", key = "'patent:' + #years")
    public List<StatsVO.TrendStatVO> getPatentTrend(int years) {
        // 查询所有已解析的专利
        LambdaQueryWrapper<Patent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Patent::getParseStatus, "SUCCESS");
        wrapper.isNotNull(Patent::getPublicationDate);
        List<Patent> patents = patentMapper.selectList(wrapper);

        if (patents.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算年份范围
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - years + 1;

        // 按年份分组统计
        Map<Integer, Long> yearCount = patents.stream()
                .filter(p -> p.getPublicationDate() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getPublicationDate().getYear(),
                        Collectors.counting()
                ));

        // 生成完整的年份序列（包括数量为0的年份）
        List<StatsVO.TrendStatVO> result = new ArrayList<>();
        for (int year = startYear; year <= currentYear; year++) {
            StatsVO.TrendStatVO stat = new StatsVO.TrendStatVO();
            stat.setYear(String.valueOf(year));
            stat.setCount(yearCount.getOrDefault(year, 0L));
            result.add(stat);
        }

        return result;
    }

    @Override
    @Cacheable(value = "stats", key = "'applicants:' + #topN")
    public List<StatsVO.EntityStatVO> getTopApplicants(int topN) {
        // 查询所有已解析的专利
        LambdaQueryWrapper<Patent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Patent::getParseStatus, "SUCCESS");
        wrapper.isNotNull(Patent::getApplicant);
        List<Patent> patents = patentMapper.selectList(wrapper);

        if (patents.isEmpty()) {
            return Collections.emptyList();
        }

        // 按申请人分组统计
        Map<String, Long> applicantCount = patents.stream()
                .filter(p -> p.getApplicant() != null && !p.getApplicant().isBlank())
                .collect(Collectors.groupingBy(
                        Patent::getApplicant,
                        Collectors.counting()
                ));

        return applicantCount.entrySet().stream()
                .map(entry -> {
                    StatsVO.EntityStatVO stat = new StatsVO.EntityStatVO();
                    stat.setName(entry.getKey());
                    stat.setCount(entry.getValue());
                    return stat;
                })
                .sorted((a, b) -> Long.compare(b.getCount(), a.getCount()))
                .limit(topN)
                .collect(Collectors.toList());
    }

    @Override
    @CacheEvict(value = {"stats", "wordcloud", "trend"}, allEntries = true)
    public void evictAllStatsCache() {
        log.info("已清除所有统计缓存");
    }
}
