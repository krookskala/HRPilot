package com.hrpilot.backend.employee.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeResponse(
    Long id,
    String email,
    String firstName,
    String lastName,
    String position,
    BigDecimal salary,
    LocalDate hireDate,
    String photoUrl
) {}