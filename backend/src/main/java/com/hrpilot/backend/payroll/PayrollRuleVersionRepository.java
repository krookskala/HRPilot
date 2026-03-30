package com.hrpilot.backend.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollRuleVersionRepository extends JpaRepository<PayrollRuleVersion, Long> {
    Optional<PayrollRuleVersion> findByIsActiveTrue();
}
