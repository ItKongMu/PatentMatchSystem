package com.patent.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.patent.common.PageResult;
import com.patent.common.exception.BusinessException;
import com.patent.mapper.*;
import com.patent.model.dto.PatentCsvDTO;
import com.patent.model.dto.PatentTextDTO;
import com.patent.model.entity.*;
import com.patent.model.vo.CsvImportResultVO;
import com.patent.model.vo.CsvPreviewVO;
import com.patent.model.vo.PatentListVO;
import com.patent.model.vo.PatentVO;
import com.patent.model.vo.UploadResultVO;
import com.patent.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 专利服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatentServiceImpl implements PatentService {

    private final PatentMapper patentMapper;
    private final PatentEntityMapper patentEntityMapper;
    private final PatentDomainMapper patentDomainMapper;
    private final PatentVectorMapper patentVectorMapper;
    private final FileService fileService;
    private final VectorService vectorService;
    private final AuthService authService;
    private final PatentProcessorService patentProcessorService;
    private final SearchService searchService;

    @Override
    @Transactional
    public UploadResultVO uploadPatentPdf(MultipartFile file, String publicationNo) {
        // 验证文件
        if (file.isEmpty()) {
            throw new BusinessException("请选择要上传的文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new BusinessException("只支持PDF文件");
        }

        // 生成唯一文件名：patents/UUID.pdf
        String objectName = String.format("patents/%s.pdf", IdUtil.fastSimpleUUID());

        // 上传到MinIO
        fileService.uploadFile(file, objectName);

        // 保存专利记录（title由LLM处理时从PDF提取）
        Patent patent = new Patent();
        patent.setPublicationNo(publicationNo);
        patent.setFilePath(objectName);
        patent.setSourceType("FILE");
        patent.setParseStatus("PENDING");
        patent.setCreatedBy(getCurrentUserId());
        patentMapper.insert(patent);

        log.info("专利PDF上传成功, id: {}, path: {}", patent.getId(), objectName);

        // 构建响应
        UploadResultVO result = new UploadResultVO();
        result.setPatentId(patent.getId());
        result.setFilePath(objectName);
        result.setParseStatus("PENDING");
        result.setMessage("上传成功，等待处理");

        return result;
    }

    @Override
    @Transactional
    public UploadResultVO createPatentFromText(PatentTextDTO dto) {
        // 创建专利记录
        Patent patent = new Patent();
        patent.setPublicationNo(dto.getPublicationNo());
        patent.setTitle(dto.getTitle());
        patent.setApplicant(dto.getApplicant());
        patent.setPublicationDate(dto.getPublicationDate());
        patent.setPatentAbstract(dto.getPatentAbstract());
        patent.setSourceType("TEXT");
        patent.setParseStatus("PENDING");
        patent.setCreatedBy(getCurrentUserId());
        patentMapper.insert(patent);

        log.info("专利文本录入成功, id: {}, title: {}", patent.getId(), dto.getTitle());

        // 构建响应
        UploadResultVO result = new UploadResultVO();
        result.setPatentId(patent.getId());
        result.setParseStatus("PENDING");
        result.setMessage("录入成功，等待处理");

        return result;
    }

    @Override
    public void processPatent(Long patentId) {
        // 委托给异步处理服务
        patentProcessorService.processPatentAsync(patentId);
    }

    @Override
    public PatentVO getPatentDetail(Long patentId) {
        Patent patent = patentMapper.selectById(patentId);
        if (patent == null) {
            throw new BusinessException("专利不存在");
        }

        PatentVO vo = new PatentVO();
        BeanUtils.copyProperties(patent, vo);

        // 查询实体
        List<PatentEntity> entities = patentEntityMapper.selectByPatentId(patentId);
        vo.setEntities(entities.stream().map(e -> {
            PatentVO.EntityVO entityVO = new PatentVO.EntityVO();
            entityVO.setId(e.getId());
            entityVO.setEntityName(e.getEntityName());
            entityVO.setEntityType(e.getEntityType());
            entityVO.setImportance(e.getImportance());
            return entityVO;
        }).toList());

        // 查询领域
        List<PatentDomain> domains = patentDomainMapper.selectByPatentId(patentId);
        vo.setDomains(domains.stream().map(d -> {
            PatentVO.DomainVO domainVO = new PatentVO.DomainVO();
            domainVO.setDomainCode(d.getDomainCode());
            domainVO.setDomainLevel(d.getDomainLevel());
            domainVO.setDomainDesc(d.getDomainDesc());
            return domainVO;
        }).toList());

        // 查询向量信息
        PatentVector vector = patentVectorMapper.selectByPatentId(patentId);
        if (vector != null) {
            PatentVO.VectorVO vectorVO = new PatentVO.VectorVO();
            vectorVO.setVectorId(vector.getVectorId());
            vectorVO.setEmbeddingModel(vector.getEmbeddingModel());
            vectorVO.setVectorDim(vector.getVectorDim());
            vo.setVector(vectorVO);
        }

        return vo;
    }

    @Override
    public PageResult<PatentListVO> getPatentList(Integer pageNum, Integer pageSize, String parseStatus, String keyword) {
        Page<PatentListVO> page = new Page<>(pageNum, pageSize);
        IPage<PatentListVO> result = patentMapper.selectPatentPage(page, parseStatus, keyword, null);
        return PageResult.of(result.getRecords(), result.getTotal(), pageNum, pageSize);
    }

    @Override
    @Transactional
    public void deletePatent(Long patentId) {
        Patent patent = patentMapper.selectById(patentId);
        if (patent == null) {
            throw new BusinessException("专利不存在");
        }

        // 删除向量
        PatentVector vector = patentVectorMapper.selectByPatentId(patentId);
        if (vector != null) {
            vectorService.deleteVector(vector.getVectorId());
            patentVectorMapper.deleteByPatentId(patentId);
        }

        // 删除关联数据
        patentEntityMapper.deleteByPatentId(patentId);
        patentDomainMapper.deleteByPatentId(patentId);

        // 删除MinIO文件
        if (patent.getFilePath() != null) {
            try {
                fileService.deleteFile(patent.getFilePath());
            } catch (Exception e) {
                log.warn("删除MinIO文件失败: {}", patent.getFilePath(), e);
            }
        }

        // 从ES删除索引
        try {
            searchService.deleteFromEs(patentId);
        } catch (Exception e) {
            log.warn("从ES删除专利索引失败: {}", patentId, e);
        }

        // 删除专利记录
        patentMapper.deleteById(patentId);
        log.info("专利删除成功: {}", patentId);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            return authService.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public CsvPreviewVO previewCsv(MultipartFile file) {
        validateCsvFile(file);
        CsvPreviewVO result = new CsvPreviewVO();
        List<PatentCsvDTO> allData = parseCsvFile(file);
        
        result.setTotalRows(allData.size());
        result.setAllData(allData);
        
        // 统计有效/无效行数
        int validCount = 0;
        int invalidCount = 0;
        for (PatentCsvDTO dto : allData) {
            if (dto.getValid()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }
        result.setValidRows(validCount);
        result.setInvalidRows(invalidCount);
        
        // 预览前20行
        int previewSize = Math.min(20, allData.size());
        result.setPreviewData(allData.subList(0, previewSize));
        
        // 设置表头
        result.setHeaders(Arrays.asList("公开号", "标题", "申请人", "公开日期", "摘要"));
        
        // 设置消息
        if (invalidCount > 0) {
            result.setMessage(String.format("解析完成：共%d条数据，其中%d条有效，%d条无效", 
                    allData.size(), validCount, invalidCount));
            result.setCanImport(validCount > 0);
        } else {
            result.setMessage(String.format("解析完成：共%d条有效数据", allData.size()));
        }
        
        return result;
    }

    @Override
    @Transactional
    public CsvImportResultVO importCsv(MultipartFile file, boolean autoProcess) {
        validateCsvFile(file);
        List<PatentCsvDTO> dataList = parseCsvFile(file);
        return doImport(dataList, autoProcess);
    }

    @Override
    @Transactional
    public CsvImportResultVO importCsvData(List<PatentCsvDTO> dataList, boolean autoProcess) {
        return doImport(dataList, autoProcess);
    }

    /**
     * 执行导入
     */
    private CsvImportResultVO doImport(List<PatentCsvDTO> dataList, boolean autoProcess) {
        CsvImportResultVO result = new CsvImportResultVO();
        result.setTotalRows(dataList.size());
        Long userId = getCurrentUserId();

        for (PatentCsvDTO dto : dataList) {
            // 跳过无效数据
            if (!dto.getValid()) {
                result.addFailed(dto);
                continue;
            }

            // 检查公开号是否重复
            if (dto.getPublicationNo() != null && !dto.getPublicationNo().trim().isEmpty()) {
                Patent existing = patentMapper.selectByPublicationNo(dto.getPublicationNo().trim());
                if (existing != null) {
                    dto.setErrorMessage("公开号已存在：" + dto.getPublicationNo());
                    dto.setValid(false);
                    result.addSkipped();
                    continue;
                }
            }

            try {
                // 创建专利记录
                Patent patent = new Patent();
                patent.setPublicationNo(dto.getPublicationNo() != null ? dto.getPublicationNo().trim() : null);
                patent.setTitle(dto.getTitle().trim());
                patent.setApplicant(dto.getApplicant() != null ? dto.getApplicant().trim() : null);
                patent.setPatentAbstract(dto.getPatentAbstract().trim());
                patent.setSourceType("CSV");
                patent.setParseStatus("PENDING");
                patent.setCreatedBy(userId);

                // 解析日期
                if (dto.getPublicationDate() != null && !dto.getPublicationDate().trim().isEmpty()) {
                    try {
                        patent.setPublicationDate(parseDate(dto.getPublicationDate().trim()));
                    } catch (Exception e) {
                        log.warn("日期解析失败: {}", dto.getPublicationDate());
                    }
                }

                patentMapper.insert(patent);
                result.addSuccess(patent.getId());

                // 如果需要自动处理
                if (autoProcess) {
                    try {
                        patentProcessorService.processPatentAsync(patent.getId());
                    } catch (Exception e) {
                        log.warn("自动处理专利{}失败: {}", patent.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("导入专利失败: {}", dto, e);
                dto.setErrorMessage("导入失败：" + e.getMessage());
                dto.setValid(false);
                result.addFailed(dto);
            }
        }

        result.buildMessage();
        log.info("CSV导入完成: {}", result.getMessage());
        return result;
    }

    /**
     * 验证CSV文件
     */
    private void validateCsvFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("请选择要上传的CSV文件");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            throw new BusinessException("只支持CSV文件格式");
        }
        // 限制文件大小（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BusinessException("文件大小不能超过10MB");
        }
    }

    /**
     * 解析CSV文件
     */
    private List<PatentCsvDTO> parseCsvFile(MultipartFile file) {
        List<PatentCsvDTO> dataList = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            String line;
            int rowNum = 0;
            boolean isFirstLine = true;
            
            while ((line = reader.readLine()) != null) {
                rowNum++;
                
                // 跳过空行
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                // 跳过表头行（检测是否包含常见表头关键字）
                if (isFirstLine) {
                    isFirstLine = false;
                    String lowerLine = line.toLowerCase();
                    if (lowerLine.contains("标题") || lowerLine.contains("title") || 
                        lowerLine.contains("公开号") || lowerLine.contains("publication") ||
                        lowerLine.contains("摘要") || lowerLine.contains("abstract")) {
                        continue;
                    }
                }
                
                PatentCsvDTO dto = parseCsvLine(line, rowNum);
                dataList.add(dto);
            }
        } catch (Exception e) {
            log.error("CSV文件解析失败", e);
            throw new BusinessException("CSV文件解析失败：" + e.getMessage());
        }
        
        if (dataList.isEmpty()) {
            throw new BusinessException("CSV文件中没有有效数据");
        }
        
        return dataList;
    }

    /**
     * 解析CSV行
     * 支持格式: 公开号,标题,申请人,公开日期,摘要
     */
    private PatentCsvDTO parseCsvLine(String line, int rowNum) {
        PatentCsvDTO dto = new PatentCsvDTO();
        dto.setRowNum(rowNum);
        
        // 解析CSV（支持引号包围的字段）
        List<String> fields = parseCsvFields(line);
        
        if (fields.size() >= 5) {
            dto.setPublicationNo(fields.get(0));
            dto.setTitle(fields.get(1));
            dto.setApplicant(fields.get(2));
            dto.setPublicationDate(fields.get(3));
            dto.setPatentAbstract(fields.get(4));
        } else if (fields.size() >= 2) {
            // 最小格式：标题,摘要
            dto.setTitle(fields.get(0));
            dto.setPatentAbstract(fields.size() > 1 ? fields.get(1) : "");
        } else {
            dto.setValid(false);
            dto.setErrorMessage("CSV格式错误，至少需要标题和摘要两列");
        }
        
        // 验证数据
        dto.validate();
        
        return dto;
    }

    /**
     * 解析CSV字段（支持引号和转义）
     */
    private List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    // 转义的引号
                    current.append('"');
                    i++;
                } else {
                    // 切换引号状态
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                // 字段分隔符
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        
        // 添加最后一个字段
        fields.add(current.toString().trim());
        
        return fields;
    }

    /**
     * 解析日期字符串
     */
    private LocalDate parseDate(String dateStr) {
        // 尝试多种日期格式
        String[] patterns = {
            "yyyy-MM-dd",
            "yyyy/MM/dd",
            "yyyyMMdd",
            "yyyy年MM月dd日"
        };
        
        for (String pattern : patterns) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
            } catch (DateTimeParseException ignored) {
            }
        }
        
        throw new BusinessException("无法解析日期格式：" + dateStr);
    }
}
