package com.hrpilot.backend.auth.dto;

import java.time.Instant;

public record PasswordResetResponse(
    String message,
    String resetUrl,
    Instant expiresAt
) {}
