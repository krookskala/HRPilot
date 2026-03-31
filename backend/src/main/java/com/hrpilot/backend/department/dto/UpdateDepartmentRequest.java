package com.hrpilot.backend.department.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateDepartmentRequest(
    @NotBlank
    String name,

    Long managerId,

    Long parentDepartmentId
) {}
