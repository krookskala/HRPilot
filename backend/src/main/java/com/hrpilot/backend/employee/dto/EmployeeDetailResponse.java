package com.hrpilot.backend.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EmployeeDetailResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String position,
    BigDecimal salary,
    LocalDate hireDate,
    String photoUrl,
    Long departmentId,
    String departmentName,
    List<EmploymentHistoryResponse> employmentHistory,
    List<EmployeeDocumentResponse> documents
) {
}
