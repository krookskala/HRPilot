package com.hrpilot.backend.payroll;

import com.hrpilot.backend.common.exception.BusinessRuleException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.employee.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PayrollCalculatorService {

    private static final int SCALE = 2;

    private final PayrollRuleVersionRepository ruleVersionRepository;
    private final PayrollTaxClassRuleRepository taxClassRuleRepository;

    public PayrollCalculationResult calculate(Employee employee, BigDecimal bonus, BigDecimal additionalDeduction, String requestedTaxClass) {
        PayrollRuleVersion ruleVersion = ruleVersionRepository.findByIsActiveTrue()
            .orElseThrow(() -> new ResourceNotFoundException("PayrollRuleVersion", "active", true));

        String taxClass = requestedTaxClass != null && !requestedTaxClass.isBlank()
            ? requestedTaxClass.trim().toUpperCase()
            : "I";

        PayrollTaxClassRule taxClassRule = taxClassRuleRepository.findByRuleVersionIdAndTaxClass(ruleVersion.getId(), taxClass)
            .orElseThrow(() -> new BusinessRuleException("Unsupported tax class: " + taxClass));

        BigDecimal normalizedBonus = bonus != null ? bonus : BigDecimal.ZERO;
        BigDecimal normalizedAdditionalDeduction = additionalDeduction != null ? additionalDeduction : BigDecimal.ZERO;
        BigDecimal monthlyBaseSalary = employee.getSalary().divide(BigDecimal.valueOf(12), SCALE, RoundingMode.HALF_UP);
        BigDecimal grossSalary = monthlyBaseSalary.add(normalizedBonus).setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal employeeContributions = sum(
            percentage(grossSalary, ruleVersion.getEmployeePensionRate()),
            percentage(grossSalary, ruleVersion.getEmployeeHealthRate()),
            percentage(grossSalary, ruleVersion.getEmployeeUnemploymentRate()),
            percentage(grossSalary, ruleVersion.getEmployeeCareRate())
        );

        BigDecimal employerContributions = sum(
            percentage(grossSalary, ruleVersion.getEmployerPensionRate()),
            percentage(grossSalary, ruleVersion.getEmployerHealthRate()),
            percentage(grossSalary, ruleVersion.getEmployerUnemploymentRate()),
            percentage(grossSalary, ruleVersion.getEmployerCareRate())
        );

        BigDecimal taxableBase = grossSalary
            .subtract(employeeContributions)
            .subtract(ruleVersion.getMonthlyTaxFreeAllowance())
            .subtract(taxClassRule.getMonthlyAllowance())
            .max(BigDecimal.ZERO);

        BigDecimal incomeTax = percentage(
            taxableBase,
            ruleVersion.getIncomeTaxBaseRate().multiply(taxClassRule.getTaxMultiplier())
        );
        BigDecimal solidarity = percentage(incomeTax, ruleVersion.getSolidarityRate());

        BigDecimal totalDeductions = employeeContributions
            .add(incomeTax)
            .add(solidarity)
            .add(normalizedAdditionalDeduction)
            .setScale(SCALE, RoundingMode.HALF_UP);

        BigDecimal netSalary = grossSalary.subtract(totalDeductions).setScale(SCALE, RoundingMode.HALF_UP);

        List<PayrollCalculationResult.PayrollComponentDraft> components = new ArrayList<>();
        components.add(new PayrollCalculationResult.PayrollComponentDraft(PayrollComponentType.EARNING, "BASE_SALARY", "Base salary", monthlyBaseSalary));
        if (normalizedBonus.compareTo(BigDecimal.ZERO) > 0) {
            components.add(new PayrollCalculationResult.PayrollComponentDraft(PayrollComponentType.EARNING, "BONUS", "Bonus", normalizedBonus));
        }
        addContributionComponents(components, PayrollComponentType.EMPLOYEE_DEDUCTION, grossSalary, ruleVersion);
        components.add(new PayrollCalculationResult.PayrollComponentDraft(PayrollComponentType.TAX, "INCOME_TAX", "Income tax", incomeTax));
        components.add(new PayrollCalculationResult.PayrollComponentDraft(PayrollComponentType.TAX, "SOLIDARITY", "Solidarity surcharge", solidarity));
        if (normalizedAdditionalDeduction.compareTo(BigDecimal.ZERO) > 0) {
            components.add(new PayrollCalculationResult.PayrollComponentDraft(PayrollComponentType.EMPLOYEE_DEDUCTION, "ADDITIONAL_DEDUCTION", "Additional deduction", normalizedAdditionalDeduction));
        }
        addEmployerContributionComponents(components, grossSalary, ruleVersion);

        return new PayrollCalculationResult(
            monthlyBaseSalary,
            grossSalary,
            employeeContributions,
            employerContributions,
            incomeTax,
            totalDeductions,
            netSalary,
            taxClass,
            components
        );
    }

    private void addContributionComponents(List<PayrollCalculationResult.PayrollComponentDraft> components,
                                           PayrollComponentType type,
                                           BigDecimal grossSalary,
                                           PayrollRuleVersion ruleVersion) {
        components.add(new PayrollCalculationResult.PayrollComponentDraft(type, "EMP_PENSION", "Employee pension", percentage(grossSalary, ruleVersion.getEmployeePensionRate())));
        components.add(new PayrollCalculationResult.PayrollComponentDraft(type, "EMP_HEALTH", "Employee health insurance", percentage(grossSalary, ruleVersion.getEmployeeHealthRate())));
        components.add(new PayrollCalculationResult.PayrollComponentDraft(type, "EMP_UNEMPLOYMENT", "Employee unemployment insurance", percentage(grossSalary, ruleVersion.getEmployeeUnemploymentRate())));
        components.add(new PayrollCalculationResult.PayrollComponentDraft(type, "EMP_CARE", "Employee long-term care", percentage(grossSalary, ruleVersion.getEmployeeCareRate())));
    }

    private void addEmployerContributionComponents(List<PayrollCalculationResult.PayrollComponentDraft> components,
                                                   BigDecimal grossSalary,
                                                   PayrollRuleVersion ruleVersion) {
        components.add(new PayrollCalculationResult.PayrollComponentDraft(PayrollComponentType.EMPLOYER_CONTRIBUTION, "ER_PENSION", "Employer pension", percentage(grossSalary, ruleVersion.getEmployerPensionRate())));
        components.add(new PayrollCalculationResult.PayrollComponentDraft(PayrollComponentType.EMPLOYER_CONTRIBUTION, "ER_HEALTH", "Employer health insurance", percentage(grossSalary, ruleVersion.getEmployerHealthRate())));
        components.add(new PayrollCalculationResult.PayrollComponentDraft(PayrollComponentType.EMPLOYER_CONTRIBUTION, "ER_UNEMPLOYMENT", "Employer unemployment insurance", percentage(grossSalary, ruleVersion.getEmployerUnemploymentRate())));
        components.add(new PayrollCalculationResult.PayrollComponentDraft(PayrollComponentType.EMPLOYER_CONTRIBUTION, "ER_CARE", "Employer long-term care", percentage(grossSalary, ruleVersion.getEmployerCareRate())));
    }

    private BigDecimal percentage(BigDecimal base, BigDecimal rate) {
        return base.multiply(rate).setScale(SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal result = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            result = result.add(value);
        }
        return result.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
