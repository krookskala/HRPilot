package com.hrpilot.backend.payroll;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayrollComponentRepository extends JpaRepository<PayrollComponent, Long> {
    List<PayrollComponent> findByPayrollRecordIdOrderByIdAsc(Long payrollRecordId);
    void deleteByPayrollRecordId(Long payrollRecordId);
    void deleteByPayrollRecordEmployeeId(Long employeeId);
}
