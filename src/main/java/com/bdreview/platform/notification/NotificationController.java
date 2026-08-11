package com.bdreview.platform.notification;

import com.bdreview.platform.common.CurrentUser;
import com.bdreview.platform.common.PageRequestDefaults;
import com.bdreview.platform.common.PageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationListResponse> list(
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        UUID userId = CurrentUser.id();
        var results = notificationService.myNotifications(
                userId, PageRequest.of(page, PageRequestDefaults.clamp(size)));
        return ResponseEntity.ok(new NotificationListResponse(PageResponse.of(results), notificationService.unreadCount(userId)));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        notificationService.markRead(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllRead() {
        notificationService.markAllRead(CurrentUser.id());
        return ResponseEntity.noContent().build();
    }
}
