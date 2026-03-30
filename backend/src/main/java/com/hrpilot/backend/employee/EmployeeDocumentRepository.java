package com.hrpilot.backend.employee;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {
    List<EmployeeDocument> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
    Optional<EmployeeDocument> findByIdAndEmployeeId(Long id, Long employeeId);
    void deleteByEmployeeId(Long employeeId);
}
