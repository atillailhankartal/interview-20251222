package com.brokage.processor.repository;

import com.brokage.processor.entity.SagaInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SagaInstanceRepository extends JpaRepository<SagaInstance, UUID> {

    Optional<SagaInstance> findByCorrelationId(UUID correlationId);

    Page<SagaInstance> findBySagaType(String sagaType, Pageable pageable);

    Page<SagaInstance> findByStatus(SagaInstance.SagaStatus status, Pageable pageable);

    @Query("SELECT s FROM SagaInstance s WHERE s.status IN ('STARTED', 'IN_PROGRESS') " +
           "AND s.startedAt < :timeout")
    List<SagaInstance> findTimedOutSagas(@Param("timeout") LocalDateTime timeout);

    @Query("SELECT s FROM SagaInstance s WHERE s.status = 'FAILED' AND s.retryCount < s.maxRetries")
    List<SagaInstance> findRetryableSagas();

    @Query("SELECT s FROM SagaInstance s WHERE s.status = 'COMPENSATING'")
    List<SagaInstance> findCompensatingSagas();

    long countByStatus(SagaInstance.SagaStatus status);

    @Query("SELECT s FROM SagaInstance s WHERE s.status IN :statuses")
    Page<SagaInstance> findByStatusIn(@Param("statuses") List<SagaInstance.SagaStatus> statuses, Pageable pageable);

    boolean existsByCorrelationId(UUID correlationId);
}
