package com.brokage.webapi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String id;
    private String type;
    private String title;
    private String message;
    private String severity; // INFO, SUCCESS, WARNING, ERROR
    private String targetUserId;
    private String targetRole;
    private Map<String, Object> data;
    private LocalDateTime timestamp;
    private boolean read;

    public enum NotificationType {
        ORDER_CREATED,
        ORDER_MATCHED,
        ORDER_CANCELED,
        ORDER_FAILED,
        DEPOSIT_COMPLETED,
        WITHDRAWAL_COMPLETED,
        PRICE_ALERT,
        SYSTEM_ALERT,
        CUSTOMER_ASSIGNED,
        CUSTOMER_ACTIVITY
    }

    public enum Severity {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }
}
