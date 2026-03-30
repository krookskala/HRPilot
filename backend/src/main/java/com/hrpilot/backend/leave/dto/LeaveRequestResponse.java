package com.hrpilot.backend.leave.dto;

import com.hrpilot.backend.leave.LeaveStatus;
import com.hrpilot.backend.leave.LeaveType;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LeaveRequestResponse(
    Long id,
    Long employeeId,
    String employeeFullName,
    LeaveType type,
    LocalDate startDate,
    LocalDate endDate,
    Integer workingDays,
    LeaveStatus status,
    String reason,
    Long approvedByUserId,
    String approvedByUserEmail,
    Long rejectedByUserId,
    String rejectedByUserEmail,
    Long cancelledByUserId,
    String cancelledByUserEmail,
    LocalDateTime actionedAt,
    LocalDateTime cancelledAt,
    String rejectionReason,
    String cancellationReason,
    LocalDateTime createdAt
) {}
