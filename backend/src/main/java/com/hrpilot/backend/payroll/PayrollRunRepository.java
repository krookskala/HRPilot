package com.hrpilot.backend.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    boolean existsByNameAndYearAndMonth(String name, int year, int month);
}
