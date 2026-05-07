package com.hanpeng.excel.service.impl;

import com.hanpeng.excel.dto.OrderDTO;
import com.hanpeng.excel.dto.ParseResultDTO;
import com.hanpeng.excel.service.ValidationService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;

@Service
public class ValidationServiceImpl implements ValidationService {

    private static final Pattern MOBILE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern LANDLINE_PATTERN = Pattern.compile("^0\\d{2,3}-?\\d{7,8}$");
    private static final Set<String> VALID_TEMP_ZONES = new HashSet<>(Arrays.asList("常温", "冷藏", "冷冻"));

    @Override
    public List<ParseResultDTO.FieldErrorDTO> validateOrder(OrderDTO order) {
        List<ParseResultDTO.FieldErrorDTO> errors = new ArrayList<>();

        // Required field checks
        if (isEmpty(order.getSenderName())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("senderName", "发件人姓名", "不能为空"));
        }
        if (isEmpty(order.getSenderPhone())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("senderPhone", "发件人电话", "不能为空"));
        } else if (!isValidPhone(order.getSenderPhone())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("senderPhone", "发件人电话", "格式错误"));
        }
        if (isEmpty(order.getSenderAddress())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("senderAddress", "发件人地址", "不能为空"));
        }
        if (isEmpty(order.getReceiverName())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("receiverName", "收件人姓名", "不能为空"));
        }
        if (isEmpty(order.getReceiverPhone())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("receiverPhone", "收件人电话", "不能为空"));
        } else if (!isValidPhone(order.getReceiverPhone())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("receiverPhone", "收件人电话", "格式错误"));
        }
        if (isEmpty(order.getReceiverAddress())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("receiverAddress", "收件人地址", "不能为空"));
        }

        // Weight validation
        if (isEmpty(order.getWeight())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("weight", "重量(kg)", "不能为空"));
        } else {
            try {
                double w = Double.parseDouble(order.getWeight());
                if (w <= 0) {
                    errors.add(new ParseResultDTO.FieldErrorDTO("weight", "重量(kg)", "必须为正数"));
                }
            } catch (NumberFormatException e) {
                errors.add(new ParseResultDTO.FieldErrorDTO("weight", "重量(kg)", "必须为数字"));
            }
        }

        // Quantity validation
        if (isEmpty(order.getQuantity())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("quantity", "件数", "不能为空"));
        } else {
            try {
                int q = Integer.parseInt(order.getQuantity());
                if (q <= 0) {
                    errors.add(new ParseResultDTO.FieldErrorDTO("quantity", "件数", "必须为正整数"));
                }
            } catch (NumberFormatException e) {
                // Try parsing as double first (e.g., "2.0")
                try {
                    double d = Double.parseDouble(order.getQuantity());
                    if (d <= 0 || d != Math.floor(d)) {
                        errors.add(new ParseResultDTO.FieldErrorDTO("quantity", "件数", "必须为正整数"));
                    }
                } catch (NumberFormatException e2) {
                    errors.add(new ParseResultDTO.FieldErrorDTO("quantity", "件数", "必须为正整数"));
                }
            }
        }

        // TempZone validation
        if (isEmpty(order.getTempZone())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("tempZone", "温层", "不能为空"));
        } else if (!VALID_TEMP_ZONES.contains(order.getTempZone().trim())) {
            errors.add(new ParseResultDTO.FieldErrorDTO("tempZone", "温层", "必须为: 常温/冷藏/冷冻"));
        }

        return errors;
    }

    @Override
    public List<ParseResultDTO.RowDataDTO> validateAll(List<ParseResultDTO.RowDataDTO> rows) {
        // Track external code duplicates within batch
        Map<String, Integer> codeFirstSeen = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            ParseResultDTO.RowDataDTO row = rows.get(i);
            List<ParseResultDTO.FieldErrorDTO> errors = validateOrder(row.getData());

            // Check batch-internal duplicate for externalCode
            String code = row.getData().getExternalCode();
            if (code != null && !code.trim().isEmpty()) {
                code = code.trim();
                if (codeFirstSeen.containsKey(code)) {
                    errors.add(new ParseResultDTO.FieldErrorDTO(
                        "externalCode", "外部编码",
                        "与第" + codeFirstSeen.get(code) + "行重复"));
                } else {
                    codeFirstSeen.put(code, row.getRowIndex());
                }
            }

            row.setErrors(errors);
        }

        return rows;
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidPhone(String phone) {
        if (phone == null) return false;
        phone = phone.trim();
        return MOBILE_PATTERN.matcher(phone).matches() || LANDLINE_PATTERN.matcher(phone).matches();
    }
}
