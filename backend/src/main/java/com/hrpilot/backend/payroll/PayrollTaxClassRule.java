package com.hrpilot.backend.payroll;

import com.hrpilot.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "payroll_tax_class_rules")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayrollTaxClassRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rule_version_id", nullable = false)
    private PayrollRuleVersion ruleVersion;

    @Column(name = "tax_class", nullable = false)
    private String taxClass;

    @Column(name = "monthly_allowance", nullable = false)
    private BigDecimal monthlyAllowance;

    @Column(name = "tax_multiplier", nullable = false)
    private BigDecimal taxMultiplier;
}
