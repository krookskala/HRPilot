package com.hrpilot.backend.user.dto;

import java.time.Instant;

public record UserInvitationResponse(
    UserResponse user,
    String inviteToken,
    String inviteUrl,
    Instant expiresAt
) {}
