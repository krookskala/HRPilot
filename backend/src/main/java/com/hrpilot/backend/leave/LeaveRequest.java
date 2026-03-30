package com.hrpilot.backend.leave;

import com.hrpilot.backend.common.BaseEntity;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType type;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(name = "working_days", nullable = false)
    private Integer workingDays;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LeaveStatus status = LeaveStatus.PENDING;

    private String reason;

    @ManyToOne
    @JoinColumn(name = "approved_by_user_id")
    private User approvedByUser;

    @ManyToOne
    @JoinColumn(name = "rejected_by_user_id")
    private User rejectedByUser;

    @ManyToOne
    @JoinColumn(name = "cancelled_by_user_id")
    private User cancelledByUser;

    @Column(name = "actioned_at")
    private LocalDateTime actionedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "cancellation_reason")
    private String cancellationReason;
}
