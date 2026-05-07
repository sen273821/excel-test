package com.hanpeng.excel.controller;

import com.hanpeng.excel.dto.*;
import com.hanpeng.excel.entity.LogisticsOrder;
import com.hanpeng.excel.entity.TemplateMappingRule;
import com.hanpeng.excel.repository.TemplateMappingRuleRepository;
import com.hanpeng.excel.service.OrderService;
import com.hanpeng.excel.service.TemplateRecognitionService;
import com.hanpeng.excel.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private TemplateRecognitionService recognitionService;

    @Autowired
    private ValidationService validationService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private TemplateMappingRuleRepository mappingRuleRepository;

    @PostMapping("/parse")
    public ResultVO<ParseResultDTO> parseExcel(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResultVO.error("文件为空，请选择有效的Excel文件");
            }
            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResultVO.error("不支持的文件格式，请上传 .xlsx 或 .xls 文件");
            }

            ParseResultDTO result = recognitionService.parseExcel(file);

            // Validate all rows
            if (result.isRecognized()) {
                validationService.validateAll(result.getRows());
            }

            return ResultVO.success(result);
        } catch (Exception e) {
            return ResultVO.error("解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/parse-with-mapping")
    public ResultVO<ParseResultDTO> parseWithMapping(
            @RequestParam("file") MultipartFile file,
            @RequestParam("mapping") String mappingJson,
            @RequestParam(value = "headerRowIndex", required = false) Integer headerRowIndex,
            @RequestParam(value = "sheetName", required = false) String sheetName) {
        try {
            Map<String, String> mapping = parseJsonMapping(mappingJson);
            ParseResultDTO result = recognitionService.parseExcelWithMapping(
                file, mapping, headerRowIndex, sheetName);

            // Validate all rows
            validationService.validateAll(result.getRows());

            return ResultVO.success(result);
        } catch (Exception e) {
            return ResultVO.error("解析失败: " + e.getMessage());
        }
    }

    @PostMapping("/submit")
    public ResultVO<SubmitResultDTO> submitOrders(@RequestBody SubmitRequestDTO request) {
        try {
            if (request.getOrders() == null || request.getOrders().isEmpty()) {
                return ResultVO.error("提交数据为空");
            }

            String batchId = request.getBatchId();
            if (batchId == null || batchId.isEmpty()) {
                batchId = UUID.randomUUID().toString().substring(0, 8);
            }

            SubmitResultDTO result = orderService.submitOrders(batchId, request.getOrders());

            // Save template mapping if provided
            if (request.getFingerprint() != null && request.getColumnMapping() != null) {
                saveMapping(request.getFingerprint(), request.getColumnMapping());
            }

            return ResultVO.success(result);
        } catch (Exception e) {
            return ResultVO.error("提交失败: " + e.getMessage());
        }
    }

    @GetMapping("/list")
    public ResultVO<Page<LogisticsOrder>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String externalCode,
            @RequestParam(required = false) String receiverName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        Page<LogisticsOrder> result = orderService.listOrders(
            externalCode, receiverName, startTime, endTime, page, size);
        return ResultVO.success(result);
    }

    @GetMapping("/check-duplicate")
    public ResultVO<List<String>> checkDuplicate(@RequestParam String codes) {
        List<String> codeList = Arrays.asList(codes.split(","));
        List<String> duplicates = orderService.checkDuplicateCodes(codeList);
        return ResultVO.success(duplicates);
    }

    // Template mapping endpoints
    @GetMapping("/mapping/match")
    public ResultVO<Map<String, String>> getMapping(@RequestParam String fingerprint) {
        Optional<TemplateMappingRule> rule = mappingRuleRepository.findByFingerprint(fingerprint);
        if (rule.isPresent()) {
            Map<String, String> mapping = parseJsonMapping(rule.get().getColumnMapping());
            return ResultVO.success(mapping);
        }
        return ResultVO.success(null);
    }

    @PostMapping("/mapping/save")
    public ResultVO<String> saveMapping(@RequestBody Map<String, Object> body) {
        String fingerprint = (String) body.get("fingerprint");
        @SuppressWarnings("unchecked")
        Map<String, String> mapping = (Map<String, String>) body.get("columnMapping");
        Integer headerRowIndex = body.get("headerRowIndex") != null ?
            Integer.parseInt(body.get("headerRowIndex").toString()) : null;
        String sheetName = (String) body.get("sheetName");

        if (fingerprint == null || mapping == null) {
            return ResultVO.error("参数不完整");
        }

        saveMapping(fingerprint, mapping, headerRowIndex, sheetName);
        return ResultVO.success("保存成功");
    }

    private void saveMapping(String fingerprint, Map<String, String> mapping) {
        saveMapping(fingerprint, mapping, null, null);
    }

    private void saveMapping(String fingerprint, Map<String, String> mapping,
                              Integer headerRowIndex, String sheetName) {
        Optional<TemplateMappingRule> existing = mappingRuleRepository.findByFingerprint(fingerprint);
        TemplateMappingRule rule;
        if (existing.isPresent()) {
            rule = existing.get();
        } else {
            rule = new TemplateMappingRule();
            rule.setFingerprint(fingerprint);
        }
        rule.setColumnMapping(toJsonString(mapping));
        if (headerRowIndex != null) rule.setHeaderRowIndex(headerRowIndex);
        if (sheetName != null) rule.setSheetName(sheetName);
        mappingRuleRepository.save(rule);
    }

    private String toJsonString(Map<String, String> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (!first) sb.append(",");
            sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        sb.append("}");
        return sb.toString();
    }

    private Map<String, String> parseJsonMapping(String json) {
        Map<String, String> result = new LinkedHashMap<>();
        if (json == null || json.isEmpty()) return result;
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
        String[] pairs = json.split(",");
        for (String pair : pairs) {
            int colonIdx = pair.indexOf(":");
            if (colonIdx > 0) {
                String key = pair.substring(0, colonIdx).trim().replace("\"", "");
                String val = pair.substring(colonIdx + 1).trim().replace("\"", "");
                result.put(key, val);
            }
        }
        return result;
    }
}
