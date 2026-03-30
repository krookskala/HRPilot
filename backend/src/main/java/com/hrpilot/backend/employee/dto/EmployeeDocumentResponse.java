package com.hrpilot.backend.employee.dto;

import com.hrpilot.backend.employee.EmployeeDocumentType;

import java.time.LocalDateTime;

public record EmployeeDocumentResponse(
    Long id,
    EmployeeDocumentType documentType,
    String title,
    String description,
    String originalFilename,
    String contentType,
    long fileSize,
    Long uploadedByUserId,
    String uploadedByEmail,
    LocalDateTime uploadedAt
) {
}
