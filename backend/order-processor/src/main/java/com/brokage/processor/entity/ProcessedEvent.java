package com.brokage.processor.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_events", indexes = {
        @Index(name = "idx_processed_event_type", columnList = "event_type"),
        @Index(name = "idx_processed_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "processed_at", nullable = false)
    @Builder.Default
    private LocalDateTime processedAt = LocalDateTime.now();

    @Column(name = "result", length = 2000)
    private String result;

    public static ProcessedEvent of(UUID eventId, String eventType, UUID aggregateId) {
        return ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .processedAt(LocalDateTime.now())
                .build();
    }

    public static ProcessedEvent of(UUID eventId, String eventType, UUID aggregateId, String result) {
        return ProcessedEvent.builder()
                .eventId(eventId)
                .eventType(eventType)
                .aggregateId(aggregateId)
                .processedAt(LocalDateTime.now())
                .result(result)
                .build();
    }
}
