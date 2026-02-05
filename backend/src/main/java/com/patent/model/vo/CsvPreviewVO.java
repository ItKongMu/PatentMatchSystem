package com.patent.model.vo;

import com.patent.model.dto.PatentCsvDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * CSV预览结果VO
 */
@Data
@Schema(description = "CSV预览结果")
public class CsvPreviewVO {

    @Schema(description = "总行数")
    private Integer totalRows = 0;

    @Schema(description = "有效行数")
    private Integer validRows = 0;

    @Schema(description = "无效行数")
    private Integer invalidRows = 0;

    @Schema(description = "预览数据（前N行）")
    private List<PatentCsvDTO> previewData = new ArrayList<>();

    @Schema(description = "所有解析的数据")
    private List<PatentCsvDTO> allData = new ArrayList<>();

    @Schema(description = "CSV表头")
    private List<String> headers = new ArrayList<>();

    @Schema(description = "解析状态消息")
    private String message;

    @Schema(description = "是否可以导入")
    private Boolean canImport = true;
}
