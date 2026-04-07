package com.hrpilot.backend.user.dto;

import jakarta.validation.constraints.NotNull;

public record ChangeDarkModeRequest(
    @NotNull
    Boolean darkMode
) {}
