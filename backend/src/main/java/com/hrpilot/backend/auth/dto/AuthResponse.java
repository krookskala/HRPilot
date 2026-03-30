package com.hrpilot.backend.auth.dto;

import com.hrpilot.backend.user.dto.CurrentUserResponse;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    CurrentUserResponse user
) {}
