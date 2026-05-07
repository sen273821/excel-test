package com.hanpeng.excel.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ParseResultDTO {
    private boolean recognized;
    private int totalRows;
    private List<String> headers;
    private Map<String, String> columnMapping;
    private List<RowDataDTO> rows;
    private String fingerprint;
    private Integer headerRowIndex;
    private String sheetName;

    @Data
    public static class RowDataDTO {
        private int rowIndex;
        private OrderDTO data;
        private List<FieldErrorDTO> errors;
    }

    @Data
    public static class FieldErrorDTO {
        private String field;
        private String fieldLabel;
        private String message;

        public FieldErrorDTO() {}

        public FieldErrorDTO(String field, String fieldLabel, String message) {
            this.field = field;
            this.fieldLabel = fieldLabel;
            this.message = message;
        }
    }
}
