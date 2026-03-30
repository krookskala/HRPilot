package com.hrpilot.backend.user;

import com.hrpilot.backend.user.dto.CurrentUserProfileResponse;
import com.hrpilot.backend.user.dto.CurrentUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class CurrentUserController {

    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<CurrentUserResponse> getCurrentUser() {
        return ResponseEntity.ok(currentUserService.getCurrentUserResponse());
    }

    @GetMapping("/profile")
    public ResponseEntity<CurrentUserProfileResponse> getCurrentUserProfile() {
        return ResponseEntity.ok(currentUserService.getCurrentUserProfile());
    }
}
