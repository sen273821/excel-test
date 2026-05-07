package com.hanpeng.excel.service;

import com.hanpeng.excel.entity.ExcelData;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

public interface ExcelService {

    void importExcel(MultipartFile file);

    Page<ExcelData> listData(int page, int size);

    void exportExcel(HttpServletResponse response);

    void downloadTemplate(HttpServletResponse response);
}
