package com.hrpilot.backend.employee.dto;

import jakarta.validation.constraints.Size;

public record UpdatePersonalInfoRequest(
    @Size(min = 2, max = 50) String firstName,
    @Size(min = 2, max = 50) String lastName,
    @Size(max = 30) String phone,
    @Size(max = 500) String address,
    @Size(max = 100) String emergencyContactName,
    @Size(max = 30) String emergencyContactPhone
) {}
