package com.brokage.webapi.service;

import com.brokage.webapi.dto.NotificationDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationStreamService {

    private final ObjectMapper objectMapper;

    // User-specific notification sinks
    private final Map<String, Sinks.Many<NotificationDTO>> userSinks = new ConcurrentHashMap<>();

    // Role-based notification sinks (for broadcasts)
    private final Sinks.Many<NotificationDTO> adminSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<NotificationDTO> brokerSink = Sinks.many().multicast().onBackpressureBuffer();
    private final Sinks.Many<NotificationDTO> customerSink = Sinks.many().multicast().onBackpressureBuffer();

    // Global notification sink (for all users)
    private final Sinks.Many<NotificationDTO> globalSink = Sinks.many().multicast().onBackpressureBuffer();

    /**
     * Subscribe to notifications for a specific user
     */
    public Flux<NotificationDTO> subscribeToUserNotifications(String userId, String role) {
        Sinks.Many<NotificationDTO> userSink = userSinks.computeIfAbsent(userId,
                k -> Sinks.many().multicast().onBackpressureBuffer());

        Flux<NotificationDTO> roleBroadcast = switch (role) {
            case "ADMIN" -> adminSink.asFlux();
            case "BROKER" -> brokerSink.asFlux();
            default -> customerSink.asFlux();
        };

        return Flux.merge(
                userSink.asFlux(),
                roleBroadcast,
                globalSink.asFlux()
        ).doOnCancel(() -> {
            log.info("User {} unsubscribed from notifications", userId);
            // Optionally clean up the sink if no more subscribers
        });
    }

    /**
     * Send a notification to a specific user
     */
    public void sendToUser(String userId, NotificationDTO notification) {
        Sinks.Many<NotificationDTO> sink = userSinks.get(userId);
        if (sink != null) {
            sink.tryEmitNext(notification);
        }
    }

    /**
     * Send a notification to all users with a specific role
     */
    public void sendToRole(String role, NotificationDTO notification) {
        switch (role) {
            case "ADMIN" -> adminSink.tryEmitNext(notification);
            case "BROKER" -> brokerSink.tryEmitNext(notification);
            case "CUSTOMER" -> customerSink.tryEmitNext(notification);
        }
    }

    /**
     * Broadcast to all users
     */
    public void broadcast(NotificationDTO notification) {
        globalSink.tryEmitNext(notification);
    }

    /**
     * Listen to order events from Kafka
     */
    @KafkaListener(topics = "order-events", groupId = "web-api-notifications")
    public void handleOrderEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.get("eventType").asText();
            String customerId = event.has("customerId") ? event.get("customerId").asText() : null;

            NotificationDTO notification = switch (eventType) {
                case "OrderCreated" -> createNotification(
                        "ORDER_CREATED",
                        "Order Created",
                        "Your order has been created and is pending",
                        "INFO",
                        customerId,
                        event
                );
                case "OrderMatched" -> createNotification(
                        "ORDER_MATCHED",
                        "Order Matched",
                        "Your order has been successfully matched",
                        "SUCCESS",
                        customerId,
                        event
                );
                case "OrderCancelled" -> createNotification(
                        "ORDER_CANCELLED",
                        "Order Cancelled",
                        "Your order has been cancelled",
                        "WARNING",
                        customerId,
                        event
                );
                case "OrderFailed" -> createNotification(
                        "ORDER_FAILED",
                        "Order Failed",
                        event.has("reason") ? event.get("reason").asText() : "Order processing failed",
                        "ERROR",
                        customerId,
                        event
                );
                default -> null;
            };

            if (notification != null && customerId != null) {
                sendToUser(customerId, notification);
                // Also notify admins
                sendToRole("ADMIN", notification);
            }
        } catch (Exception e) {
            log.error("Error processing order event for notification", e);
        }
    }

    /**
     * Listen to asset events from Kafka
     */
    @KafkaListener(topics = "asset-events", groupId = "web-api-notifications")
    public void handleAssetEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);
            String eventType = event.get("eventType").asText();
            String customerId = event.has("customerId") ? event.get("customerId").asText() : null;

            NotificationDTO notification = switch (eventType) {
                case "DepositCompleted" -> createNotification(
                        "DEPOSIT_COMPLETED",
                        "Deposit Completed",
                        "Your deposit has been processed",
                        "SUCCESS",
                        customerId,
                        event
                );
                case "WithdrawalCompleted" -> createNotification(
                        "WITHDRAWAL_COMPLETED",
                        "Withdrawal Completed",
                        "Your withdrawal has been processed",
                        "SUCCESS",
                        customerId,
                        event
                );
                default -> null;
            };

            if (notification != null && customerId != null) {
                sendToUser(customerId, notification);
            }
        } catch (Exception e) {
            log.error("Error processing asset event for notification", e);
        }
    }

    /**
     * Listen to audit events (for admin notifications)
     */
    @KafkaListener(topics = "audit-events", groupId = "web-api-notifications")
    public void handleAuditEvent(String message) {
        try {
            JsonNode event = objectMapper.readTree(message);

            // Only send critical audit events to admins
            String severity = event.has("severity") ? event.get("severity").asText() : "INFO";
            if ("ERROR".equals(severity) || "CRITICAL".equals(severity)) {
                NotificationDTO notification = NotificationDTO.builder()
                        .id(java.util.UUID.randomUUID().toString())
                        .type("SYSTEM_ALERT")
                        .title("System Alert")
                        .message(event.has("message") ? event.get("message").asText() : "System event occurred")
                        .severity(severity)
                        .timestamp(LocalDateTime.now())
                        .read(false)
                        .build();

                sendToRole("ADMIN", notification);
            }
        } catch (Exception e) {
            log.error("Error processing audit event for notification", e);
        }
    }

    private NotificationDTO createNotification(String type, String title, String message,
                                                String severity, String targetUserId, JsonNode eventData) {
        return NotificationDTO.builder()
                .id(java.util.UUID.randomUUID().toString())
                .type(type)
                .title(title)
                .message(message)
                .severity(severity)
                .targetUserId(targetUserId)
                .data(objectMapper.convertValue(eventData, Map.class))
                .timestamp(LocalDateTime.now())
                .read(false)
                .build();
    }

    /**
     * Keep-alive ping for SSE connections
     */
    public Flux<NotificationDTO> heartbeat() {
        return Flux.interval(Duration.ofSeconds(30))
                .map(tick -> NotificationDTO.builder()
                        .id("heartbeat-" + tick)
                        .type("HEARTBEAT")
                        .title("Heartbeat")
                        .message("Connection alive")
                        .severity("INFO")
                        .timestamp(LocalDateTime.now())
                        .build());
    }
}
