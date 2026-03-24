package com.hrpilot.backend.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthRequest(
    @NotBlank
    String email,

    @NotBlank
    String password
){}