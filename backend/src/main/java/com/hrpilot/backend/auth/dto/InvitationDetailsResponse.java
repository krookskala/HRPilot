package com.hrpilot.backend.auth.dto;

import com.hrpilot.backend.user.Role;

import java.time.Instant;

public record InvitationDetailsResponse(
    String email,
    Role role,
    String preferredLang,
    Instant expiresAt
) {}
