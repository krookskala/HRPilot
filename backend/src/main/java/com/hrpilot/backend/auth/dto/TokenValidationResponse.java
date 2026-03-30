package com.hrpilot.backend.auth.dto;

import java.time.Instant;

public record TokenValidationResponse(
    String email,
    Instant expiresAt
) {}
