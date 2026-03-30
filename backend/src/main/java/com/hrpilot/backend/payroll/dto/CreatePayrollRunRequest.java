package com.hrpilot.backend.payroll.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreatePayrollRunRequest(
    @NotBlank
    String name,
    @NotNull
    Integer year,
    @NotNull
    Integer month,
    List<Long> employeeIds,
    Boolean includeAllEmployees,
    BigDecimal bonus,
    BigDecimal additionalDeduction,
    String taxClass
) {
}
