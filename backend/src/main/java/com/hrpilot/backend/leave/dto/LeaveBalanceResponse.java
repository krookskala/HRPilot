package com.hrpilot.backend.leave.dto;

import com.hrpilot.backend.leave.LeaveType;

public record LeaveBalanceResponse(
    Long id,
    Long employeeId,
    LeaveType leaveType,
    int year,
    int totalDays,
    int usedDays,
    int remainingDays
) {}
