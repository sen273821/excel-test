package com.hanpeng.excel.service;

import com.hanpeng.excel.dto.OrderDTO;
import com.hanpeng.excel.dto.ParseResultDTO;

import java.util.List;

public interface ValidationService {

    List<ParseResultDTO.FieldErrorDTO> validateOrder(OrderDTO order);

    List<ParseResultDTO.RowDataDTO> validateAll(List<ParseResultDTO.RowDataDTO> rows);
}
