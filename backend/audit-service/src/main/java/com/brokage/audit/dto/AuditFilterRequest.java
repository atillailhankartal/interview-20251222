package com.brokage.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditFilterRequest {

    private String entityType;
    private UUID entityId;
    private String action;
    private UUID customerId;
    private UUID performedBy;
    private String serviceName;
    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 50;

    @Builder.Default
    private String sortBy = "timestamp";

    @Builder.Default
    private String sortDirection = "DESC";
}
