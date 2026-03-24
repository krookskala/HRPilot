package com.hrpilot.backend.user.dto;

import com.hrpilot.backend.user.Role;

public record UpdateUserRequest(
    Role role,
    boolean isActive,
    String preferredLang
){}