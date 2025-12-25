package com.brokage.webapi.controller;

import com.brokage.webapi.dto.AuditDTO;
import com.brokage.webapi.dto.AuditDTO.AuditFilterRequest;
import com.brokage.webapi.dto.AuditDTO.AuditPageResponse;
import com.brokage.webapi.service.AuditProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Audit Controller - ADMIN ONLY
 * Provides audit log viewing capabilities for administrators
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditProxyService auditProxyService;

    /**
     * Get paginated audit logs with filters
     */
    @GetMapping
    public Mono<ResponseEntity<AuditPageResponse>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) String performedBy,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            Authentication authentication) {

        String token = extractToken(authentication);
        log.info("Admin {} fetching audit logs", extractUsername(authentication));

        AuditFilterRequest filter = AuditFilterRequest.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .customerId(customerId)
                .performedBy(performedBy)
                .serviceName(serviceName)
                .startDate(startDate)
                .endDate(endDate)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        return auditProxyService.getAuditLogs(filter, token)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error fetching audit logs", e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Get single audit log by ID
     */
    @GetMapping("/{id}")
    public Mono<ResponseEntity<AuditDTO>> getAuditLogById(
            @PathVariable String id,
            Authentication authentication) {

        String token = extractToken(authentication);
        log.info("Admin {} fetching audit log: {}", extractUsername(authentication), id);

        return auditProxyService.getAuditLogById(id, token)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    /**
     * Get audit trail for a specific entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    public Mono<ResponseEntity<AuditPageResponse>> getEntityAuditTrail(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        String token = extractToken(authentication);
        log.info("Admin {} fetching audit trail for {} {}",
                extractUsername(authentication), entityType, entityId);

        return auditProxyService.getEntityAuditTrail(entityType, entityId, page, size, token)
                .map(ResponseEntity::ok);
    }

    /**
     * Get audit statistics by entity type and action
     */
    @GetMapping("/stats/{entityType}/{action}")
    public Mono<ResponseEntity<Long>> getAuditStats(
            @PathVariable String entityType,
            @PathVariable String action,
            Authentication authentication) {

        String token = extractToken(authentication);
        log.info("Admin {} fetching audit stats for {} {}",
                extractUsername(authentication), entityType, action);

        return auditProxyService.getAuditStats(entityType, action, token)
                .map(ResponseEntity::ok);
    }

    private String extractToken(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        return "";
    }

    private String extractUsername(Authentication authentication) {
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("preferred_username");
        }
        return "unknown";
    }
}
