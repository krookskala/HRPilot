package com.hrpilot.backend.security;

import com.hrpilot.backend.user.Role;

public record AuthenticatedUser(
    Long id,
    String email,
    Role role
) {}
