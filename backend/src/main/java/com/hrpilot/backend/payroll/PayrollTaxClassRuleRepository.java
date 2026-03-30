package com.hrpilot.backend.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollTaxClassRuleRepository extends JpaRepository<PayrollTaxClassRule, Long> {
    Optional<PayrollTaxClassRule> findByRuleVersionIdAndTaxClass(Long ruleVersionId, String taxClass);
}
