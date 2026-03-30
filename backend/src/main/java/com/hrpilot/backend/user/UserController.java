package com.hrpilot.backend.user;

import com.hrpilot.backend.user.dto.AdminInviteUserRequest;
import com.hrpilot.backend.user.dto.UserResponse;
import com.hrpilot.backend.user.dto.UserInvitationResponse;
import com.hrpilot.backend.user.dto.UpdateUserRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService userService;
    private final CurrentUserService currentUserService;

    @PostMapping("/invite")
    public ResponseEntity<UserInvitationResponse> inviteUser(
            @Valid @RequestBody AdminInviteUserRequest request) {
        UserInvitationResponse response = userService.inviteUser(
            request,
            currentUserService.getCurrentUserEntity()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @org.springframework.web.bind.annotation.RequestParam(required = false) String email,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Role role,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean isActive,
            Pageable pageable) {
        return ResponseEntity.ok(userService.getAllUsers(email, role, isActive, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request, currentUserService.getCurrentUserEntity()));
    }

    @PostMapping("/{id}/resend-invite")
    public ResponseEntity<UserInvitationResponse> resendInvitation(@PathVariable Long id) {
        return ResponseEntity.ok(userService.resendInvitation(id, currentUserService.getCurrentUserEntity()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id, currentUserService.getCurrentUserEntity());
        return ResponseEntity.noContent().build();
    }
}
