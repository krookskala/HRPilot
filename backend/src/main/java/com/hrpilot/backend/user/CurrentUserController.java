package com.hrpilot.backend.user;

import com.hrpilot.backend.user.dto.CurrentUserProfileResponse;
import com.hrpilot.backend.user.dto.CurrentUserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
