package com.hrpilot.backend.payroll.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePayrollRequest(
    @NotNull
    Long employeeId,

    @NotNull
    int year,
    
    @NotNull
    int month,

    @NotNull
    BigDecimal baseSalary,

    @NotNull
    BigDecimal bonus,

    @NotNull
    BigDecimal deductions
) {}