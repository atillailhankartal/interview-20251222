package com.brokage.audit.service;

import com.brokage.audit.document.AuditLog;
import com.brokage.audit.dto.AuditFilterRequest;
import com.brokage.audit.dto.AuditLogDTO;
import com.brokage.audit.repository.AuditLogRepository;
import com.brokage.common.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private AuditService auditService;

    private UUID customerId;
    private UUID entityId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        entityId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Create Audit Log Tests")
    class CreateAuditLogTests {

        @Test
        @DisplayName("Should create audit log successfully")
        void shouldCreateAuditLogSuccessfully() {
            // Given
            AuditLog savedLog = createAuditLog("log-1", "ORDER", entityId, "CREATED");

            when(auditLogRepository.findByEventId(any())).thenReturn(Optional.empty());
            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);

            // When
            AuditLogDTO result = auditService.createAuditLog(
                    "ORDER", entityId, "CREATED",
                    customerId, customerId, "CUSTOMER",
                    null, Map.of("orderId", entityId.toString()),
                    "Order created", "order-service", eventId
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getEntityType()).isEqualTo("ORDER");
            assertThat(result.getAction()).isEqualTo("CREATED");
            verify(auditLogRepository).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Should return existing audit log for duplicate eventId")
        void shouldReturnExistingAuditLogForDuplicateEventId() {
            // Given
            AuditLog existingLog = createAuditLog("log-1", "ORDER", entityId, "CREATED");
            existingLog.setEventId(eventId);

            when(auditLogRepository.findByEventId(eventId)).thenReturn(Optional.of(existingLog));

            // When
            AuditLogDTO result = auditService.createAuditLog(
                    "ORDER", entityId, "CREATED",
                    customerId, customerId, "CUSTOMER",
                    null, null,
                    "Order created", "order-service", eventId
            );

            // Then
            assertThat(result).isNotNull();
            verify(auditLogRepository, never()).save(any(AuditLog.class));
        }

        @Test
        @DisplayName("Should create audit log with null eventId")
        void shouldCreateAuditLogWithNullEventId() {
            // Given
            AuditLog savedLog = createAuditLog("log-1", "ASSET", entityId, "DEBITED");

            when(auditLogRepository.save(any(AuditLog.class))).thenReturn(savedLog);

            // When
            AuditLogDTO result = auditService.createAuditLog(
                    "ASSET", entityId, "DEBITED",
                    customerId, null, "SYSTEM",
                    null, Map.of("amount", "100"),
                    "Asset debited", "asset-service", null
            );

            // Then
            assertThat(result).isNotNull();
            verify(auditLogRepository).save(any(AuditLog.class));
        }
    }

    @Nested
    @DisplayName("Get Audit Log Tests")
    class GetAuditLogTests {

        @Test
        @DisplayName("Should get audit log by ID")
        void shouldGetAuditLogById() {
            // Given
            String logId = "log-123";
            AuditLog auditLog = createAuditLog(logId, "ORDER", entityId, "MATCHED");

            when(auditLogRepository.findById(logId)).thenReturn(Optional.of(auditLog));

            // When
            AuditLogDTO result = auditService.getAuditLogById(logId);

            // Then
            assertThat(result.getId()).isEqualTo(logId);
            assertThat(result.getAction()).isEqualTo("MATCHED");
        }

        @Test
        @DisplayName("Should throw exception when audit log not found")
        void shouldThrowExceptionWhenAuditLogNotFound() {
            // Given
            String logId = "non-existent";
            when(auditLogRepository.findById(logId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> auditService.getAuditLogById(logId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get Entity Audit Trail Tests")
    class GetEntityAuditTrailTests {

        @Test
        @DisplayName("Should get entity audit trail")
        void shouldGetEntityAuditTrail() {
            // Given
            List<AuditLog> logs = List.of(
                    createAuditLog("log-1", "ORDER", entityId, "CREATED"),
                    createAuditLog("log-2", "ORDER", entityId, "MATCHED")
            );
            Page<AuditLog> logPage = new PageImpl<>(logs);

            when(auditLogRepository.findByEntityTypeAndEntityId(
                    eq("ORDER"), eq(entityId), any(Pageable.class)))
                    .thenReturn(logPage);

            // When
            var result = auditService.getEntityAuditTrail("ORDER", entityId, 0, 10);

            // Then
            assertThat(result.getContent()).hasSize(2);
            assertThat(result.getContent().get(0).getAction()).isEqualTo("CREATED");
        }
    }

    @Nested
    @DisplayName("Get Customer Audit Logs Tests")
    class GetCustomerAuditLogsTests {

        @Test
        @DisplayName("Should get customer audit logs")
        void shouldGetCustomerAuditLogs() {
            // Given
            List<AuditLog> logs = List.of(
                    createAuditLog("log-1", "ORDER", entityId, "CREATED"),
                    createAuditLog("log-2", "ASSET", UUID.randomUUID(), "DEBITED")
            );
            Page<AuditLog> logPage = new PageImpl<>(logs);

            when(auditLogRepository.findByCustomerId(eq(customerId), any(Pageable.class)))
                    .thenReturn(logPage);

            // When
            var result = auditService.getCustomerAuditLogs(customerId, 0, 10);

            // Then
            assertThat(result.getContent()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Get Audit Logs with Filter Tests")
    class GetAuditLogsWithFilterTests {

        @Test
        @DisplayName("Should get audit logs with filters")
        void shouldGetAuditLogsWithFilters() {
            // Given
            AuditFilterRequest filter = AuditFilterRequest.builder()
                    .entityType("ORDER")
                    .action("CREATED")
                    .page(0)
                    .size(10)
                    .sortBy("timestamp")
                    .sortDirection("DESC")
                    .build();

            List<AuditLog> logs = List.of(
                    createAuditLog("log-1", "ORDER", entityId, "CREATED")
            );

            when(mongoTemplate.find(any(Query.class), eq(AuditLog.class))).thenReturn(logs);
            when(mongoTemplate.count(any(Query.class), eq(AuditLog.class))).thenReturn(1L);

            // When
            var result = auditService.getAuditLogs(filter);

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getEntityType()).isEqualTo("ORDER");
        }

        @Test
        @DisplayName("Should get audit logs with date range")
        void shouldGetAuditLogsWithDateRange() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(7);
            LocalDateTime endDate = LocalDateTime.now();

            AuditFilterRequest filter = AuditFilterRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .page(0)
                    .size(10)
                    .build();

            when(mongoTemplate.find(any(Query.class), eq(AuditLog.class))).thenReturn(List.of());
            when(mongoTemplate.count(any(Query.class), eq(AuditLog.class))).thenReturn(0L);

            // When
            var result = auditService.getAuditLogs(filter);

            // Then
            assertThat(result.getContent()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Count Tests")
    class CountTests {

        @Test
        @DisplayName("Should count by entity type and action")
        void shouldCountByEntityTypeAndAction() {
            // Given
            when(auditLogRepository.countByEntityTypeAndAction("ORDER", "CREATED")).thenReturn(10L);

            // When
            long count = auditService.countByEntityTypeAndAction("ORDER", "CREATED");

            // Then
            assertThat(count).isEqualTo(10L);
        }
    }

    private AuditLog createAuditLog(String id, String entityType, UUID entityId, String action) {
        return AuditLog.builder()
                .id(id)
                .eventId(UUID.randomUUID())
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .customerId(customerId)
                .performedBy(customerId)
                .performedByRole("CUSTOMER")
                .serviceName("test-service")
                .timestamp(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
