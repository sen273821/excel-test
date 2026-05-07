package com.hanpeng.excel.service;

import com.hanpeng.excel.dto.OrderDTO;
import com.hanpeng.excel.dto.SubmitResultDTO;
import com.hanpeng.excel.entity.LogisticsOrder;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderService {

    SubmitResultDTO submitOrders(String batchId, List<OrderDTO> orders);

    Page<LogisticsOrder> listOrders(String externalCode, String receiverName,
                                     LocalDateTime startTime, LocalDateTime endTime,
                                     int page, int size);

    List<String> checkDuplicateCodes(List<String> codes);
}
