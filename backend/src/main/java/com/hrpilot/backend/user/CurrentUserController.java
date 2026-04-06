package com.hrpilot.backend.user;

import com.hrpilot.backend.auth.dto.AuthResponse;
import com.hrpilot.backend.user.dto.ChangeEmailRequest;
import com.hrpilot.backend.user.dto.ChangeLanguageRequest;
import com.hrpilot.backend.user.dto.ChangePasswordRequest;
import com.hrpilot.backend.user.dto.CurrentUserProfileResponse;
import com.hrpilot.backend.user.dto.CurrentUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Current User", description = "Current authenticated user info and profile")
@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final CurrentUserService currentUserService;

    @Operation(summary = "Get current user info")
    @GetMapping
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        return ResponseEntity.ok(currentUserService.getCurrentUserResponse());
    }

    @Operation(summary = "Get current user profile with employee details")
    @GetMapping("/profile")
    public ResponseEntity<CurrentUserProfileResponse> getCurrentUserProfile() {
        return ResponseEntity.ok(currentUserService.getCurrentUserProfile());
    }

    @Operation(summary = "Change preferred language")
    @PutMapping("/language")
    public ResponseEntity<Void> changeLanguage(@Valid @RequestBody ChangeLanguageRequest request) {
        currentUserService.changeLanguage(request);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Change password")
    @PutMapping("/password")
    public ResponseEntity<AuthResponse> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(currentUserService.changePassword(request));
    }

    @Operation(summary = "Change email")
    @PutMapping("/email")
    public ResponseEntity<AuthResponse> changeEmail(@Valid @RequestBody ChangeEmailRequest request) {
        return ResponseEntity.ok(currentUserService.changeEmail(request));
    }
}
