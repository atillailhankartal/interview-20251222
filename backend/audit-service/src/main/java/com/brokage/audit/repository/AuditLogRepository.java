package com.brokage.audit.repository;

import com.brokage.audit.document.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    Optional<AuditLog> findByEventId(UUID eventId);

    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, UUID entityId, Pageable pageable);

    Page<AuditLog> findByCustomerId(UUID customerId, Pageable pageable);

    Page<AuditLog> findByPerformedBy(UUID performedBy, Pageable pageable);

    Page<AuditLog> findByAction(String action, Pageable pageable);

    Page<AuditLog> findByServiceName(String serviceName, Pageable pageable);

    @Query("{'timestamp': {$gte: ?0, $lte: ?1}}")
    Page<AuditLog> findByTimestampBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("{'entityType': ?0, 'action': ?1, 'timestamp': {$gte: ?2, $lte: ?3}}")
    Page<AuditLog> findByEntityTypeAndActionAndTimestampBetween(
            String entityType, String action, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("{'customerId': ?0, 'timestamp': {$gte: ?1, $lte: ?2}}")
    Page<AuditLog> findByCustomerIdAndTimestampBetween(
            UUID customerId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    List<AuditLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, UUID entityId);

    long countByEntityTypeAndAction(String entityType, String action);

    long countByCustomerId(UUID customerId);
}
