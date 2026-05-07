package com.hanpeng.excel.service;

import com.hanpeng.excel.dto.ParseResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface TemplateRecognitionService {

    ParseResultDTO parseExcel(MultipartFile file);

    ParseResultDTO parseExcelWithMapping(MultipartFile file, Map<String, String> columnMapping,
                                          Integer headerRowIndex, String sheetName);
}
