package com.hrpilot.backend.audit.dto;

import java.time.LocalDateTime;

public record AuditLogResponse(
    Long id,
    Long actorUserId,
    String actorEmail,
    String actionType,
    String targetType,
    String targetId,
    String summary,
    String details,
    String ipAddress,
    String userAgent,
    LocalDateTime createdAt
) {}
