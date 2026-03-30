package com.hrpilot.backend.leave.dto;

import jakarta.validation.constraints.NotBlank;

public record LeaveActionRequest(
    @NotBlank
    String reason
) {
}
