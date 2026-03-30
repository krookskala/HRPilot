package com.hrpilot.backend.leave;

import com.hrpilot.backend.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "leave_policies")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeavePolicy extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, unique = true)
    private LeaveType leaveType;

    @Column(name = "annual_days", nullable = false)
    private int annualDays;

    @Column(name = "carryover_enabled", nullable = false)
    private boolean carryoverEnabled;

    @Column(name = "carryover_max_days", nullable = false)
    private int carryoverMaxDays;

    @Column(name = "expires_month")
    private Integer expiresMonth;

    @Column(name = "expires_day")
    private Integer expiresDay;
}
