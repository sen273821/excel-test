package com.hanpeng.excel.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "template_mapping_rule")
public class TemplateMappingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String fingerprint;

    @Column(name = "header_row_index")
    private Integer headerRowIndex;

    @Column(name = "sheet_name", length = 100)
    private String sheetName;

    @Column(name = "column_mapping", nullable = false, columnDefinition = "TEXT")
    private String columnMapping;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createTime == null) createTime = now;
        if (updateTime == null) updateTime = now;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
