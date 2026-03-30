package com.hrpilot.backend.payroll;

import java.math.BigDecimal;
import java.util.List;

public record PayrollCalculationResult(
    BigDecimal monthlyBaseSalary,
    BigDecimal grossSalary,
    BigDecimal employeeSocialContributions,
    BigDecimal employerSocialContributions,
    BigDecimal incomeTax,
    BigDecimal totalDeductions,
    BigDecimal netSalary,
    String taxClass,
    List<PayrollComponentDraft> components
) {
    public record PayrollComponentDraft(
        PayrollComponentType componentType,
        String code,
        String label,
        BigDecimal amount
    ) {
    }
}
