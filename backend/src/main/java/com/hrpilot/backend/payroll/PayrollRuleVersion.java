package com.hrpilot.backend.payroll;

import com.hrpilot.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_rule_versions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRuleVersion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "version_label", nullable = false, unique = true)
    private String versionLabel;

    @Column(name = "employee_pension_rate", nullable = false)
    private BigDecimal employeePensionRate;

    @Column(name = "employee_health_rate", nullable = false)
    private BigDecimal employeeHealthRate;

    @Column(name = "employee_unemployment_rate", nullable = false)
    private BigDecimal employeeUnemploymentRate;

    @Column(name = "employee_care_rate", nullable = false)
    private BigDecimal employeeCareRate;

    @Column(name = "employer_pension_rate", nullable = false)
    private BigDecimal employerPensionRate;

    @Column(name = "employer_health_rate", nullable = false)
    private BigDecimal employerHealthRate;

    @Column(name = "employer_unemployment_rate", nullable = false)
    private BigDecimal employerUnemploymentRate;

    @Column(name = "employer_care_rate", nullable = false)
    private BigDecimal employerCareRate;

    @Column(name = "income_tax_base_rate", nullable = false)
    private BigDecimal incomeTaxBaseRate;

    @Column(name = "solidarity_rate", nullable = false)
    private BigDecimal solidarityRate;

    @Column(name = "monthly_tax_free_allowance", nullable = false)
    private BigDecimal monthlyTaxFreeAllowance;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;
}
