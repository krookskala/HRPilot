package com.hrpilot.backend.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateEmployeeRequest(
    @NotNull
    Long userId,

    @NotBlank
    String firstName,

    @NotBlank
    String lastName,

    @NotBlank
    String position,

    @NotNull
    BigDecimal salary,

    @NotNull
    LocalDate hireDate
) {}