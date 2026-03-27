package com.hrpilot.backend.payroll.dto;

import jakarta.validation.constraints.NotNull;
import com.hrpilot.backend.payroll.PayrollStatus;
import java.math.BigDecimal;

public record PayrollResponse(
    @NotNull
    Long id,

    @NotNull
    Long employeeId,

    @NotNull
    String employeeFullName,

    @NotNull
    int year,

    @NotNull
    int month,

    @NotNull
    BigDecimal baseSalary,

    @NotNull
    BigDecimal bonus,

    @NotNull
    BigDecimal deductions,

    @NotNull
    BigDecimal netSalary,

    @NotNull
    PayrollStatus status
) {}