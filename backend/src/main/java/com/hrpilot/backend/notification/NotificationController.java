package com.hrpilot.backend.notification;

import com.hrpilot.backend.notification.dto.NotificationResponse;
import com.hrpilot.backend.user.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Notifications", description = "User notification management")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @Operation(summary = "Get notifications for current user")
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(Pageable pageable) {
        Long userId = currentUserService.getCurrentUser().id();
        return ResponseEntity.ok(notificationService.getForUser(userId, pageable));
    }

    @Operation(summary = "Mark a notification as read")
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long id) {
        Long userId = currentUserService.getCurrentUser().id();
        return ResponseEntity.ok(notificationService.markAsRead(id, userId));
    }
}
