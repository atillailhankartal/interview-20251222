package com.brokage.processor.entity;

import com.brokage.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "saga_instances", indexes = {
        @Index(name = "idx_saga_correlation", columnList = "correlation_id"),
        @Index(name = "idx_saga_type", columnList = "saga_type"),
        @Index(name = "idx_saga_status", columnList = "status"),
        @Index(name = "idx_saga_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SagaInstance extends BaseEntity {

    @Column(name = "correlation_id", nullable = false, unique = true)
    private UUID correlationId;

    @Column(name = "saga_type", nullable = false, length = 100)
    private String sagaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SagaStatus status = SagaStatus.STARTED;

    @Column(name = "current_step", nullable = false, length = 100)
    private String currentStep;

    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(name = "context", columnDefinition = "TEXT")
    private String context;

    @Column(name = "completed_steps", columnDefinition = "TEXT")
    private String completedSteps;

    @Column(name = "failed_step", length = 100)
    private String failedStep;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    @Builder.Default
    private Integer maxRetries = 3;

    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    public enum SagaStatus {
        STARTED,
        IN_PROGRESS,
        COMPENSATING,
        COMPLETED,
        FAILED,
        COMPENSATION_FAILED
    }

    public boolean isCompleted() {
        return status == SagaStatus.COMPLETED;
    }

    public boolean isFailed() {
        return status == SagaStatus.FAILED || status == SagaStatus.COMPENSATION_FAILED;
    }

    public boolean isInProgress() {
        return status == SagaStatus.STARTED || status == SagaStatus.IN_PROGRESS;
    }

    public boolean canRetry() {
        return retryCount < maxRetries;
    }

    public void markAsCompleted() {
        this.status = SagaStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void markAsFailed(String step, String error) {
        this.status = SagaStatus.FAILED;
        this.failedStep = step;
        this.errorMessage = error;
        this.completedAt = LocalDateTime.now();
    }

    public void startCompensation() {
        this.status = SagaStatus.COMPENSATING;
    }

    public void incrementRetry() {
        this.retryCount++;
    }
}
