package com.hrpilot.backend.department.dto;

public record DepartmentResponse(
    Long id,
    String name,
    String managerEmail,
    Long parentDepartmentId,
    String parentDepartmentName
) {}