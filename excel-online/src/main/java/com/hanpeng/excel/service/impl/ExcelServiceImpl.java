package com.hanpeng.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.hanpeng.excel.dto.ExcelDataDTO;
import com.hanpeng.excel.entity.ExcelData;
import com.hanpeng.excel.listener.ExcelDataListener;
import com.hanpeng.excel.repository.ExcelDataRepository;
import com.hanpeng.excel.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExcelServiceImpl implements ExcelService {

    @Autowired
    private ExcelDataRepository repository;

    @Override
    public void importExcel(MultipartFile file) {
        try {
            EasyExcel.read(file.getInputStream(),
                    ExcelDataDTO.class,
                    new ExcelDataListener(repository))
                    .sheet()
                    .doRead();
        } catch (IOException e) {
            throw new RuntimeException("读取Excel文件失败", e);
        }
    }

    @Override
    public Page<ExcelData> listData(int page, int size) {
        PageRequest pageRequest = PageRequest.of(
                page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return repository.findAll(pageRequest);
    }

    @Override
    public void exportExcel(HttpServletResponse response) {
        try {
            configureResponse(response, "exported_data");
            List<ExcelDataDTO> dtoList = buildExportData();
            EasyExcel.write(response.getOutputStream(), ExcelDataDTO.class)
                    .sheet("数据")
                    .doWrite(dtoList);
        } catch (IOException e) {
            throw new RuntimeException("导出Excel失败", e);
        }
    }

    @Override
    public void downloadTemplate(HttpServletResponse response) {
        try {
            configureResponse(response, "import_template");
            EasyExcel.write(response.getOutputStream(), ExcelDataDTO.class)
                    .sheet("模板")
                    .doWrite(Collections.emptyList());
        } catch (IOException e) {
            throw new RuntimeException("下载模板失败", e);
        }
    }

    private void configureResponse(HttpServletResponse response,
                                   String fileName) throws IOException {
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encoded = URLEncoder.encode(fileName, "UTF-8");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + encoded + ".xlsx");
    }

    private List<ExcelDataDTO> buildExportData() {
        return repository.findAll().stream()
                .map(this::entityToDTO)
                .collect(Collectors.toList());
    }

    private ExcelDataDTO entityToDTO(ExcelData entity) {
        ExcelDataDTO dto = new ExcelDataDTO();
        dto.setName(entity.getName());
        dto.setPhone(entity.getPhone());
        dto.setEmail(entity.getEmail());
        dto.setAddress(entity.getAddress());
        dto.setRemark(entity.getRemark());
        return dto;
    }
}
