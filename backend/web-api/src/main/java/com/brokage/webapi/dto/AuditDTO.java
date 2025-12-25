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
public class AuditDTO {
    private String id;
    private String eventId;
    private String entityType;
    private String entityId;
    private String action;
    private String customerId;
    private String customerEmail;
    private String performedBy;
    private String performedByEmail;
    private String performedByRole;
    private Map<String, Object> previousState;
    private Map<String, Object> newState;
    private Map<String, Object> changes;
    private String description;
    private String serviceName;
    private String traceId;
    private LocalDateTime timestamp;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditFilterRequest {
        private String entityType;
        private String entityId;
        private String action;
        private String customerId;
        private String performedBy;
        private String serviceName;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private int page;
        private int size;
        private String sortBy;
        private String sortDirection;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuditPageResponse {
        private java.util.List<AuditDTO> content;
        private int page;
        private int size;
        private long totalElements;
        private int totalPages;
        private boolean first;
        private boolean last;
    }
}
