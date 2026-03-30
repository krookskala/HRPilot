package com.hrpilot.backend.leave.dto;

import com.hrpilot.backend.leave.LeaveActionType;
import com.hrpilot.backend.leave.LeaveStatus;

import java.time.LocalDateTime;

public record LeaveRequestHistoryResponse(
    Long id,
    LeaveActionType actionType,
    LeaveStatus fromStatus,
    LeaveStatus toStatus,
    Long actorUserId,
    String actorUserEmail,
    String note,
    LocalDateTime occurredAt
) {
}
