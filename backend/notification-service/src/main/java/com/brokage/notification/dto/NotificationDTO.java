package com.brokage.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {

    private String id;
    private UUID eventId;
    private UUID customerId;
    private String notificationType;
    private String channel;
    private String recipient;
    private String subject;
    private String body;
    private Map<String, Object> templateVariables;
    private String status;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private LocalDateTime createdAt;
}
