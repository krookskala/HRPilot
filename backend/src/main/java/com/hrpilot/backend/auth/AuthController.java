package com.hrpilot.backend.auth;

import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.auth.dto.*;
import com.hrpilot.backend.common.exception.AuthenticationException;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.notification.NotificationType;
import com.hrpilot.backend.security.JwtService;
import com.hrpilot.backend.user.CurrentUserService;
import com.hrpilot.backend.user.User;
import com.hrpilot.backend.user.UserRepository;
import com.hrpilot.backend.user.dto.CurrentUserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final InvitationTokenService invitationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final com.hrpilot.backend.employee.EmployeeRepository employeeRepository;

    @Value("${app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        log.info("Login attempt for email: {}", request.email());
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

        if (!user.isActive() || user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
            throw new AuthenticationException("Account is not active");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            auditLogService.log(user, "LOGIN_FAILED", "User", user.getId().toString(),
                "Login failed", "Wrong password");
            throw new AuthenticationException("Invalid email or password");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        auditLogService.log(user, "LOGIN_SUCCESS", "User", user.getId().toString(),
            "Login successful", null);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken(), buildUserResponse(user)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(
            @Valid @RequestBody TokenRefreshRequest request) {
        RefreshToken refreshToken = refreshTokenService.rotateRefreshToken(request.refreshToken());
        User user = refreshToken.getUser();

        String newAccessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        auditLogService.log(user, "TOKEN_REFRESHED", "User", user.getId().toString(),
            "Access token refreshed", null);

        return ResponseEntity.ok(new TokenRefreshResponse(newAccessToken, refreshToken.getToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.revokeToken(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/invitations/{token}")
    public ResponseEntity<InvitationDetailsResponse> getInvitation(@PathVariable String token) {
        InvitationToken invitationToken = invitationTokenService.validateInvitation(token);
        return ResponseEntity.ok(invitationTokenService.toDetails(invitationToken));
    }

    @PostMapping("/invitations/accept")
    public ResponseEntity<AuthResponse> acceptInvitation(
            @Valid @RequestBody AcceptInvitationRequest request) {
        InvitationToken invitationToken = invitationTokenService.validateInvitation(request.token());
        User user = invitationToken.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setActive(true);
        user.setActivatedAt(LocalDateTime.now());
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
        invitationTokenService.consume(invitationToken);

        auditLogService.log(user, "INVITATION_ACCEPTED", "User", user.getId().toString(),
            "Invitation accepted", null);
        notificationService.create(user, NotificationType.ACCOUNT_ACTIVATED,
            "Account activated",
            "Your HRPilot account is ready to use.",
            "/dashboard");

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        return ResponseEntity.ok(new AuthResponse(accessToken, refreshToken.getToken(), buildUserResponse(user)));
    }

    @PostMapping("/password/request")
    public ResponseEntity<PasswordResetResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user != null) {
            PasswordResetToken token = passwordResetTokenService.createResetToken(user);
            auditLogService.log(user, "PASSWORD_RESET_REQUESTED", "User", user.getId().toString(),
                "Password reset requested", null);
            log.info("Password reset token created for user: {}", user.getEmail());
        } else {
            log.warn("Password reset requested for non-existent email: {}", request.email());
        }

        return ResponseEntity.ok(new PasswordResetResponse(
            "If the email exists, a reset link has been sent.",
            null,
            null
        ));
    }

    @GetMapping("/password/{token}")
    public ResponseEntity<TokenValidationResponse> validatePasswordResetToken(@PathVariable String token) {
        PasswordResetToken resetToken = passwordResetTokenService.validate(token);
        return ResponseEntity.ok(new TokenValidationResponse(
            resetToken.getUser().getEmail(),
            resetToken.getExpiresAt()
        ));
    }

    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = passwordResetTokenService.validate(request.token());
        User user = resetToken.getUser();

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);

        passwordResetTokenService.consume(resetToken);
        refreshTokenService.deleteByUserId(user.getId());
        auditLogService.log(user, "PASSWORD_RESET_COMPLETED", "User", user.getId().toString(),
            "Password reset completed", null);
        notificationService.create(user, NotificationType.PASSWORD_RESET,
            "Password updated",
            "Your password was changed successfully.",
            "/profile");

        return ResponseEntity.noContent().build();
    }

    private CurrentUserResponse buildUserResponse(User user) {
        var employee = employeeRepository.findByUserIdWithDepartment(user.getId()).orElse(null);
        return new CurrentUserResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getPreferredLang(),
            employee != null ? employee.getId() : null,
            employee != null ? employee.getFirstName() : null,
            employee != null ? employee.getLastName() : null,
            employee != null && employee.getDepartment() != null ? employee.getDepartment().getId() : null,
            employee != null && employee.getDepartment() != null ? employee.getDepartment().getName() : null,
            0L,
            user.getActivatedAt(),
            user.getLastLoginAt()
        );
    }
}
