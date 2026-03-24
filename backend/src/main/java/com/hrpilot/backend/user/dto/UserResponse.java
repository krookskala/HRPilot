package com.hrpilot.backend.user.dto;

import com.hrpilot.backend.user.Role;

public record UserResponse(
    Long id,
    String email,
    Role role,
    boolean isActive,
    String preferredLang
) {}