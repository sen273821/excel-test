package com.hanpeng.excel.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubmitResultDTO {
    private int total;
    private int success;
    private int failed;
    private List<FailedRow> failedRows;

    @Data
    public static class FailedRow {
        private int rowIndex;
        private String reason;

        public FailedRow() {}

        public FailedRow(int rowIndex, String reason) {
            this.rowIndex = rowIndex;
            this.reason = reason;
        }
    }
}
