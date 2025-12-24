package com.brokage.audit.dto;

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
public class AuditLogDTO {

    private String id;
    private UUID eventId;
    private String entityType;
    private UUID entityId;
    private String action;
    private UUID customerId;
    private String customerEmail;
    private UUID performedBy;
    private String performedByEmail;
    private String performedByRole;
    private Map<String, Object> previousState;
    private Map<String, Object> newState;
    private Map<String, Object> changes;
    private String description;
    private String serviceName;
    private String traceId;
    private LocalDateTime timestamp;
}
