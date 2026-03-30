package com.hrpilot.backend.user;

import com.hrpilot.backend.audit.AuditLogService;
import com.hrpilot.backend.auth.InvitationToken;
import com.hrpilot.backend.auth.InvitationTokenService;
import com.hrpilot.backend.auth.RefreshTokenService;
import com.hrpilot.backend.common.exception.DuplicateResourceException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.notification.NotificationType;
import com.hrpilot.backend.user.dto.AdminInviteUserRequest;
import com.hrpilot.backend.user.dto.CreateUserRequest;
import com.hrpilot.backend.user.dto.UpdateUserRequest;
import com.hrpilot.backend.user.dto.UserInvitationResponse;
import com.hrpilot.backend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final InvitationTokenService invitationTokenService;
    private final RefreshTokenService refreshTokenService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend-base-url:http://localhost:4200}")
    private String frontendBaseUrl;

    @Transactional
    public UserInvitationResponse inviteUser(AdminInviteUserRequest request, User actorUser) {
        log.info("Inviting user with email: {}", request.email());
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(null)
            .role(request.role())
            .isActive(false)
            .preferredLang(request.preferredLang() == null || request.preferredLang().isBlank()
                ? "en"
                : request.preferredLang())
            .build();

        User savedUser = userRepository.save(user);
        InvitationToken invitationToken = invitationTokenService.createInvitation(savedUser, actorUser);

        auditLogService.log(actorUser, "USER_INVITED", "User", savedUser.getId().toString(),
            "User invited: " + savedUser.getEmail(), null);

        notificationService.create(actorUser, NotificationType.INVITATION_CREATED,
            "Invitation created",
            "Invitation link created for " + savedUser.getEmail(),
            "/users");

        return new UserInvitationResponse(
            toResponse(savedUser),
            invitationToken.getToken(),
            frontendBaseUrl + "/accept-invite/" + invitationToken.getToken(),
            invitationToken.getExpiresAt()
        );
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        User user = User.builder()
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .role(request.role())
            .isActive(true)
            .preferredLang("en")
            .activatedAt(LocalDateTime.now())
            .build();

        return toResponse(userRepository.save(user));
    }

    public Page<UserResponse> getAllUsers(String email, Role role, Boolean isActive, Pageable pageable) {
        return userRepository.search(email, role, isActive, pageable).map(this::toResponse);
    }

    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return getAllUsers(null, null, null, pageable);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return toResponse(user);
    }

    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, User actorUser) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.isActive() != null) {
            user.setActive(request.isActive());
        }
        if (request.preferredLang() != null && !request.preferredLang().isBlank()) {
            user.setPreferredLang(request.preferredLang());
        }

        if (!user.isActive()) {
            refreshTokenService.deleteByUserId(user.getId());
        }

        User savedUser = userRepository.save(user);

        auditLogService.log(actorUser, "USER_UPDATED", "User", savedUser.getId().toString(),
            "User updated: " + savedUser.getEmail(), null);

        return toResponse(savedUser);
    }

    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        return updateUser(id, request, null);
    }

    @Transactional
    public UserInvitationResponse resendInvitation(Long id, User actorUser) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        InvitationToken invitationToken = invitationTokenService.createInvitation(user, actorUser);

        auditLogService.log(actorUser, "USER_INVITE_REISSUED", "User", user.getId().toString(),
            "Invitation reissued for " + user.getEmail(), null);

        return new UserInvitationResponse(
            toResponse(user),
            invitationToken.getToken(),
            frontendBaseUrl + "/accept-invite/" + invitationToken.getToken(),
            invitationToken.getExpiresAt()
        );
    }

    @Transactional
    public void deleteUser(Long id, User actorUser) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

        refreshTokenService.deleteByUserId(id);
        userRepository.delete(user);

        auditLogService.log(actorUser, "USER_DELETED", "User", id.toString(),
            "User deleted: " + user.getEmail(), null);
    }

    public void deleteUser(Long id) {
        deleteUser(id, null);
    }

    private UserResponse toResponse(User user) {
        Long employeeId = employeeRepository.findByUserId(user.getId())
            .map(employee -> employee.getId())
            .orElse(null);

        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getRole(),
            user.isActive(),
            user.getPreferredLang(),
            employeeId,
            user.getActivatedAt() == null,
            user.getActivatedAt(),
            user.getLastLoginAt()
        );
    }
}
