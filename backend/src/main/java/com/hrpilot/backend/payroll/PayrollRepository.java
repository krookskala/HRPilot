package com.hrpilot.backend.payroll;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface PayrollRepository extends JpaRepository<PayrollRecord, Long> {
    List<PayrollRecord> findByEmployeeId(Long employeeId);
    long countByStatus(PayrollStatus status);

    @Query("SELECT COALESCE(SUM(p.netSalary), 0) FROM PayrollRecord p WHERE p.status = 'PAID'")
    BigDecimal sumPaidNetSalary();
}