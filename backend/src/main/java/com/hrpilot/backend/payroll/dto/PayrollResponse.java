package com.hrpilot.backend.payroll.dto;

import jakarta.validation.constraints.NotNull;
import com.hrpilot.backend.payroll.PayrollStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    BigDecimal grossSalary,

    @NotNull
    BigDecimal bonus,

    @NotNull
    BigDecimal deductions,

    @NotNull
    BigDecimal employeeSocialContributions,

    @NotNull
    BigDecimal employerSocialContributions,

    @NotNull
    BigDecimal incomeTax,

    @NotNull
    BigDecimal netSalary,

    @NotNull
    String taxClass,

    @NotNull
    PayrollStatus status,

    Long runId,
    LocalDateTime publishedAt,
    LocalDateTime paidAt,
    boolean hasPayslip,
    List<PayrollComponentResponse> components
) {}
