package com.hanpeng.excel.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmitRequestDTO {
    private String batchId;
    private List<OrderDTO> orders;
    private String fingerprint;
    private java.util.Map<String, String> columnMapping;
}
