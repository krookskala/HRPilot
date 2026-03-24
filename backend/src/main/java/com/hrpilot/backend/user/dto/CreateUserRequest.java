package com.hrpilot.backend.user.dto;

import com.hrpilot.backend.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateUserRequest(
    @NotBlank
    @Email
    String email,

    @NotBlank
    String password,

    @NotNull
    Role role
){}