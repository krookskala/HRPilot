package com.hrpilot.backend.payroll.dto;

import com.hrpilot.backend.payroll.PayrollComponentType;

import java.math.BigDecimal;

public record PayrollComponentResponse(
    Long id,
    PayrollComponentType componentType,
    String code,
    String label,
    BigDecimal amount
) {
}
