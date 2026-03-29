package com.hrpilot.backend.employee.dto;

import java.math.BigDecimal;

public record UpdateEmployeeRequest(
    String firstName,
    String lastName,
    String position,
    BigDecimal salary,
    Long departmentId
) {}
