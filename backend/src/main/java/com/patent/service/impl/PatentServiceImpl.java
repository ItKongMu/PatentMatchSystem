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
import com.patent.service.GraphService;
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
    private final GraphService graphService;
    private final SysUserMapper sysUserMapper;

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

        // 如果有正文或IPC分类，生成文本文件存入MinIO
        if ((dto.getFullText() != null && !dto.getFullText().trim().isEmpty()) ||
            (dto.getIpcClassification() != null && !dto.getIpcClassification().trim().isEmpty())) {
            String filePath = generateAndStoreTextFile(dto, patent);
            patent.setFilePath(filePath);
        }

        patentMapper.insert(patent);

        // 如果有IPC分类，保存到patent_domain表
        if (dto.getIpcClassification() != null && !dto.getIpcClassification().trim().isEmpty()) {
            saveIpcClassification(patent.getId(), dto.getIpcClassification());
        }

        log.info("专利文本录入成功, id: {}, title: {}, hasFullText: {}", 
                patent.getId(), dto.getTitle(), dto.getFullText() != null && !dto.getFullText().isEmpty());

        // 构建响应
        UploadResultVO result = new UploadResultVO();
        result.setPatentId(patent.getId());
        result.setFilePath(patent.getFilePath());
        result.setParseStatus("PENDING");
        result.setMessage("录入成功，等待处理");

        return result;
    }

    /**
     * 生成结构化文本文件并存入MinIO（用于TEXT录入）
     */
    private String generateAndStoreTextFile(PatentTextDTO dto, Patent patent) {
        StringBuilder content = new StringBuilder();
        content.append("专利名称：").append(dto.getTitle() != null ? dto.getTitle() : "").append("\n");
        content.append("公开号：").append(dto.getPublicationNo() != null ? dto.getPublicationNo() : "").append("\n");
        content.append("公开日期：").append(dto.getPublicationDate() != null ? dto.getPublicationDate().toString() : "").append("\n");
        content.append("申请人：").append(dto.getApplicant() != null ? dto.getApplicant() : "").append("\n");
        
        if (dto.getIpcClassification() != null && !dto.getIpcClassification().trim().isEmpty()) {
            content.append("IPC分类：").append(dto.getIpcClassification()).append("\n");
        }
        
        content.append("\n摘要：\n");
        content.append(dto.getPatentAbstract() != null ? dto.getPatentAbstract() : "").append("\n");
        
        if (dto.getFullText() != null && !dto.getFullText().trim().isEmpty()) {
            content.append("\n专利正文\n");
            content.append(dto.getFullText()).append("\n");
        }

        // 生成唯一文件名
        String objectName = String.format("patents/text_%s.txt", IdUtil.fastSimpleUUID());

        // 上传到MinIO
        byte[] bytes = content.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        fileService.uploadBytes(bytes, objectName, "text/plain; charset=utf-8");

        log.info("TEXT专利文本文件生成并上传成功: {}", objectName);
        return objectName;
    }

    @Override
    public void processPatent(Long patentId) {
        Patent patent = patentMapper.selectById(patentId);
        if (patent == null) {
            throw new BusinessException("专利不存在");
        }

        Long currentUserId = getCurrentUserId();
        // 校验权限：创建者本人或管理员可触发
        if (!isAdminUser(currentUserId) && !currentUserId.equals(patent.getCreatedBy())) {
            throw new BusinessException("无权限处理此专利");
        }

        // 仅允许 PENDING / FAILED 状态的专利触发（避免重复处理正在进行的专利）
        String status = patent.getParseStatus();
        if (!"PENDING".equals(status) && !"FAILED".equals(status)) {
            throw new BusinessException("专利当前状态为 " + status + "，不可触发处理（请使用重新处理接口）");
        }

        patentProcessorService.processPatentAsync(patentId);
    }

    @Override
    public void reprocessPatent(Long patentId) {
        Patent patent = patentMapper.selectById(patentId);
        if (patent == null) {
            throw new BusinessException("专利不存在");
        }

        Long currentUserId = getCurrentUserId();
        // 仅管理员可强制重新处理
        if (!isAdminUser(currentUserId)) {
            throw new BusinessException("无权限执行重新处理，仅管理员可操作");
        }

        log.info("管理员 {} 触发专利重新处理: patentId={}, currentStatus={}", currentUserId, patentId, patent.getParseStatus());
        // 重置状态为 PENDING，再触发异步处理
        patentMapper.updateParseStatus(patentId, "PENDING", null);
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
    @Transactional(rollbackFor = Exception.class)
    public void deletePatent(Long patentId) {
        Patent patent = patentMapper.selectById(patentId);
        if (patent == null) {
            throw new BusinessException("专利不存在");
        }

        // 权限校验：创建者本人或管理员可删除
        Long currentUserId = getCurrentUserId();
        if (!isAdminUser(currentUserId)
                && (patent.getCreatedBy() == null || !patent.getCreatedBy().equals(currentUserId))) {
            throw new BusinessException("无权限删除此专利");
        }

        // 删除 Qdrant 向量（外部服务，失败不回滚 MySQL 事务）
        PatentVector vector = patentVectorMapper.selectByPatentId(patentId);
        if (vector != null) {
            try {
                vectorService.deleteVector(vector.getVectorId());
            } catch (Exception e) {
                log.warn("删除 Qdrant 向量失败（继续执行）: vectorId={}", vector.getVectorId(), e);
            }
            patentVectorMapper.deleteByPatentId(patentId);
        }

        // 删除关联数据（MySQL 事务内）
        patentEntityMapper.deleteByPatentId(patentId);
        patentDomainMapper.deleteByPatentId(patentId);

        // 删除MinIO文件（外部服务，失败不回滚）
        if (patent.getFilePath() != null) {
            try {
                fileService.deleteFile(patent.getFilePath());
            } catch (Exception e) {
                log.warn("删除MinIO文件失败: {}", patent.getFilePath(), e);
            }
        }

        // 从ES删除索引（外部服务，失败不回滚）
        try {
            searchService.deleteFromEs(patentId);
        } catch (Exception e) {
            log.warn("从ES删除专利索引失败: {}", patentId, e);
        }

        // 从 Neo4j 删除图谱节点
        if (patent.getPublicationNo() != null) {
            try {
                graphService.deletePatentGraph(patent.getPublicationNo());
            } catch (Exception e) {
                log.warn("从Neo4j删除专利图谱失败: {}", patentId, e);
            }
        }

        // 删除专利记录
        patentMapper.deleteById(patentId);
        log.info("专利删除成功: {}", patentId);
    }

    /**
     * 获取当前用户ID（未登录时返回 null）
     */
    private Long getCurrentUserId() {
        try {
            return authService.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断指定用户是否为管理员（直接查 SysUserMapper）
     */
    private boolean isAdminUser(Long userId) {
        if (userId == null) return false;
        try {
            SysUser user = sysUserMapper.selectById(userId);
            return user != null && "admin".equals(user.getRole());
        } catch (Exception e) {
            return false;
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
        result.setHeaders(Arrays.asList("公开号", "标题", "申请人", "公开日期", "IPC分类", "摘要", "正文"));
        
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
    public CsvImportResultVO importCsv(MultipartFile file, boolean autoProcess) {
        validateCsvFile(file);
        List<PatentCsvDTO> dataList = parseCsvFile(file);
        return doImport(dataList, autoProcess);
    }

    @Override
    public CsvImportResultVO importCsvData(List<PatentCsvDTO> dataList, boolean autoProcess) {
        return doImport(dataList, autoProcess);
    }

    /**
     * 执行导入
     *
     * <p>修复说明：
     * <ul>
     *   <li>去除方法级 @Transactional：原来整个循环在一个大事务中，若某条记录失败会回滚所有已成功记录；
     *       改为每条记录独立事务（通过 insertSinglePatent 方法），互不影响。</li>
     *   <li>异步处理在事务外触发：原代码在 @Transactional 方法内部调用异步处理，事务尚未提交导致
     *       异步线程查不到刚插入的 patent 记录；现在事务提交后再调用异步处理。</li>
     *   <li>batchSize 修正：batchSize 是"每批并行处理的专利数"，应固定为合理值（默认10），
     *       而非 min(20, total)（当只有1条时 batchSize=1 无意义）。</li>
     * </ul>
     * </p>
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
                // 每条记录独立事务插入，互不影响
                Long patentId = insertSinglePatent(dto, userId);
                result.addSuccess(patentId);
            } catch (Exception e) {
                log.error("导入专利失败: {}", dto, e);
                dto.setErrorMessage("导入失败：" + e.getMessage());
                dto.setValid(false);
                result.addFailed(dto);
            }
        }

        // 事务已全部提交后，再触发异步处理（避免异步线程查不到刚插入的记录）
        if (autoProcess && !result.getImportedPatentIds().isEmpty()) {
            List<Long> patentIds = result.getImportedPatentIds();
            // batchSize 固定为10：每批向量化10条，平衡 API 并发压力与处理速度
            int batchSize = 10;
            try {
                patentProcessorService.batchProcessPatentsAsync(patentIds, batchSize);
                log.info("已提交批量处理任务, 数量: {}, 每批大小: {}", patentIds.size(), batchSize);
            } catch (Exception e) {
                log.warn("提交批量处理任务失败，降级为逐个处理: {}", e.getMessage());
                for (Long patentId : patentIds) {
                    try {
                        patentProcessorService.processPatentAsync(patentId);
                    } catch (Exception ex) {
                        log.warn("自动处理专利 {} 失败: {}", patentId, ex.getMessage());
                    }
                }
            }
        }

        result.buildMessage();
        log.info("CSV导入完成: {}", result.getMessage());
        return result;
    }

    /**
     * 单条专利记录独立事务插入
     * 独立事务确保：每条记录成功/失败互不影响；事务提交后异步处理可见
     *
     * @return 新插入的专利 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long insertSinglePatent(PatentCsvDTO dto, Long userId) {
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

        // 如果有正文，生成文本文件存入MinIO
        if (dto.getFullText() != null && !dto.getFullText().trim().isEmpty()) {
            String filePath = generateAndStorePatentTextFile(dto, patent);
            patent.setFilePath(filePath);
        }

        patentMapper.insert(patent);

        // 保存IPC分类到PatentDomain表
        if (dto.getIpcClassification() != null && !dto.getIpcClassification().trim().isEmpty()) {
            saveIpcClassification(patent.getId(), dto.getIpcClassification().trim());
        }

        return patent.getId();
    }
    
    /**
     * 生成专利文本文件并存入MinIO
     * 文件格式与PDF解析后的格式一致，便于后续统一处理
     */
    private String generateAndStorePatentTextFile(PatentCsvDTO dto, Patent patent) {
        StringBuilder content = new StringBuilder();
        
        // 构建与PDF格式一致的文本结构
        if (dto.getTitle() != null && !dto.getTitle().trim().isEmpty()) {
            content.append("专利名称：").append(dto.getTitle().trim()).append("\n\n");
        }
        if (dto.getPublicationNo() != null && !dto.getPublicationNo().trim().isEmpty()) {
            content.append("公开号：").append(dto.getPublicationNo().trim()).append("\n");
        }
        if (dto.getPublicationDate() != null && !dto.getPublicationDate().trim().isEmpty()) {
            content.append("公开日期：").append(dto.getPublicationDate().trim()).append("\n");
        }
        if (dto.getApplicant() != null && !dto.getApplicant().trim().isEmpty()) {
            content.append("申请人：").append(dto.getApplicant().trim()).append("\n");
        }
        if (dto.getIpcClassification() != null && !dto.getIpcClassification().trim().isEmpty()) {
            content.append("IPC分类：").append(dto.getIpcClassification().trim()).append("\n");
        }
        content.append("\n");
        
        // 摘要
        if (dto.getPatentAbstract() != null && !dto.getPatentAbstract().trim().isEmpty()) {
            content.append("摘要：\n").append(dto.getPatentAbstract().trim()).append("\n\n");
        }
        
        // 正文
        if (dto.getFullText() != null && !dto.getFullText().trim().isEmpty()) {
            content.append("专利正文\n").append(dto.getFullText().trim()).append("\n");
        }
        
        // 生成文件名：patents/csv_UUID.txt
        String objectName = String.format("patents/csv_%s.txt", IdUtil.fastSimpleUUID());
        
        // 存入MinIO
        try {
            byte[] contentBytes = content.toString().getBytes(StandardCharsets.UTF_8);
            fileService.uploadBytes(contentBytes, objectName, "text/plain; charset=utf-8");
            log.info("CSV专利文本文件已存入MinIO: {}", objectName);
            return objectName;
        } catch (Exception e) {
            log.error("存储CSV专利文本文件失败", e);
            throw new BusinessException("存储专利文本文件失败：" + e.getMessage());
        }
    }
    
    /**
     * 保存IPC分类到PatentDomain表
     * @param patentId 专利ID
     * @param ipcClassification IPC分类字符串（多个用逗号分隔）
     */
    private void saveIpcClassification(Long patentId, String ipcClassification) {
        // 解析IPC分类（支持逗号、分号、空格分隔）
        String[] ipcCodes = ipcClassification.split("[,;\\s]+");
        
        for (String ipcCode : ipcCodes) {
            ipcCode = ipcCode.trim();
            if (ipcCode.isEmpty()) {
                continue;
            }
            
            PatentDomain domain = new PatentDomain();
            domain.setPatentId(patentId);
            domain.setDomainCode(ipcCode);
            
            // 根据IPC编码长度判断层级
            // 例如: G(部) -> G06(大类) -> G06F(小类) -> G06F16(主组) -> G06F16/30(分组)
            int level = determineIpcLevel(ipcCode);
            domain.setDomainLevel(level);
            
            // 设置领域描述（使用IPC编码本身作为描述，后续可由LLM补充）
            domain.setDomainDesc(ipcCode);
            
            patentDomainMapper.insert(domain);
        }
    }
    
    /**
     * 根据IPC编码判断层级
     */
    private int determineIpcLevel(String ipcCode) {
        if (ipcCode == null || ipcCode.isEmpty()) {
            return 1;
        }
        
        // 移除空格
        ipcCode = ipcCode.replace(" ", "");
        
        // 包含斜杠的是分组级别(5)或主组级别(4)
        if (ipcCode.contains("/")) {
            String[] parts = ipcCode.split("/");
            if (parts.length >= 2 && parts[1].length() > 2) {
                return 5; // 分组
            }
            return 4; // 主组
        }
        
        // 根据长度判断
        int len = ipcCode.length();
        if (len == 1) {
            return 1; // 部 (如 G)
        } else if (len <= 3) {
            return 2; // 大类 (如 G06)
        } else if (len <= 4) {
            return 3; // 小类 (如 G06F)
        } else {
            return 4; // 主组 (如 G06F16)
        }
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
     * 支持格式: 公开号,标题,申请人,公开日期,IPC分类,摘要,正文(可选)
     */
    private PatentCsvDTO parseCsvLine(String line, int rowNum) {
        PatentCsvDTO dto = new PatentCsvDTO();
        dto.setRowNum(rowNum);
        
        // 解析CSV（支持引号包围的字段）
        List<String> fields = parseCsvFields(line);
        
        if (fields.size() >= 7) {
            // 完整格式：公开号,标题,申请人,公开日期,IPC分类,摘要,正文
            dto.setPublicationNo(fields.get(0));
            dto.setTitle(fields.get(1));
            dto.setApplicant(fields.get(2));
            dto.setPublicationDate(fields.get(3));
            dto.setIpcClassification(fields.get(4));
            dto.setPatentAbstract(fields.get(5));
            dto.setFullText(fields.get(6));
        } else if (fields.size() == 6) {
            // 6列格式：公开号,标题,申请人,公开日期,IPC分类,摘要（无正文）
            dto.setPublicationNo(fields.get(0));
            dto.setTitle(fields.get(1));
            dto.setApplicant(fields.get(2));
            dto.setPublicationDate(fields.get(3));
            dto.setIpcClassification(fields.get(4));
            dto.setPatentAbstract(fields.get(5));
        } else if (fields.size() == 5) {
            // 旧格式兼容：公开号,标题,申请人,公开日期,摘要（无IPC分类）
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
