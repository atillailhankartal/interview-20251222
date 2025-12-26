package com.brokage.notification.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "notifications")
@CompoundIndex(name = "idx_customer_type_timestamp", def = "{'customerId': 1, 'notificationType': 1, 'createdAt': -1}")
@CompoundIndex(name = "idx_status_channel", def = "{'status': 1, 'channel': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    @Id
    private String id;

    @Indexed
    private UUID eventId;

    @Indexed
    private UUID customerId;

    private String customerEmail;

    @Indexed
    private String notificationType;

    @Indexed
    private String channel;

    private String recipient;

    private String templateCode;

    private String subject;

    private String body;

    private Map<String, Object> templateVariables;

    private Map<String, Object> metadata;

    @Indexed
    private String status;

    private String errorMessage;

    private Integer retryCount;

    private Integer maxRetries;

    private LocalDateTime scheduledAt;

    private LocalDateTime sentAt;

    private LocalDateTime deliveredAt;

    private LocalDateTime failedAt;

    private String traceId;

    private String spanId;

    @Indexed
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDateTime updatedAt;

    public enum NotificationStatus {
        PENDING,
        SCHEDULED,
        SENDING,
        SENT,
        DELIVERED,
        FAILED,
        CANCELED
    }

    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH,
        WEBSOCKET,
        IN_APP
    }

    public boolean canRetry() {
        return retryCount != null && maxRetries != null && retryCount < maxRetries;
    }

    public void incrementRetry() {
        if (retryCount == null) {
            retryCount = 0;
        }
        retryCount++;
    }

    public void markAsSent() {
        this.status = NotificationStatus.SENT.name();
        this.sentAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void markAsFailed(String error) {
        this.status = NotificationStatus.FAILED.name();
        this.errorMessage = error;
        this.failedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
