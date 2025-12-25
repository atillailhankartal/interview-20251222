package com.brokage.audit.controller;

import com.brokage.audit.dto.AuditFilterRequest;
import com.brokage.audit.dto.AuditLogDTO;
import com.brokage.audit.service.AuditService;
import com.brokage.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getAuditLogs(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID performedBy,
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {

        log.debug("Fetching audit logs with filters");

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

        Page<AuditLogDTO> auditLogs = auditService.getAuditLogs(filter);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuditLogDTO>> getAuditLogById(@PathVariable String id) {
        log.debug("Fetching audit log by ID: {}", id);
        AuditLogDTO auditLog = auditService.getAuditLogById(id);
        return ResponseEntity.ok(ApiResponse.success(auditLog));
    }

    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getEntityAuditTrail(
            @PathVariable String entityType,
            @PathVariable UUID entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("Fetching audit trail for {} {}", entityType, entityId);
        Page<AuditLogDTO> auditLogs = auditService.getEntityAuditTrail(entityType, entityId, page, size);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasRole('ADMIN') or @auditSecurityService.isOwnCustomer(#customerId, authentication)")
    public ResponseEntity<ApiResponse<Page<AuditLogDTO>>> getCustomerAuditLogs(
            @PathVariable UUID customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        log.debug("Fetching audit logs for customer {}", customerId);
        Page<AuditLogDTO> auditLogs = auditService.getCustomerAuditLogs(customerId, page, size);
        return ResponseEntity.ok(ApiResponse.success(auditLogs));
    }

    @GetMapping("/stats/{entityType}/{action}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> getActionCount(
            @PathVariable String entityType,
            @PathVariable String action) {

        log.debug("Getting count for {} {}", entityType, action);
        long count = auditService.countByEntityTypeAndAction(entityType, action);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
