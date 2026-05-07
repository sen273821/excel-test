package com.hanpeng.excel.repository;

import com.hanpeng.excel.entity.LogisticsOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogisticsOrderRepository extends JpaRepository<LogisticsOrder, Long> {

    List<LogisticsOrder> findByExternalCodeIn(List<String> externalCodes);

    @Query("SELECT o FROM LogisticsOrder o WHERE " +
           "(:externalCode IS NULL OR o.externalCode LIKE %:externalCode%) AND " +
           "(:receiverName IS NULL OR o.receiverName LIKE %:receiverName%) AND " +
           "(:startTime IS NULL OR o.createTime >= :startTime) AND " +
           "(:endTime IS NULL OR o.createTime <= :endTime)")
    Page<LogisticsOrder> findByFilters(
        @Param("externalCode") String externalCode,
        @Param("receiverName") String receiverName,
        @Param("startTime") LocalDateTime startTime,
        @Param("endTime") LocalDateTime endTime,
        Pageable pageable
    );
}
