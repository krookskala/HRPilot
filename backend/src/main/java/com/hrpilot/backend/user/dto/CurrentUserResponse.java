package com.hrpilot.backend.user.dto;

import com.hrpilot.backend.user.Role;

import java.time.LocalDateTime;

public record CurrentUserResponse(
    Long id,
    String email,
    Role role,
    boolean isActive,
    String preferredLang,
    Long employeeId,
    String firstName,
    String lastName,
    Long departmentId,
    String departmentName,
    long unreadNotifications,
    LocalDateTime activatedAt,
    LocalDateTime lastLoginAt
) {}
