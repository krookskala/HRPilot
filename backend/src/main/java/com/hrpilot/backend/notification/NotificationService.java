package com.hrpilot.backend.notification;

import com.hrpilot.backend.common.exception.ResourceNotFoundException;
import com.hrpilot.backend.notification.dto.NotificationResponse;
import com.hrpilot.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void create(User user, NotificationType type, String title, String message, String actionUrl) {
        notificationRepository.save(Notification.builder()
            .user(user)
            .type(type)
            .title(title)
            .message(message)
            .actionUrl(actionUrl)
            .build());
    }

    public Page<NotificationResponse> getForUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .map(notification -> new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getActionUrl(),
                notification.getReadAt() != null,
                notification.getCreatedAt(),
                notification.getReadAt()
            ));
    }

    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public NotificationResponse markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", "id", notificationId);
        }

        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
        }

        return new NotificationResponse(
            notification.getId(),
            notification.getType(),
            notification.getTitle(),
            notification.getMessage(),
            notification.getActionUrl(),
            true,
            notification.getCreatedAt(),
            notification.getReadAt()
        );
    }
}
