package com.hanpeng.excel.dto;

import lombok.Data;

@Data
public class OrderDTO {
    private String externalCode;
    private String senderName;
    private String senderPhone;
    private String senderAddress;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private String weight;
    private String quantity;
    private String tempZone;
    private String remark;
}
