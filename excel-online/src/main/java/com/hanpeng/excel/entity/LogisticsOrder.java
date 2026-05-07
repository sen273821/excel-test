package com.hanpeng.excel.entity;

import lombok.Data;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "logistics_order", indexes = {
    @Index(name = "idx_external_code", columnList = "external_code"),
    @Index(name = "idx_receiver_name", columnList = "receiver_name"),
    @Index(name = "idx_batch_id", columnList = "batch_id"),
    @Index(name = "idx_create_time", columnList = "create_time")
})
public class LogisticsOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_code", length = 100)
    private String externalCode;

    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    @Column(name = "sender_phone", nullable = false, length = 30)
    private String senderPhone;

    @Column(name = "sender_address", nullable = false, length = 500)
    private String senderAddress;

    @Column(name = "receiver_name", nullable = false, length = 100)
    private String receiverName;

    @Column(name = "receiver_phone", nullable = false, length = 30)
    private String receiverPhone;

    @Column(name = "receiver_address", nullable = false, length = 500)
    private String receiverAddress;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal weight;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "temp_zone", nullable = false, length = 20)
    private String tempZone;

    @Column(length = 500)
    private String remark;

    @Column(name = "batch_id", length = 50)
    private String batchId;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = LocalDateTime.now();
        }
    }
}
