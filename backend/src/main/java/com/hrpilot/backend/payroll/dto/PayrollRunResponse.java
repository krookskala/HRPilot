package com.hrpilot.backend.payroll.dto;

import com.hrpilot.backend.payroll.PayrollRunStatus;

import java.time.LocalDateTime;

public record PayrollRunResponse(
    Long id,
    String name,
    int year,
    int month,
    PayrollRunStatus status,
    int payrollCount,
    LocalDateTime createdAt,
    LocalDateTime publishedAt,
    LocalDateTime paidAt
) {
}
