package com.brokage.notification.controller;

import com.brokage.common.dto.ApiResponse;
import com.brokage.common.dto.PageResponse;
import com.brokage.notification.dto.NotificationDTO;
import com.brokage.notification.dto.SendNotificationRequest;
import com.brokage.notification.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<NotificationDTO>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        log.info("Sending notification to customer: {}", request.getCustomerId());
        NotificationDTO notification = notificationService.createNotification(request, "MANUAL", null);
        return ResponseEntity.ok(ApiResponse.success(notification, "Notification created successfully"));
    }

    @GetMapping("/{notificationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER') or @notificationSecurityService.canAccess(#notificationId, authentication)")
    public ResponseEntity<ApiResponse<NotificationDTO>> getNotification(@PathVariable String notificationId) {
        NotificationDTO notification = notificationService.getNotificationById(notificationId);
        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'BROKER') or @notificationSecurityService.isOwnCustomer(#customerId, authentication)")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDTO>>> getCustomerNotifications(
            @PathVariable UUID customerId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<NotificationDTO> notifications = notificationService.getCustomerNotifications(customerId, type, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDTO>>> getMyNotifications(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Get customer ID from JWT claims
        UUID customerId = UUID.fromString(jwt.getClaim("customer_id"));
        PageResponse<NotificationDTO> notifications = notificationService.getCustomerNotifications(customerId, type, page, size);
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @PostMapping("/{notificationId}/mark-sent")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsSent(@PathVariable String notificationId) {
        NotificationDTO notification = notificationService.markAsSent(notificationId);
        return ResponseEntity.ok(ApiResponse.success(notification, "Notification marked as sent"));
    }

    @PostMapping("/{notificationId}/mark-failed")
    @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM')")
    public ResponseEntity<ApiResponse<NotificationDTO>> markAsFailed(
            @PathVariable String notificationId,
            @RequestParam String errorMessage) {
        NotificationDTO notification = notificationService.markAsFailed(notificationId, errorMessage);
        return ResponseEntity.ok(ApiResponse.success(notification, "Notification marked as failed"));
    }
}
