package com.brokage.webapi.controller;

import com.brokage.webapi.dto.NotificationDTO;
import com.brokage.webapi.service.NotificationStreamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/stream")
@RequiredArgsConstructor
@Slf4j
public class StreamController {

    private final NotificationStreamService notificationStreamService;

    /**
     * SSE endpoint for real-time notifications
     * Streams notifications based on user's role and ID
     */
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<NotificationDTO>> streamNotifications(Authentication authentication) {
        String userId = extractUserId(authentication);
        String role = extractPrimaryRole(authentication);
        String username = extractUsername(authentication);

        log.info("User {} ({}) subscribed to notification stream", username, role);

        return Flux.merge(
                notificationStreamService.subscribeToUserNotifications(userId, role),
                notificationStreamService.heartbeat()
        )
        .map(notification -> ServerSentEvent.<NotificationDTO>builder()
                .id(notification.getId())
                .event(notification.getType())
                .data(notification)
                .build())
        .doOnCancel(() -> log.info("User {} unsubscribed from notification stream", username));
    }

    /**
     * SSE endpoint for dashboard data updates
     * Pushes dashboard updates every 30 seconds
     */
    @GetMapping(value = "/dashboard", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamDashboardUpdates(Authentication authentication) {
        String userId = extractUserId(authentication);

        return Flux.interval(java.time.Duration.ofSeconds(30))
                .map(tick -> ServerSentEvent.<String>builder()
                        .id("dashboard-" + tick)
                        .event("DASHBOARD_UPDATE")
                        .data("{\"refresh\": true, \"timestamp\": \"" + java.time.LocalDateTime.now() + "\"}")
                        .build());
    }

    private String extractUserId(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        return "unknown";
    }

    private String extractUsername(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("preferred_username");
        }
        return "unknown";
    }

    private String extractPrimaryRole(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .filter(role -> List.of("ADMIN", "BROKER", "CUSTOMER").contains(role))
                .findFirst()
                .orElse("CUSTOMER");
    }
}
