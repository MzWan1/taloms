package za.co.taloms.notification.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import za.co.taloms.common.ApiResponse;
import za.co.taloms.notification.application.service.NotificationService;
import za.co.taloms.notification.domain.entity.Notification;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationRestController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<Notification>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(notificationService.findAll(),
                "Notifications retrieved successfully"));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR','ROLE_REPORT_VIEWER')")
    public ResponseEntity<ApiResponse<List<Notification>>> getByEntity(
            @PathVariable String entityType, @PathVariable Long entityId) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.findByEntity(entityType, entityId),
                "Notifications retrieved successfully"));
    }

    @GetMapping("/recipient/{recipient}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN','ROLE_TA_ADMINISTRATOR')")
    public ResponseEntity<ApiResponse<List<Notification>>> getByRecipient(@PathVariable String recipient) {
        return ResponseEntity.ok(ApiResponse.success(
                notificationService.findByRecipient(recipient),
                "Notifications retrieved successfully"));
    }

    @PostMapping("/process")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> processPending() {
        notificationService.processPendingNotifications();
        return ResponseEntity.ok(ApiResponse.success(null, "Pending notifications processed"));
    }
}
