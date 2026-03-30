package com.hrpilot.backend.audit;

import com.hrpilot.backend.audit.dto.AuditLogResponse;
import com.hrpilot.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User actorUser, String actionType, String targetType,
                    String targetId, String summary, String details) {
        ServletRequestAttributes attrs =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        String ipAddress = attrs != null && attrs.getRequest() != null
            ? attrs.getRequest().getRemoteAddr()
            : null;
        String userAgent = attrs != null && attrs.getRequest() != null
            ? attrs.getRequest().getHeader("User-Agent")
            : null;

        auditLogRepository.save(AuditLog.builder()
            .actorUser(actorUser)
            .actionType(actionType)
            .targetType(targetType)
            .targetId(targetId)
            .summary(summary)
            .details(details)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .build());
    }

    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
            .map(log -> new AuditLogResponse(
                log.getId(),
                log.getActorUser() != null ? log.getActorUser().getId() : null,
                log.getActorUser() != null ? log.getActorUser().getEmail() : null,
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getSummary(),
                log.getDetails(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getCreatedAt()
            ));
    }
}
