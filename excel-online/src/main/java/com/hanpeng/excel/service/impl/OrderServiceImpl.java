package com.hanpeng.excel.service.impl;

import com.hanpeng.excel.dto.OrderDTO;
import com.hanpeng.excel.dto.SubmitResultDTO;
import com.hanpeng.excel.entity.LogisticsOrder;
import com.hanpeng.excel.repository.LogisticsOrderRepository;
import com.hanpeng.excel.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private LogisticsOrderRepository orderRepository;

    @Override
    @Transactional
    public SubmitResultDTO submitOrders(String batchId, List<OrderDTO> orders) {
        SubmitResultDTO result = new SubmitResultDTO();
        result.setTotal(orders.size());
        int success = 0;
        List<SubmitResultDTO.FailedRow> failedRows = new ArrayList<>();

        List<LogisticsOrder> toSave = new ArrayList<>();

        for (int i = 0; i < orders.size(); i++) {
            OrderDTO dto = orders.get(i);
            try {
                LogisticsOrder order = new LogisticsOrder();
                order.setExternalCode(dto.getExternalCode());
                order.setSenderName(dto.getSenderName());
                order.setSenderPhone(dto.getSenderPhone());
                order.setSenderAddress(dto.getSenderAddress());
                order.setReceiverName(dto.getReceiverName());
                order.setReceiverPhone(dto.getReceiverPhone());
                order.setReceiverAddress(dto.getReceiverAddress());

                // Parse weight
                String weightStr = dto.getWeight();
                if (weightStr != null && !weightStr.isEmpty()) {
                    order.setWeight(new BigDecimal(weightStr.trim()));
                } else {
                    order.setWeight(BigDecimal.ZERO);
                }

                // Parse quantity
                String qtyStr = dto.getQuantity();
                if (qtyStr != null && !qtyStr.isEmpty()) {
                    double d = Double.parseDouble(qtyStr.trim());
                    order.setQuantity((int) d);
                } else {
                    order.setQuantity(0);
                }

                order.setTempZone(dto.getTempZone());
                order.setRemark(dto.getRemark());
                order.setBatchId(batchId);

                toSave.add(order);
                success++;
            } catch (Exception e) {
                failedRows.add(new SubmitResultDTO.FailedRow(i + 1, e.getMessage()));
            }
        }

        // Batch save
        if (!toSave.isEmpty()) {
            orderRepository.saveAll(toSave);
        }

        result.setSuccess(success);
        result.setFailed(failedRows.size());
        result.setFailedRows(failedRows);
        return result;
    }

    @Override
    public Page<LogisticsOrder> listOrders(String externalCode, String receiverName,
                                            LocalDateTime startTime, LocalDateTime endTime,
                                            int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        return orderRepository.findByFilters(
            externalCode != null && !externalCode.isEmpty() ? externalCode : null,
            receiverName != null && !receiverName.isEmpty() ? receiverName : null,
            startTime,
            endTime,
            pageRequest
        );
    }

    @Override
    public List<String> checkDuplicateCodes(List<String> codes) {
        if (codes == null || codes.isEmpty()) return new ArrayList<>();
        List<String> validCodes = codes.stream()
            .filter(c -> c != null && !c.trim().isEmpty())
            .collect(Collectors.toList());
        if (validCodes.isEmpty()) return new ArrayList<>();

        return orderRepository.findByExternalCodeIn(validCodes).stream()
            .map(LogisticsOrder::getExternalCode)
            .distinct()
            .collect(Collectors.toList());
    }
}
