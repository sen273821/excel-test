package com.hanpeng.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.hanpeng.excel.dto.OrderDTO;
import com.hanpeng.excel.dto.ParseResultDTO;
import com.hanpeng.excel.entity.TemplateMappingRule;
import com.hanpeng.excel.repository.TemplateMappingRuleRepository;
import com.hanpeng.excel.service.TemplateRecognitionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TemplateRecognitionServiceImpl implements TemplateRecognitionService {

    @Autowired
    private TemplateMappingRuleRepository mappingRuleRepository;

    private static final Map<String, List<String>> FIELD_ALIASES = new LinkedHashMap<>();

    static {
        FIELD_ALIASES.put("externalCode", Arrays.asList(
            "外部编码", "外部订单号", "客户单号", "Ref Code", "ref code", "外部单号"));
        FIELD_ALIASES.put("senderName", Arrays.asList(
            "发件人姓名", "发货人", "发件人", "Sender", "sender", "寄件人"));
        FIELD_ALIASES.put("senderPhone", Arrays.asList(
            "发件人电话", "发货电话", "发件电话", "Sender Tel", "sender tel", "寄件电话"));
        FIELD_ALIASES.put("senderAddress", Arrays.asList(
            "发件人地址", "发货地址", "发件地址", "Sender Address", "sender address", "寄件地址"));
        FIELD_ALIASES.put("receiverName", Arrays.asList(
            "收件人姓名", "收货人", "收件人", "Receiver", "receiver", "收方"));
        FIELD_ALIASES.put("receiverPhone", Arrays.asList(
            "收件人电话", "收货电话", "收件电话", "Receiver Tel", "receiver tel"));
        FIELD_ALIASES.put("receiverAddress", Arrays.asList(
            "收件人地址", "收货地址", "收件地址", "Receiver Address", "receiver address"));
        FIELD_ALIASES.put("weight", Arrays.asList(
            "重量(kg)", "重量(KG)", "Weight(kg)", "weight(kg)", "重量"));
        FIELD_ALIASES.put("quantity", Arrays.asList(
            "件数", "数量", "Qty", "qty", "包裹数"));
        FIELD_ALIASES.put("tempZone", Arrays.asList(
            "温层", "温度要求", "Temp Zone", "temp zone", "温度"));
        FIELD_ALIASES.put("remark", Arrays.asList(
            "备注", "附言", "Note", "note", "说明"));
    }

    @Override
    public ParseResultDTO parseExcel(MultipartFile file) {
        try {
            InputStream is = file.getInputStream();
            String filename = file.getOriginalFilename();

            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                throw new RuntimeException("不支持的文件格式，请上传 .xlsx 或 .xls 文件");
            }

            // Read all sheets' raw data to detect headers
            List<SheetData> sheetsData = readAllSheets(file);

            if (sheetsData.isEmpty()) {
                throw new RuntimeException("文件为空或无有效Sheet");
            }

            // Find the best sheet and header row
            DetectionResult detection = detectTemplate(sheetsData);

            if (detection == null) {
                throw new RuntimeException("无法检测到有效数据");
            }

            // Generate fingerprint
            String fingerprint = generateFingerprint(detection.headers);

            // Try to find saved mapping
            Map<String, String> columnMapping = detection.columnMapping;
            Optional<TemplateMappingRule> savedRule = mappingRuleRepository.findByFingerprint(fingerprint);
            if (savedRule.isPresent() && (columnMapping == null || countMappedFields(columnMapping) < 5)) {
                columnMapping = parseJsonMapping(savedRule.get().getColumnMapping());
            }

            // If still no good mapping, try alias matching
            if (columnMapping == null || countMappedFields(columnMapping) < 5) {
                columnMapping = buildMappingFromAliases(detection.headers);
            }

            boolean recognized = countMappedFields(columnMapping) >= 5;

            // Parse data rows
            List<ParseResultDTO.RowDataDTO> rows = new ArrayList<>();
            List<Map<Integer, String>> dataRows = detection.dataRows;

            for (int i = 0; i < dataRows.size(); i++) {
                Map<Integer, String> row = dataRows.get(i);
                ParseResultDTO.RowDataDTO rowData = new ParseResultDTO.RowDataDTO();
                rowData.setRowIndex(i + 1);
                OrderDTO dto = mapRowToOrder(row, detection.headers, columnMapping);
                rowData.setData(dto);
                rowData.setErrors(new ArrayList<>());
                rows.add(rowData);
            }

            ParseResultDTO result = new ParseResultDTO();
            result.setRecognized(recognized);
            result.setTotalRows(rows.size());
            result.setHeaders(detection.headers);
            result.setColumnMapping(columnMapping);
            result.setRows(rows);
            result.setFingerprint(fingerprint);
            result.setHeaderRowIndex(detection.headerRowIndex);
            result.setSheetName(detection.sheetName);

            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("解析Excel文件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public ParseResultDTO parseExcelWithMapping(MultipartFile file, Map<String, String> columnMapping,
                                                 Integer headerRowIndex, String sheetName) {
        try {
            List<SheetData> sheetsData = readAllSheets(file);
            SheetData targetSheet = null;

            if (sheetName != null) {
                targetSheet = sheetsData.stream()
                    .filter(s -> sheetName.equals(s.sheetName))
                    .findFirst().orElse(null);
            }
            if (targetSheet == null) {
                targetSheet = sheetsData.get(0);
            }

            int hri = headerRowIndex != null ? headerRowIndex : 0;
            List<String> headers = new ArrayList<>();
            if (hri < targetSheet.allRows.size()) {
                Map<Integer, String> headerRow = targetSheet.allRows.get(hri);
                int maxCol = headerRow.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
                for (int c = 0; c <= maxCol; c++) {
                    headers.add(headerRow.getOrDefault(c, ""));
                }
            }

            String fingerprint = generateFingerprint(headers);

            List<ParseResultDTO.RowDataDTO> rows = new ArrayList<>();
            for (int i = hri + 1; i < targetSheet.allRows.size(); i++) {
                Map<Integer, String> row = targetSheet.allRows.get(i);
                if (isEmptyRow(row)) continue;
                ParseResultDTO.RowDataDTO rowData = new ParseResultDTO.RowDataDTO();
                rowData.setRowIndex(rows.size() + 1);
                OrderDTO dto = mapRowToOrder(row, headers, columnMapping);
                rowData.setData(dto);
                rowData.setErrors(new ArrayList<>());
                rows.add(rowData);
            }

            ParseResultDTO result = new ParseResultDTO();
            result.setRecognized(true);
            result.setTotalRows(rows.size());
            result.setHeaders(headers);
            result.setColumnMapping(columnMapping);
            result.setRows(rows);
            result.setFingerprint(fingerprint);
            result.setHeaderRowIndex(hri);
            result.setSheetName(targetSheet.sheetName);

            return result;
        } catch (Exception e) {
            throw new RuntimeException("解析Excel文件失败: " + e.getMessage(), e);
        }
    }

    private List<SheetData> readAllSheets(MultipartFile file) throws Exception {
        List<SheetData> result = new ArrayList<>();
        InputStream is = file.getInputStream();

        com.alibaba.excel.ExcelReader reader = EasyExcel.read(is).build();
        List<com.alibaba.excel.read.metadata.ReadSheet> sheets = reader.excelExecutor().sheetList();

        for (com.alibaba.excel.read.metadata.ReadSheet sheet : sheets) {
            MapListener listener = new MapListener();
            com.alibaba.excel.read.metadata.ReadSheet readSheet =
                EasyExcel.readSheet(sheet.getSheetNo())
                    .headRowNumber(0)
                    .registerReadListener(listener)
                    .build();
            reader.read(readSheet);
            SheetData sd = new SheetData();
            sd.sheetName = sheet.getSheetName();
            sd.allRows = listener.getRows();
            result.add(sd);
        }
        reader.finish();
        return result;
    }

    private DetectionResult detectTemplate(List<SheetData> sheetsData) {
        // Priority 1: Look for sheet named "订单数据"
        for (SheetData sd : sheetsData) {
            if ("订单数据".equals(sd.sheetName)) {
                DetectionResult dr = detectHeaderInSheet(sd);
                if (dr != null) return dr;
            }
        }

        // Priority 2: Skip instruction-only sheets, try data sheets
        for (SheetData sd : sheetsData) {
            if ("填写说明".equals(sd.sheetName)) continue;
            DetectionResult dr = detectHeaderInSheet(sd);
            if (dr != null) return dr;
        }

        // Priority 3: Try first sheet
        if (!sheetsData.isEmpty()) {
            return detectHeaderInSheet(sheetsData.get(0));
        }

        return null;
    }

    private DetectionResult detectHeaderInSheet(SheetData sd) {
        if (sd.allRows.isEmpty()) return null;

        int bestRow = -1;
        int bestScore = 0;
        List<String> bestHeaders = null;

        int scanLimit = Math.min(5, sd.allRows.size());
        for (int r = 0; r < scanLimit; r++) {
            Map<Integer, String> row = sd.allRows.get(r);
            List<String> headers = rowToHeaderList(row);
            int score = countAliasMatches(headers);
            if (score > bestScore) {
                bestScore = score;
                bestRow = r;
                bestHeaders = headers;
            }
        }

        if (bestScore < 3) return null; // Need at least 3 field matches

        DetectionResult dr = new DetectionResult();
        dr.sheetName = sd.sheetName;
        dr.headerRowIndex = bestRow;
        dr.headers = bestHeaders;
        dr.columnMapping = buildMappingFromAliases(bestHeaders);

        // Collect data rows (skip empty rows)
        dr.dataRows = new ArrayList<>();
        for (int i = bestRow + 1; i < sd.allRows.size(); i++) {
            Map<Integer, String> row = sd.allRows.get(i);
            if (isEmptyRow(row)) continue;
            dr.dataRows.add(row);
        }

        return dr;
    }

    private List<String> rowToHeaderList(Map<Integer, String> row) {
        if (row.isEmpty()) return Collections.emptyList();
        int maxCol = row.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        List<String> headers = new ArrayList<>();
        for (int c = 0; c <= maxCol; c++) {
            String val = row.getOrDefault(c, "");
            headers.add(val != null ? val.trim() : "");
        }
        return headers;
    }

    private int countAliasMatches(List<String> headers) {
        int count = 0;
        for (String header : headers) {
            if (header == null || header.isEmpty()) continue;
            for (Map.Entry<String, List<String>> entry : FIELD_ALIASES.entrySet()) {
                if (entry.getValue().contains(header)) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private Map<String, String> buildMappingFromAliases(List<String> headers) {
        Map<String, String> mapping = new LinkedHashMap<>();
        for (String header : headers) {
            if (header == null || header.isEmpty()) continue;
            for (Map.Entry<String, List<String>> entry : FIELD_ALIASES.entrySet()) {
                if (entry.getValue().contains(header)) {
                    mapping.put(header, entry.getKey());
                    break;
                }
            }
        }
        return mapping;
    }

    private int countMappedFields(Map<String, String> mapping) {
        if (mapping == null) return 0;
        Set<String> mappedFields = new HashSet<>(mapping.values());
        // Count required fields that are mapped
        List<String> requiredFields = Arrays.asList(
            "senderName", "senderPhone", "senderAddress",
            "receiverName", "receiverPhone", "receiverAddress",
            "weight", "quantity", "tempZone"
        );
        return (int) requiredFields.stream().filter(mappedFields::contains).count();
    }

    private OrderDTO mapRowToOrder(Map<Integer, String> row, List<String> headers,
                                    Map<String, String> columnMapping) {
        OrderDTO dto = new OrderDTO();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            String fieldName = columnMapping.get(header);
            if (fieldName == null) continue;
            String value = row.getOrDefault(i, "");
            if (value == null) value = "";
            setDtoField(dto, fieldName, value.trim());
        }
        return dto;
    }

    private void setDtoField(OrderDTO dto, String fieldName, String value) {
        switch (fieldName) {
            case "externalCode": dto.setExternalCode(value); break;
            case "senderName": dto.setSenderName(value); break;
            case "senderPhone": dto.setSenderPhone(value); break;
            case "senderAddress": dto.setSenderAddress(value); break;
            case "receiverName": dto.setReceiverName(value); break;
            case "receiverPhone": dto.setReceiverPhone(value); break;
            case "receiverAddress": dto.setReceiverAddress(value); break;
            case "weight": dto.setWeight(value); break;
            case "quantity": dto.setQuantity(value); break;
            case "tempZone": dto.setTempZone(value); break;
            case "remark": dto.setRemark(value); break;
        }
    }

    private boolean isEmptyRow(Map<Integer, String> row) {
        return row.values().stream().allMatch(v -> v == null || v.trim().isEmpty());
    }

    public String generateFingerprint(List<String> headers) {
        List<String> sorted = headers.stream()
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .sorted()
            .collect(Collectors.toList());
        String joined = String.join("|", sorted);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(joined.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(joined.hashCode());
        }
    }

    private Map<String, String> parseJsonMapping(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isEmpty()) return result;
        // Simple JSON parsing without external library
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split(":");
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String val = kv[1].trim().replace("\"", "");
                result.put(key, val);
            }
        }
        return result;
    }

    // Inner classes
    private static class SheetData {
        String sheetName;
        List<Map<Integer, String>> allRows = new ArrayList<>();
    }

    private static class DetectionResult {
        String sheetName;
        int headerRowIndex;
        List<String> headers;
        Map<String, String> columnMapping;
        List<Map<Integer, String>> dataRows;
    }

    private static class MapListener extends AnalysisEventListener<Map<Integer, String>> {
        private final List<Map<Integer, String>> rows = new ArrayList<>();

        @Override
        public void invoke(Map<Integer, String> data, AnalysisContext context) {
            rows.add(new LinkedHashMap<>(data));
        }

        @Override
        public void doAfterAllAnalysed(AnalysisContext context) {}

        public List<Map<Integer, String>> getRows() { return rows; }
    }
}
