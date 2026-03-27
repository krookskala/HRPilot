package com.hrpilot.backend.leave.dto;

import com.hrpilot.backend.leave.LeaveStatus;
import com.hrpilot.backend.leave.LeaveType;
import java.time.LocalDate;

public record LeaveRequestResponse(
    Long id,
    Long employeeId,
    String employeeFullName,
    LeaveType type,
    LocalDate startDate,
    LocalDate endDate,
    LeaveStatus status,
    String reason
) {}