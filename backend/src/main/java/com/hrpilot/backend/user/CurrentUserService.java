package com.hrpilot.backend.user;

import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.auth.RefreshTokenService;
import com.hrpilot.backend.auth.RefreshToken;
import com.hrpilot.backend.auth.dto.AuthResponse;
import com.hrpilot.backend.common.exception.AuthenticationException;
import com.hrpilot.backend.common.exception.DuplicateResourceException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeDocumentRepository;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.employee.EmploymentHistoryRepository;
import com.hrpilot.backend.employee.dto.EmployeeDocumentResponse;
import com.hrpilot.backend.employee.dto.EmploymentHistoryResponse;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.notification.NotificationType;
import com.hrpilot.backend.security.AuthenticatedUser;
import com.hrpilot.backend.security.JwtService;
import com.hrpilot.backend.user.dto.ChangeEmailRequest;
import com.hrpilot.backend.user.dto.ChangeLanguageRequest;
import com.hrpilot.backend.user.dto.ChangePasswordRequest;
import com.hrpilot.backend.user.dto.CurrentUserProfileResponse;
import com.hrpilot.backend.user.dto.CurrentUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final EmploymentHistoryRepository historyRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthenticatedUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser currentUser)) {
            throw new AuthenticationException("No authenticated user in context");
        }
        return currentUser;
    }

    public User getCurrentUserEntity() {
        AuthenticatedUser currentUser = getCurrentUser();
        return userRepository.findById(currentUser.id())
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.id()));
    }

    public CurrentUserResponse getCurrentUserResponse() {
        User user = getCurrentUserEntity();
        Employee employee = employeeRepository.findByUserIdWithDepartment(user.getId()).orElse(null);

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
            notificationService.countUnread(user.getId()),
            user.getActivatedAt(),
            user.getLastLoginAt()
        );
    }

    @Transactional
    public void changeLanguage(ChangeLanguageRequest request) {
        User user = getCurrentUserEntity();
        user.setPreferredLang(request.preferredLang());
        userRepository.save(user);
        auditLogService.log(user, "LANGUAGE_CHANGED", "User", user.getId().toString(),
            "Language changed to " + request.preferredLang(), null);
    }

    @Transactional
    public AuthResponse changePassword(ChangePasswordRequest request) {
        User user = getCurrentUserEntity();

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AuthenticationException("Current password is incorrect");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        refreshTokenService.deleteByUserId(user.getId());

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        auditLogService.log(user, "PASSWORD_CHANGED", "User", user.getId().toString(),
            "Password changed", null);
        notificationService.create(user, NotificationType.PASSWORD_RESET,
            "Password changed", "Your password was changed successfully.", "/profile");

        return new AuthResponse(accessToken, refreshToken.getToken(), buildCurrentUserResponse(user));
    }

    @Transactional
    public AuthResponse changeEmail(ChangeEmailRequest request) {
        User user = getCurrentUserEntity();

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AuthenticationException("Password is incorrect");
        }

        if (userRepository.existsByEmail(request.newEmail())) {
            throw new DuplicateResourceException("User", "email", request.newEmail());
        }

        String oldEmail = user.getEmail();
        user.setEmail(request.newEmail());
        userRepository.save(user);

        refreshTokenService.deleteByUserId(user.getId());

        String accessToken = jwtService.generateToken(user.getEmail(), user.getRole().name());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user);

        auditLogService.log(user, "EMAIL_CHANGED", "User", user.getId().toString(),
            "Email changed from " + oldEmail + " to " + request.newEmail(), null);
        notificationService.create(user, NotificationType.SECURITY_EVENT,
            "Email changed", "Your email was changed to " + request.newEmail(), "/profile");

        return new AuthResponse(accessToken, refreshToken.getToken(), buildCurrentUserResponse(user));
    }

    private CurrentUserResponse buildCurrentUserResponse(User user) {
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
            notificationService.countUnread(user.getId()),
            user.getActivatedAt(),
            user.getLastLoginAt()
        );
    }

    public CurrentUserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUserEntity();
        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);

        List<EmploymentHistoryResponse> history = employee == null
            ? List.of()
            : historyRepository.findByEmployeeIdOrderByChangedAtDesc(employee.getId()).stream()
                .map(item -> new EmploymentHistoryResponse(
                    item.getId(),
                    item.getChangeType(),
                    item.getOldValue(),
                    item.getNewValue(),
                    item.getChangedAt()
                ))
                .toList();

        CurrentUserProfileResponse.EmployeeProfile employeeProfile = employee == null
            ? null
            : new CurrentUserProfileResponse.EmployeeProfile(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getPosition(),
                employee.getHireDate(),
                employee.getPhotoUrl(),
                employee.getDepartment() != null ? employee.getDepartment().getId() : null,
                employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                history,
                employeeDocumentRepository.findByEmployeeIdOrderByCreatedAtDesc(employee.getId()).stream()
                    .map(document -> new EmployeeDocumentResponse(
                        document.getId(),
                        document.getDocumentType(),
                        document.getTitle(),
                        document.getDescription(),
                        document.getOriginalFilename(),
                        document.getContentType(),
                        document.getFileSize(),
                        document.getUploadedByUser() != null ? document.getUploadedByUser().getId() : null,
                        document.getUploadedByUser() != null ? document.getUploadedByUser().getEmail() : null,
                        document.getCreatedAt()
                    ))
                    .toList()
            );

        return new CurrentUserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getPreferredLang(),
            user.getActivatedAt(),
            user.getLastLoginAt(),
            employeeProfile,
            notificationService.countUnread(user.getId())
        );
    }
}
