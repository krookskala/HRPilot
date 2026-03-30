package com.hrpilot.backend.payroll;

import com.hrpilot.backend.common.BaseEntity;
import com.hrpilot.backend.employee.Employee;
import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_records")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollRecord extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "run_id")
    private PayrollRun run;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private BigDecimal baseSalary;

    @Column(name = "gross_salary", nullable = false)
    private BigDecimal grossSalary;

    @Column(nullable = false)
    private BigDecimal bonus;

    @Column(nullable = false)
    private BigDecimal deductions;

    @Column(name = "employee_social_contributions", nullable = false)
    private BigDecimal employeeSocialContributions;

    @Column(name = "employer_social_contributions", nullable = false)
    private BigDecimal employerSocialContributions;

    @Column(name = "income_tax", nullable = false)
    private BigDecimal incomeTax;

    @Column(name = "tax_class", nullable = false)
    private String taxClass;

    @Column(nullable = false)
    private BigDecimal netSalary;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PayrollStatus status = PayrollStatus.DRAFT;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "payslip_storage_key")
    private String payslipStorageKey;

    @Column(name = "payslip_filename")
    private String payslipFilename;
}
