package com.hrpilot.backend.leave;

import com.hrpilot.backend.common.BaseEntity;
import com.hrpilot.backend.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "leave_request_history")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaveRequestHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "leave_request_id", nullable = false)
    private LeaveRequest leaveRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actorUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveActionType actionType;

    @Enumerated(EnumType.STRING)
    private LeaveStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus toStatus;

    private String note;

    @Column(nullable = false)
    private LocalDateTime occurredAt;
}
