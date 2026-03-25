package com.hrpilot.backend.department.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateDepartmentRequest(
    @NotBlank
    String name,

    Long managerId,

    Long parentDepartmentId
) {}