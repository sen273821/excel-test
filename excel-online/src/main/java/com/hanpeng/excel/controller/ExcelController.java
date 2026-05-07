package com.hanpeng.excel.controller;

import com.hanpeng.excel.dto.ResultVO;
import com.hanpeng.excel.entity.ExcelData;
import com.hanpeng.excel.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/excel")
public class ExcelController {

    @Autowired
    private ExcelService excelService;

    @PostMapping("/import")
    public ResultVO<Void> importExcel(
            @RequestParam("file") MultipartFile file) {
        excelService.importExcel(file);
        return ResultVO.success();
    }

    @GetMapping("/list")
    public ResultVO<Page<ExcelData>> listData(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResultVO.success(excelService.listData(page, size));
    }

    @GetMapping("/export")
    public void exportExcel(HttpServletResponse response) {
        excelService.exportExcel(response);
    }

    @GetMapping("/template")
    public void downloadTemplate(HttpServletResponse response) {
        excelService.downloadTemplate(response);
    }
}
