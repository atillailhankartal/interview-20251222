package com.brokage.asset.repository;

import com.brokage.asset.entity.OutboxEvent;
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

    List<OutboxEvent> findByProcessedFalseOrderByCreatedAtAsc();

    @Query("SELECT o FROM OutboxEvent o WHERE o.processed = false AND o.retryCount < :maxRetries ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnprocessedEvents(@Param("maxRetries") int maxRetries);

    @Modifying
    @Query("UPDATE OutboxEvent o SET o.processed = true, o.processedAt = :processedAt WHERE o.id = :id")
    void markAsProcessed(@Param("id") UUID id, @Param("processedAt") LocalDateTime processedAt);
}
