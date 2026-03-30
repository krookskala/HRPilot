package com.hrpilot.backend.user.dto;

import com.hrpilot.backend.employee.dto.EmployeeDocumentResponse;
import com.hrpilot.backend.employee.dto.EmploymentHistoryResponse;
import com.hrpilot.backend.user.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CurrentUserProfileResponse(
    Long id,
    String email,
    Role role,
    boolean isActive,
    String preferredLang,
    LocalDateTime activatedAt,
    LocalDateTime lastLoginAt,
    EmployeeProfile employee,
    long unreadNotifications
) {
    public record EmployeeProfile(
        Long employeeId,
        String firstName,
        String lastName,
        String position,
        LocalDate hireDate,
        String photoUrl,
        Long departmentId,
        String departmentName,
        List<EmploymentHistoryResponse> employmentHistory,
        List<EmployeeDocumentResponse> documents
    ) {}
}
