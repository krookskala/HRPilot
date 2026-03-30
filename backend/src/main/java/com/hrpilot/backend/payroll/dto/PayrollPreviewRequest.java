package com.hrpilot.backend.payroll.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PayrollPreviewRequest(
    @NotNull
    Long employeeId,
    @NotNull
    Integer year,
    @NotNull
    Integer month,
    BigDecimal bonus,
    BigDecimal additionalDeduction,
    String taxClass
) {
}
