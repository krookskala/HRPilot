package com.hrpilot.backend.notification;

import com.hrpilot.backend.notification.dto.NotificationResponse;
import com.hrpilot.backend.user.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(Pageable pageable) {
        Long userId = currentUserService.getCurrentUser().id();
        return ResponseEntity.ok(notificationService.getForUser(userId, pageable));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUser().id();
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }
}
