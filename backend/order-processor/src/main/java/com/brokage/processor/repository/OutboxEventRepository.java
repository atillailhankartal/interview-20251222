package com.brokage.processor.repository;

import com.brokage.processor.entity.OutboxEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnprocessedEvents(Pageable pageable);

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC")
    List<OutboxEvent> findRetryableEvents(@Param("maxRetries") int maxRetries, Pageable pageable);

    Page<OutboxEvent> findByAggregateId(UUID aggregateId, Pageable pageable);

    Page<OutboxEvent> findByEventType(String eventType, Pageable pageable);

    @Modifying
    @Query("DELETE FROM OutboxEvent o WHERE o.processed = true AND o.processedAt < :before")
    int deleteProcessedEventsBefore(@Param("before") LocalDateTime before);

    long countByProcessedFalse();
}
