package com.hrpilot.backend.auth.dto;

public record TokenRefreshResponse(
    String accessToken,
    String refreshToken
) {}
