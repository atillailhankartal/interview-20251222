package com.brokage.audit.service;

import com.brokage.audit.document.AuditLog;
import com.brokage.audit.dto.AuditFilterRequest;
import com.brokage.audit.dto.AuditLogDTO;
import com.brokage.audit.repository.AuditLogRepository;
import com.brokage.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final MongoTemplate mongoTemplate;

    public AuditLogDTO createAuditLog(String entityType, UUID entityId, String action,
                                       UUID customerId, UUID performedBy, String performedByRole,
                                       Map<String, Object> previousState, Map<String, Object> newState,
                                       String description, String serviceName, UUID eventId) {
        log.info("Creating audit log for entity {} {} action {}", entityType, entityId, action);

        if (eventId != null) {
            var existing = auditLogRepository.findByEventId(eventId);
            if (existing.isPresent()) {
                log.info("Audit log for event {} already exists, skipping", eventId);
                return toDTO(existing.get());
            }
        }

        AuditLog auditLog = AuditLog.builder()
                .eventId(eventId != null ? eventId : UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .customerId(customerId)
                .performedBy(performedBy)
                .performedByRole(performedByRole)
                .previousState(previousState)
                .newState(newState)
                .changes(calculateChanges(previousState, newState))
                .description(description)
                .serviceName(serviceName)
                .timestamp(LocalDateTime.now())
                .build();

        AuditLog saved = auditLogRepository.save(auditLog);
        log.info("Audit log created with ID: {}", saved.getId());
        return toDTO(saved);
    }

    public AuditLogDTO getAuditLogById(String id) {
        AuditLog auditLog = auditLogRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AuditLog", id));
        return toDTO(auditLog);
    }

    public Page<AuditLogDTO> getAuditLogs(AuditFilterRequest filter) {
        log.debug("Querying audit logs with filter: {}", filter);

        Sort.Direction direction = "ASC".equalsIgnoreCase(filter.getSortDirection())
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(filter.getPage(), filter.getSize(),
                Sort.by(direction, filter.getSortBy()));

        Query query = buildQuery(filter);
        query.with(pageable);

        List<AuditLog> auditLogs = mongoTemplate.find(query, AuditLog.class);
        long count = mongoTemplate.count(Query.of(query).limit(-1).skip(-1), AuditLog.class);

        Page<AuditLog> page = PageableExecutionUtils.getPage(auditLogs, pageable, () -> count);
        return page.map(this::toDTO);
    }

    public Page<AuditLogDTO> getEntityAuditTrail(String entityType, UUID entityId, int page, int size) {
        log.debug("Getting audit trail for {} {}", entityType, entityId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
        return auditLogs.map(this::toDTO);
    }

    public Page<AuditLogDTO> getCustomerAuditLogs(UUID customerId, int page, int size) {
        log.debug("Getting audit logs for customer {}", customerId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> auditLogs = auditLogRepository.findByCustomerId(customerId, pageable);
        return auditLogs.map(this::toDTO);
    }

    public long countByEntityTypeAndAction(String entityType, String action) {
        return auditLogRepository.countByEntityTypeAndAction(entityType, action);
    }

    private Query buildQuery(AuditFilterRequest filter) {
        List<Criteria> criteriaList = new ArrayList<>();

        if (filter.getEntityType() != null && !filter.getEntityType().isEmpty()) {
            criteriaList.add(Criteria.where("entityType").is(filter.getEntityType()));
        }
        if (filter.getEntityId() != null) {
            criteriaList.add(Criteria.where("entityId").is(filter.getEntityId()));
        }
        if (filter.getAction() != null && !filter.getAction().isEmpty()) {
            criteriaList.add(Criteria.where("action").is(filter.getAction()));
        }
        if (filter.getCustomerId() != null) {
            criteriaList.add(Criteria.where("customerId").is(filter.getCustomerId()));
        }
        if (filter.getPerformedBy() != null) {
            criteriaList.add(Criteria.where("performedBy").is(filter.getPerformedBy()));
        }
        if (filter.getServiceName() != null && !filter.getServiceName().isEmpty()) {
            criteriaList.add(Criteria.where("serviceName").is(filter.getServiceName()));
        }
        if (filter.getStartDate() != null) {
            criteriaList.add(Criteria.where("timestamp").gte(filter.getStartDate()));
        }
        if (filter.getEndDate() != null) {
            criteriaList.add(Criteria.where("timestamp").lte(filter.getEndDate()));
        }

        Query query = new Query();
        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }
        return query;
    }

    private Map<String, Object> calculateChanges(Map<String, Object> previousState, Map<String, Object> newState) {
        if (previousState == null || newState == null) {
            return null;
        }
        return newState;
    }

    private AuditLogDTO toDTO(AuditLog auditLog) {
        return AuditLogDTO.builder()
                .id(auditLog.getId())
                .eventId(auditLog.getEventId())
                .entityType(auditLog.getEntityType())
                .entityId(auditLog.getEntityId())
                .action(auditLog.getAction())
                .customerId(auditLog.getCustomerId())
                .customerEmail(auditLog.getCustomerEmail())
                .performedBy(auditLog.getPerformedBy())
                .performedByEmail(auditLog.getPerformedByEmail())
                .performedByRole(auditLog.getPerformedByRole())
                .previousState(auditLog.getPreviousState())
                .newState(auditLog.getNewState())
                .changes(auditLog.getChanges())
                .description(auditLog.getDescription())
                .serviceName(auditLog.getServiceName())
                .traceId(auditLog.getTraceId())
                .timestamp(auditLog.getTimestamp())
                .build();
    }
}
