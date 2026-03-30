package com.hrpilot.backend.user.dto;

import com.hrpilot.backend.user.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminInviteUserRequest(
    @NotBlank @Email String email,
    @NotNull Role role,
    @Size(max = 10) String preferredLang
) {}
