package com.brokage.processor.service;

import com.brokage.processor.entity.SagaInstance;
import com.brokage.processor.repository.SagaInstanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SagaOrchestrator {

    private final SagaInstanceRepository sagaInstanceRepository;
    private final ObjectMapper objectMapper;

    private static final String ORDER_PROCESSING_SAGA = "ORDER_PROCESSING";
    private static final String STEP_VALIDATE = "VALIDATE";
    private static final String STEP_RESERVE_ASSETS = "RESERVE_ASSETS";
    private static final String STEP_QUEUE_ORDER = "QUEUE_ORDER";
    private static final String STEP_COMPLETE = "COMPLETE";

    @Transactional
    public SagaInstance startSaga(UUID correlationId, String sagaType, Map<String, Object> payload) {
        log.info("Starting saga {} for correlation {}", sagaType, correlationId);

        if (sagaInstanceRepository.existsByCorrelationId(correlationId)) {
            log.warn("Saga already exists for correlation {}", correlationId);
            return sagaInstanceRepository.findByCorrelationId(correlationId).orElse(null);
        }

        try {
            SagaInstance saga = SagaInstance.builder()
                    .correlationId(correlationId)
                    .sagaType(sagaType)
                    .status(SagaInstance.SagaStatus.STARTED)
                    .currentStep(STEP_VALIDATE)
                    .payload(objectMapper.writeValueAsString(payload))
                    .context("{}")
                    .completedSteps("")
                    .startedAt(LocalDateTime.now())
                    .expiresAt(LocalDateTime.now().plusMinutes(5))
                    .build();

            saga = sagaInstanceRepository.save(saga);
            log.info("Saga {} started with ID {}", sagaType, saga.getId());
            return saga;
        } catch (Exception e) {
            log.error("Failed to start saga: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to start saga", e);
        }
    }

    @Transactional
    public void advanceSaga(UUID correlationId, String completedStep) {
        log.info("Advancing saga for correlation {} from step {}", correlationId, completedStep);

        SagaInstance saga = sagaInstanceRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new IllegalStateException("Saga not found: " + correlationId));

        String completedSteps = saga.getCompletedSteps();
        completedSteps = completedSteps.isEmpty() ? completedStep : completedSteps + "," + completedStep;
        saga.setCompletedSteps(completedSteps);

        String nextStep = getNextStep(completedStep);
        if (nextStep != null) {
            saga.setCurrentStep(nextStep);
            saga.setStatus(SagaInstance.SagaStatus.IN_PROGRESS);
        } else {
            saga.markAsCompleted();
            log.info("Saga {} completed successfully", correlationId);
        }

        sagaInstanceRepository.save(saga);
    }

    @Transactional
    public void failSaga(UUID correlationId, String failedStep, String errorMessage) {
        log.error("Failing saga {} at step {}: {}", correlationId, failedStep, errorMessage);

        SagaInstance saga = sagaInstanceRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new IllegalStateException("Saga not found: " + correlationId));

        saga.markAsFailed(failedStep, errorMessage);
        sagaInstanceRepository.save(saga);

        startCompensation(saga);
    }

    @Transactional
    public void startCompensation(SagaInstance saga) {
        log.info("Starting compensation for saga {}", saga.getCorrelationId());
        saga.startCompensation();
        sagaInstanceRepository.save(saga);
    }

    @Transactional
    public void completeSaga(UUID correlationId) {
        log.info("Completing saga {}", correlationId);

        SagaInstance saga = sagaInstanceRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new IllegalStateException("Saga not found: " + correlationId));

        saga.markAsCompleted();
        sagaInstanceRepository.save(saga);
    }

    public Optional<SagaInstance> getSaga(UUID correlationId) {
        return sagaInstanceRepository.findByCorrelationId(correlationId);
    }

    public List<SagaInstance> getTimedOutSagas(LocalDateTime timeout) {
        return sagaInstanceRepository.findTimedOutSagas(timeout);
    }

    public List<SagaInstance> getRetryableSagas() {
        return sagaInstanceRepository.findRetryableSagas();
    }

    @Transactional
    public void retrySaga(UUID correlationId) {
        log.info("Retrying saga {}", correlationId);

        SagaInstance saga = sagaInstanceRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new IllegalStateException("Saga not found: " + correlationId));

        if (!saga.canRetry()) {
            log.warn("Saga {} has exceeded max retries", correlationId);
            return;
        }

        saga.incrementRetry();
        saga.setStatus(SagaInstance.SagaStatus.IN_PROGRESS);
        saga.setErrorMessage(null);
        sagaInstanceRepository.save(saga);
    }

    private String getNextStep(String currentStep) {
        return switch (currentStep) {
            case STEP_VALIDATE -> STEP_RESERVE_ASSETS;
            case STEP_RESERVE_ASSETS -> STEP_QUEUE_ORDER;
            case STEP_QUEUE_ORDER -> STEP_COMPLETE;
            case STEP_COMPLETE -> null;
            default -> null;
        };
    }

    public long countByStatus(SagaInstance.SagaStatus status) {
        return sagaInstanceRepository.countByStatus(status);
    }
}
