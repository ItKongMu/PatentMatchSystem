package com.patent.service;

import com.patent.common.PageResult;
import com.patent.model.dto.PatentCsvDTO;
import com.patent.model.dto.PatentTextDTO;
import com.patent.model.vo.CsvImportResultVO;
import com.patent.model.vo.CsvPreviewVO;
import com.patent.model.vo.PatentListVO;
import com.patent.model.vo.PatentVO;
import com.patent.model.vo.UploadResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 专利服务接口
 */
public interface PatentService {

    /**
     * 上传专利PDF文件
     *
     * @param file          PDF文件
     * @param publicationNo 公开号
     * @return 上传结果
     */
    UploadResultVO uploadPatentPdf(MultipartFile file, String publicationNo);

    /**
     * 文本录入专利
     *
     * @param dto 专利文本信息
     * @return 上传结果
     */
    UploadResultVO createPatentFromText(PatentTextDTO dto);

    /**
     * 触发专利处理流程
     *
     * @param patentId 专利ID
     */
    void processPatent(Long patentId);

    /**
     * 获取专利详情
     *
     * @param patentId 专利ID
     * @return 专利详情
     */
    PatentVO getPatentDetail(Long patentId);

    /**
     * 分页查询专利列表
     *
     * @param pageNum     页码
     * @param pageSize    每页大小
     * @param parseStatus 解析状态（可选）
     * @param keyword     关键词（可选）
     * @return 分页结果
     */
    PageResult<PatentListVO> getPatentList(Integer pageNum, Integer pageSize, String parseStatus, String keyword);

    /**
     * 删除专利
     *
     * @param patentId 专利ID
     */
    void deletePatent(Long patentId);

    /**
     * 预览CSV文件内容
     *
     * @param file CSV文件
     * @return 预览结果
     */
    CsvPreviewVO previewCsv(MultipartFile file);

    /**
     * 批量导入CSV专利数据
     *
     * @param file         CSV文件
     * @param autoProcess  是否自动触发处理流程
     * @return 导入结果
     */
    CsvImportResultVO importCsv(MultipartFile file, boolean autoProcess);

    /**
     * 导入预览的CSV数据
     *
     * @param dataList     预览的数据列表
     * @param autoProcess  是否自动触发处理流程
     * @return 导入结果
     */
    CsvImportResultVO importCsvData(List<PatentCsvDTO> dataList, boolean autoProcess);
}
