package com.brokage.audit.document;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Document(collection = "audit_logs")
@CompoundIndex(name = "idx_entity_action", def = "{'entityType': 1, 'action': 1}")
@CompoundIndex(name = "idx_customer_timestamp", def = "{'customerId': 1, 'timestamp': -1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    private String id;

    @Indexed
    private UUID eventId;

    @Indexed
    private String entityType;

    @Indexed
    private UUID entityId;

    @Indexed
    private String action;

    @Indexed
    private UUID customerId;

    private String customerEmail;

    @Indexed
    private UUID performedBy;

    private String performedByEmail;

    private String performedByRole;

    private Map<String, Object> previousState;

    private Map<String, Object> newState;

    private Map<String, Object> changes;

    private String ipAddress;

    private String userAgent;

    private String requestId;

    private String traceId;

    private String spanId;

    private String serviceName;

    private String description;

    @Indexed
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
