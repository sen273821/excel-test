package com.hanpeng.excel.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.hanpeng.excel.dto.ExcelDataDTO;
import com.hanpeng.excel.entity.ExcelData;
import com.hanpeng.excel.repository.ExcelDataRepository;

import java.util.ArrayList;
import java.util.List;

public class ExcelDataListener extends AnalysisEventListener<ExcelDataDTO> {

    private static final int BATCH_SIZE = 500;
    private final List<ExcelData> dataList = new ArrayList<>();
    private final ExcelDataRepository repository;

    public ExcelDataListener(ExcelDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void invoke(ExcelDataDTO dto, AnalysisContext context) {
        dataList.add(convertToEntity(dto));
        if (dataList.size() >= BATCH_SIZE) {
            saveData();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
    }

    private void saveData() {
        repository.saveAll(dataList);
        dataList.clear();
    }

    private ExcelData convertToEntity(ExcelDataDTO dto) {
        ExcelData entity = new ExcelData();
        entity.setName(dto.getName());
        entity.setPhone(dto.getPhone());
        entity.setEmail(dto.getEmail());
        entity.setAddress(dto.getAddress());
        entity.setRemark(dto.getRemark());
        return entity;
    }
}
