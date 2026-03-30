package com.hrpilot.backend.user;

import com.hrpilot.backend.common.exception.AuthenticationException;
import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.employee.Employee;
import com.hrpilot.backend.employee.EmployeeDocumentRepository;
import com.hrpilot.backend.employee.EmployeeRepository;
import com.hrpilot.backend.employee.EmploymentHistoryRepository;
import com.hrpilot.backend.employee.dto.EmployeeDocumentResponse;
import com.hrpilot.backend.employee.dto.EmploymentHistoryResponse;
import com.hrpilot.backend.notification.NotificationService;
import com.hrpilot.backend.security.AuthenticatedUser;
import com.hrpilot.backend.user.dto.CurrentUserProfileResponse;
import com.hrpilot.backend.user.dto.CurrentUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final EmploymentHistoryRepository historyRepository;
    private final EmployeeDocumentRepository employeeDocumentRepository;
    private final NotificationService notificationService;

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
