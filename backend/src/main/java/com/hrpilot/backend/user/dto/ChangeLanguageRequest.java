package com.hrpilot.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ChangeLanguageRequest(
    @NotBlank
    @Pattern(regexp = "^(en|de|tr)$", message = "Language must be en, de, or tr")
    String preferredLang
) {}
