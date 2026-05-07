package com.hanpeng.excel.repository;

import com.hanpeng.excel.entity.TemplateMappingRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TemplateMappingRuleRepository extends JpaRepository<TemplateMappingRule, Long> {

    Optional<TemplateMappingRule> findByFingerprint(String fingerprint);
}
