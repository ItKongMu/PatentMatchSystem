package com.patent.service.impl;

import com.patent.config.PatentConfig;
import com.patent.mapper.*;
import com.patent.model.entity.*;
import com.patent.service.FileService;
import com.patent.service.LlmService;
import com.patent.service.SearchService;
import com.patent.service.StatsService;
import com.patent.service.VectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.InputStreamResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 专利处理服务（异步处理）
 * 独立出来解决@Async和@Transactional同时使用的问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatentProcessorService {

    private final PatentMapper patentMapper;
    private final PatentEntityMapper patentEntityMapper;
    private final PatentDomainMapper patentDomainMapper;
    private final PatentVectorMapper patentVectorMapper;
    private final FileService fileService;
    private final LlmService llmService;
    private final VectorService vectorService;
    private final SearchService searchService;
    private final StatsService statsService;
    private final PatentConfig patentConfig;

    /**
     * 异步处理专利
     */
    @Async
    public void processPatentAsync(Long patentId) {
        Patent patent = patentMapper.selectById(patentId);
        if (patent == null) {
            log.error("专利不存在: {}", patentId);
            return;
        }

        try {
            // 1. 如果是PDF上传，先解析PDF
            if ("FILE".equals(patent.getSourceType())) {
                patentMapper.updateParseStatus(patentId, "PARSING", null);
                parsePdfContent(patent);
            }

            // 2. LLM实体和领域提取
            patentMapper.updateParseStatus(patentId, "EXTRACTING", null);
            extractEntitiesAndDomains(patent);

            // 3. 向量化存储
            patentMapper.updateParseStatus(patentId, "VECTORIZING", null);
            storeVector(patent);

            // 4. 完成
            patentMapper.updateParseStatus(patentId, "SUCCESS", null);
            log.info("专利处理完成: {}", patentId);

            // 5. 同步到ES索引（用于全文检索）
            try {
                searchService.syncPatentToEs(patentId);
                log.info("专利同步到ES成功: {}", patentId);
            } catch (Exception e) {
                log.warn("专利同步到ES失败（不影响主流程）: {}", patentId, e);
            }

            // 6. 清除统计缓存（确保统计数据实时更新）
            try {
                statsService.evictAllStatsCache();
            } catch (Exception e) {
                log.warn("清除统计缓存失败（不影响主流程）: {}", e.getMessage());
            }

        } catch (Exception e) {
            log.error("专利处理失败: {}", patentId, e);
            patentMapper.updateParseStatus(patentId, "FAILED", e.getMessage());
        }
    }

    /**
     * 解析PDF内容
     * PDF结构：
     * - 专利名称
     * - 公开号
     * - 公开日期
     * - 申请人
     * - IPC分类（由LLM提取到patent_domain表）
     * - 摘要
     * - 正文（保存在MinIO中，摘要存数据库）
     */
    @Transactional
    public void parsePdfContent(Patent patent) {
        try {
            InputStream pdfStream = fileService.getFile(patent.getFilePath());

            // 使用Spring AI PDF Reader解析
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(
                    new InputStreamResource(pdfStream),
                    PdfDocumentReaderConfig.builder()
                            .withPagesPerDocument(1)
                            .build());

            List<Document> documents = pdfReader.read();

            // 合并所有页面文本
            String fullText = documents.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n"));

            // 解析PDF元数据
            parsePdfMetadata(patent, fullText);

            patentMapper.updateById(patent);
            log.info("PDF解析完成: {}", patent.getId());

        } catch (Exception e) {
            log.error("PDF解析失败: {}", patent.getId(), e);
            throw new RuntimeException("PDF解析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析PDF元数据
     * 从PDF文本中提取：专利名称、公开号、公开日期、申请人、摘要
     */
    private void parsePdfMetadata(Patent patent, String fullText) {
        String[] lines = fullText.split("\n");
        StringBuilder abstractBuilder = new StringBuilder();
        boolean inAbstract = false;
        boolean inMainContent = false;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            // 解析专利名称
            if (trimmedLine.startsWith("专利名称:") || trimmedLine.startsWith("专利名称：")) {
                String title = trimmedLine.substring(trimmedLine.indexOf(":") + 1).trim();
                if (title.isEmpty() && trimmedLine.contains("：")) {
                    title = trimmedLine.substring(trimmedLine.indexOf("：") + 1).trim();
                }
                if (!title.isEmpty() && (patent.getTitle() == null || patent.getTitle().isEmpty())) {
                    patent.setTitle(title);
                }
            }
            // 解析公开号
            else if (trimmedLine.startsWith("公开号:") || trimmedLine.startsWith("公开号：")) {
                String pubNo = extractValue(trimmedLine);
                if (!pubNo.isEmpty() && (patent.getPublicationNo() == null || patent.getPublicationNo().isEmpty())) {
                    patent.setPublicationNo(pubNo);
                }
            }
            // 解析公开日期
            else if (trimmedLine.startsWith("公开日期:") || trimmedLine.startsWith("公开日期：")) {
                String dateStr = extractValue(trimmedLine);
                if (!dateStr.isEmpty()) {
                    try {
                        patent.setPublicationDate(java.time.LocalDate.parse(dateStr));
                    } catch (Exception e) {
                        log.warn("解析公开日期失败: {}", dateStr);
                    }
                }
            }
            // 解析申请人
            else if (trimmedLine.startsWith("申请人:") || trimmedLine.startsWith("申请人：")) {
                String applicant = extractValue(trimmedLine);
                if (!applicant.isEmpty() && (patent.getApplicant() == null || patent.getApplicant().isEmpty())) {
                    patent.setApplicant(applicant);
                }
            }
            // 摘要开始
            else if (trimmedLine.equals("摘要:") || trimmedLine.equals("摘要：") || trimmedLine.startsWith("摘要:") || trimmedLine.startsWith("摘要：")) {
                inAbstract = true;
                String afterColon = extractValue(trimmedLine);
                if (!afterColon.isEmpty()) {
                    abstractBuilder.append(afterColon);
                }
            }
            // 正文开始，摘要结束
            else if (trimmedLine.equals("专利正文") || trimmedLine.equals("技术领域") || trimmedLine.startsWith("技术领域")) {
                inAbstract = false;
                inMainContent = true;
            }
            // 收集摘要内容
            else if (inAbstract && !inMainContent) {
                if (abstractBuilder.length() > 0) {
                    abstractBuilder.append("\n");
                }
                abstractBuilder.append(trimmedLine);
            }
        }

        // 设置摘要
        String patentAbstract = abstractBuilder.toString().trim();
        if (!patentAbstract.isEmpty()) {
            patent.setPatentAbstract(patentAbstract);
        } else {
            // 如果没有提取到摘要，使用全文（截取前2000字）
            patent.setPatentAbstract(fullText.length() > 2000 ? fullText.substring(0, 2000) : fullText);
        }

        // 如果标题还是空的，尝试从第一行获取
        if (patent.getTitle() == null || patent.getTitle().isEmpty()) {
            for (String line : lines) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.contains(":") && !trimmed.contains("：")) {
                    patent.setTitle(trimmed);
                    break;
                }
            }
        }
        
        // 最后的兜底：如果仍然没有标题，使用公开号或默认标题
        if (patent.getTitle() == null || patent.getTitle().isEmpty()) {
            if (patent.getPublicationNo() != null && !patent.getPublicationNo().isEmpty()) {
                patent.setTitle("专利-" + patent.getPublicationNo());
            } else {
                patent.setTitle("未命名专利-" + patent.getId());
            }
            log.warn("无法从PDF提取标题，使用默认标题: {}", patent.getTitle());
        }
    }

    /**
     * 提取冒号后的值
     */
    private String extractValue(String line) {
        int colonIndex = line.indexOf(":");
        if (colonIndex == -1) {
            colonIndex = line.indexOf("：");
        }
        if (colonIndex != -1 && colonIndex < line.length() - 1) {
            return line.substring(colonIndex + 1).trim();
        }
        return "";
    }

    /**
     * 提取实体和领域
     */
    @Transactional
    public void extractEntitiesAndDomains(Patent patent) {
        // 构造专利文本（标题 + 摘要）
        String patentText = String.format("标题：%s\n摘要：%s",
                patent.getTitle() != null ? patent.getTitle() : "",
                patent.getPatentAbstract() != null ? patent.getPatentAbstract() : "");

        // 调用LLM提取
        LlmService.PatentAnalysisResult result = llmService.extractEntitiesAndDomain(patentText);

        // 保存实体
        for (LlmService.EntityInfo entity : result.entities()) {
            PatentEntity pe = new PatentEntity();
            pe.setPatentId(patent.getId());
            pe.setEntityName(entity.text());
            pe.setEntityType(entity.type());
            pe.setImportance(entity.importance());
            patentEntityMapper.insert(pe);
        }

        // 保存领域（层次化）
        if (result.domain() != null) {
            LlmService.DomainInfo domain = result.domain();

            // 部
            if (domain.section() != null && !domain.section().isEmpty()) {
                saveDomain(patent.getId(), domain.section(), 1, "部-" + domain.section());
            }
            // 大类
            if (domain.mainClass() != null && !domain.mainClass().isEmpty()) {
                saveDomain(patent.getId(), domain.section() + domain.mainClass(), 2, "大类");
            }
            // 小类
            if (domain.subclass() != null && !domain.subclass().isEmpty()) {
                saveDomain(patent.getId(), domain.section() + domain.mainClass() + domain.subclass(), 3, "小类");
            }
            // 完整编码
            if (domain.fullCode() != null && !domain.fullCode().isEmpty()) {
                saveDomain(patent.getId(), domain.fullCode(), 5, domain.description());
            }
        }

        log.info("实体和领域提取完成: {}, 实体数: {}", patent.getId(), result.entities().size());
    }

    /**
     * 保存领域
     */
    private void saveDomain(Long patentId, String code, int level, String desc) {
        PatentDomain pd = new PatentDomain();
        pd.setPatentId(patentId);
        // 截断超长字段，防止数据库插入失败
        pd.setDomainCode(truncateString(code, 100));
        pd.setDomainLevel(level);
        pd.setDomainDesc(truncateString(desc, 200));
        patentDomainMapper.insert(pd);
    }
    
    /**
     * 截断字符串
     */
    private String truncateString(String str, int maxLength) {
        if (str == null) return null;
        return str.length() > maxLength ? str.substring(0, maxLength) : str;
    }

    /**
     * 存储向量
     */
    @Transactional
    public void storeVector(Patent patent) {
        List<PatentEntity> entities = patentEntityMapper.selectByPatentId(patent.getId());
        List<PatentDomain> domains = patentDomainMapper.selectByPatentId(patent.getId());

        String vectorId = vectorService.storePatentVector(patent, entities, domains);

        // 保存向量映射
        PatentVector pv = new PatentVector();
        pv.setPatentId(patent.getId());
        pv.setVectorId(vectorId);
        // 根据LLM模式设置embedding模型名称
        String embeddingModel = "online".equals(patentConfig.getLlmMode()) 
                ? "text-embedding-v3" : "nomic-embed-text";
        pv.setEmbeddingModel(embeddingModel);
        pv.setVectorDim(patentConfig.getVectorDimension());
        patentVectorMapper.insert(pv);

        log.info("向量存储完成: {}, vectorId: {}", patent.getId(), vectorId);
    }
}
