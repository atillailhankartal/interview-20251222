package com.brokage.audit.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "notification_logs")
@CompoundIndex(name = "idx_customer_type_timestamp", def = "{'customerId': 1, 'notificationType': 1, 'timestamp': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLog {

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

    private String subject;

    private String content;

    private Map<String, Object> metadata;

    @Indexed
    private String status;

    private String errorMessage;

    private Integer retryCount;

    private LocalDateTime sentAt;

    @Indexed
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
