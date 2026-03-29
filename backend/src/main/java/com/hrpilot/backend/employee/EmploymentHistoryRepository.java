package com.hrpilot.backend.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmploymentHistoryRepository extends JpaRepository<EmploymentHistory, Long> {
    List<EmploymentHistory> findByEmployeeIdOrderByChangedAtDesc(Long employeeId);
}
