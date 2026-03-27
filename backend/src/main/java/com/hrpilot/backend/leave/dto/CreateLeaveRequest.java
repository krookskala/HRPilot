package com.hrpilot.backend.leave.dto;

import com.hrpilot.backend.leave.LeaveType;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CreateLeaveRequest(
    @NotNull
    Long employeeId,

    @NotNull
    LeaveType type,

    @NotNull
    LocalDate startDate,

    @NotNull
    LocalDate endDate,

    String reason
) {}