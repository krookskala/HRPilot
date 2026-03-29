package com.hrpilot.backend.employee.dto;

import java.time.LocalDateTime;

public record EmploymentHistoryResponse(
    Long id,
    String changeType,
    String oldValue,
    String newValue,
    LocalDateTime changedAt
) {}
