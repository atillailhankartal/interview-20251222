package com.brokage.webapi.service;

import com.brokage.webapi.dto.AuditDTO;
import com.brokage.webapi.dto.AuditDTO.AuditFilterRequest;
import com.brokage.webapi.dto.AuditDTO.AuditPageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditProxyService {

    @Qualifier("auditServiceClient")
    private final WebClient auditServiceClient;

    /**
     * Fetch audit logs with filters (ADMIN only)
     */
    public Mono<AuditPageResponse> getAuditLogs(AuditFilterRequest filter, String token) {
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromPath("/api/audit")
                .queryParam("page", filter.getPage())
                .queryParam("size", filter.getSize());

        if (filter.getEntityType() != null) {
            uriBuilder.queryParam("entityType", filter.getEntityType());
        }
        if (filter.getEntityId() != null) {
            uriBuilder.queryParam("entityId", filter.getEntityId());
        }
        if (filter.getAction() != null) {
            uriBuilder.queryParam("action", filter.getAction());
        }
        if (filter.getCustomerId() != null) {
            uriBuilder.queryParam("customerId", filter.getCustomerId());
        }
        if (filter.getPerformedBy() != null) {
            uriBuilder.queryParam("performedBy", filter.getPerformedBy());
        }
        if (filter.getServiceName() != null) {
            uriBuilder.queryParam("serviceName", filter.getServiceName());
        }
        if (filter.getStartDate() != null) {
            uriBuilder.queryParam("startDate", filter.getStartDate().toString());
        }
        if (filter.getEndDate() != null) {
            uriBuilder.queryParam("endDate", filter.getEndDate().toString());
        }
        if (filter.getSortBy() != null) {
            uriBuilder.queryParam("sortBy", filter.getSortBy());
        }
        if (filter.getSortDirection() != null) {
            uriBuilder.queryParam("sortDirection", filter.getSortDirection());
        }

        return auditServiceClient.get()
                .uri(uriBuilder.toUriString())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToAuditPageResponse)
                .onErrorResume(e -> {
                    log.error("Error fetching audit logs", e);
                    return Mono.just(AuditPageResponse.builder()
                            .content(List.of())
                            .page(0)
                            .size(0)
                            .totalElements(0)
                            .totalPages(0)
                            .first(true)
                            .last(true)
                            .build());
                });
    }

    /**
     * Get audit log by ID (ADMIN only)
     */
    public Mono<AuditDTO> getAuditLogById(String id, String token) {
        return auditServiceClient.get()
                .uri("/api/audit/" + id)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToAuditDTO)
                .onErrorResume(e -> {
                    log.error("Error fetching audit log by id: {}", id, e);
                    return Mono.empty();
                });
    }

    /**
     * Get entity audit trail (ADMIN only)
     */
    public Mono<AuditPageResponse> getEntityAuditTrail(String entityType, String entityId,
                                                        int page, int size, String token) {
        return auditServiceClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/audit/entity/{entityType}/{entityId}")
                        .queryParam("page", page)
                        .queryParam("size", size)
                        .build(entityType, entityId))
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(this::mapToAuditPageResponse)
                .onErrorResume(e -> {
                    log.error("Error fetching entity audit trail", e);
                    return Mono.just(createEmptyPageResponse());
                });
    }

    /**
     * Get audit stats (ADMIN only)
     */
    public Mono<Long> getAuditStats(String entityType, String action, String token) {
        return auditServiceClient.get()
                .uri("/api/audit/stats/{entityType}/{action}", entityType, action)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    Object data = response.get("data");
                    if (data instanceof Number) {
                        return ((Number) data).longValue();
                    }
                    return 0L;
                })
                .onErrorReturn(0L);
    }

    @SuppressWarnings("unchecked")
    private AuditPageResponse mapToAuditPageResponse(Map<String, Object> response) {
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) {
            return createEmptyPageResponse();
        }

        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        List<AuditDTO> auditLogs = content != null
                ? content.stream().map(this::mapContentToAuditDTO).toList()
                : List.of();

        return AuditPageResponse.builder()
                .content(auditLogs)
                .page(getInt(data, "number"))
                .size(getInt(data, "size"))
                .totalElements(getLong(data, "totalElements"))
                .totalPages(getInt(data, "totalPages"))
                .first(getBoolean(data, "first"))
                .last(getBoolean(data, "last"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private AuditDTO mapToAuditDTO(Map<String, Object> response) {
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) {
            return null;
        }
        return mapContentToAuditDTO(data);
    }

    @SuppressWarnings("unchecked")
    private AuditDTO mapContentToAuditDTO(Map<String, Object> data) {
        return AuditDTO.builder()
                .id((String) data.get("id"))
                .eventId((String) data.get("eventId"))
                .entityType((String) data.get("entityType"))
                .entityId((String) data.get("entityId"))
                .action((String) data.get("action"))
                .customerId((String) data.get("customerId"))
                .customerEmail((String) data.get("customerEmail"))
                .performedBy((String) data.get("performedBy"))
                .performedByEmail((String) data.get("performedByEmail"))
                .performedByRole((String) data.get("performedByRole"))
                .previousState((Map<String, Object>) data.get("previousState"))
                .newState((Map<String, Object>) data.get("newState"))
                .changes((Map<String, Object>) data.get("changes"))
                .description((String) data.get("description"))
                .serviceName((String) data.get("serviceName"))
                .traceId((String) data.get("traceId"))
                .build();
    }

    private AuditPageResponse createEmptyPageResponse() {
        return AuditPageResponse.builder()
                .content(List.of())
                .page(0)
                .size(0)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .build();
    }

    private int getInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private long getLong(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private boolean getBoolean(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return false;
    }
}
