package com.hrpilot.backend.user.dto;

import com.hrpilot.backend.user.Role;

import java.time.LocalDateTime;

public record UserResponse(
    Long id,
    String email,
    Role role,
    boolean isActive,
    String preferredLang,
    Long employeeId,
    boolean pendingInvitation,
    LocalDateTime activatedAt,
    LocalDateTime lastLoginAt
) {}
