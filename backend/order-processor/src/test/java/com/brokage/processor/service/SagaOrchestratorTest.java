package com.brokage.processor.service;

import com.brokage.processor.entity.SagaInstance;
import com.brokage.processor.repository.SagaInstanceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorTest {

    @Mock
    private SagaInstanceRepository sagaInstanceRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private SagaOrchestrator sagaOrchestrator;

    private UUID correlationId;

    @BeforeEach
    void setUp() {
        correlationId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Start Saga Tests")
    class StartSagaTests {

        @Test
        @DisplayName("Should start new saga")
        void shouldStartNewSaga() throws Exception {
            // Given
            Map<String, Object> payload = Map.of("orderId", correlationId.toString());

            when(sagaInstanceRepository.existsByCorrelationId(correlationId)).thenReturn(false);
            when(objectMapper.writeValueAsString(any())).thenReturn("{}");
            when(sagaInstanceRepository.save(any(SagaInstance.class))).thenAnswer(inv -> {
                SagaInstance saga = inv.getArgument(0);
                saga.setId(UUID.randomUUID());
                return saga;
            });

            // When
            SagaInstance saga = sagaOrchestrator.startSaga(correlationId, "ORDER_PROCESSING", payload);

            // Then
            assertThat(saga).isNotNull();
            assertThat(saga.getCorrelationId()).isEqualTo(correlationId);
            assertThat(saga.getSagaType()).isEqualTo("ORDER_PROCESSING");
            assertThat(saga.getStatus()).isEqualTo(SagaInstance.SagaStatus.STARTED);
            verify(sagaInstanceRepository).save(any(SagaInstance.class));
        }

        @Test
        @DisplayName("Should return existing saga for duplicate correlation")
        void shouldReturnExistingSagaForDuplicateCorrelation() {
            // Given
            SagaInstance existingSaga = createSaga(correlationId, SagaInstance.SagaStatus.IN_PROGRESS);

            when(sagaInstanceRepository.existsByCorrelationId(correlationId)).thenReturn(true);
            when(sagaInstanceRepository.findByCorrelationId(correlationId)).thenReturn(Optional.of(existingSaga));

            // When
            SagaInstance saga = sagaOrchestrator.startSaga(correlationId, "ORDER_PROCESSING", Map.of());

            // Then
            assertThat(saga).isEqualTo(existingSaga);
            verify(sagaInstanceRepository, never()).save(any(SagaInstance.class));
        }
    }

    @Nested
    @DisplayName("Advance Saga Tests")
    class AdvanceSagaTests {

        @Test
        @DisplayName("Should advance saga to next step")
        void shouldAdvanceSagaToNextStep() {
            // Given
            SagaInstance saga = createSaga(correlationId, SagaInstance.SagaStatus.STARTED);
            saga.setCurrentStep("VALIDATE");
            saga.setCompletedSteps("");

            when(sagaInstanceRepository.findByCorrelationId(correlationId)).thenReturn(Optional.of(saga));
            when(sagaInstanceRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            sagaOrchestrator.advanceSaga(correlationId, "VALIDATE");

            // Then
            assertThat(saga.getCurrentStep()).isEqualTo("RESERVE_ASSETS");
            assertThat(saga.getCompletedSteps()).contains("VALIDATE");
            assertThat(saga.getStatus()).isEqualTo(SagaInstance.SagaStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("Should complete saga at final step")
        void shouldCompleteSagaAtFinalStep() {
            // Given
            SagaInstance saga = createSaga(correlationId, SagaInstance.SagaStatus.IN_PROGRESS);
            saga.setCurrentStep("COMPLETE");
            saga.setCompletedSteps("VALIDATE,RESERVE_ASSETS,QUEUE_ORDER");

            when(sagaInstanceRepository.findByCorrelationId(correlationId)).thenReturn(Optional.of(saga));
            when(sagaInstanceRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            sagaOrchestrator.advanceSaga(correlationId, "COMPLETE");

            // Then
            assertThat(saga.getStatus()).isEqualTo(SagaInstance.SagaStatus.COMPLETED);
            assertThat(saga.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw exception when saga not found")
        void shouldThrowExceptionWhenSagaNotFound() {
            // Given
            when(sagaInstanceRepository.findByCorrelationId(correlationId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> sagaOrchestrator.advanceSaga(correlationId, "VALIDATE"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Saga not found");
        }
    }

    @Nested
    @DisplayName("Fail Saga Tests")
    class FailSagaTests {

        @Test
        @DisplayName("Should fail saga and start compensation")
        void shouldFailSagaAndStartCompensation() {
            // Given
            SagaInstance saga = createSaga(correlationId, SagaInstance.SagaStatus.IN_PROGRESS);

            when(sagaInstanceRepository.findByCorrelationId(correlationId)).thenReturn(Optional.of(saga));
            when(sagaInstanceRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            sagaOrchestrator.failSaga(correlationId, "RESERVE_ASSETS", "Insufficient funds");

            // Then
            assertThat(saga.getStatus()).isEqualTo(SagaInstance.SagaStatus.COMPENSATING);
            assertThat(saga.getFailedStep()).isEqualTo("RESERVE_ASSETS");
            assertThat(saga.getErrorMessage()).isEqualTo("Insufficient funds");
        }
    }

    @Nested
    @DisplayName("Retry Saga Tests")
    class RetrySagaTests {

        @Test
        @DisplayName("Should retry saga")
        void shouldRetrySaga() {
            // Given
            SagaInstance saga = createSaga(correlationId, SagaInstance.SagaStatus.FAILED);
            saga.setRetryCount(0);
            saga.setMaxRetries(3);

            when(sagaInstanceRepository.findByCorrelationId(correlationId)).thenReturn(Optional.of(saga));
            when(sagaInstanceRepository.save(any(SagaInstance.class))).thenAnswer(inv -> inv.getArgument(0));

            // When
            sagaOrchestrator.retrySaga(correlationId);

            // Then
            assertThat(saga.getRetryCount()).isEqualTo(1);
            assertThat(saga.getStatus()).isEqualTo(SagaInstance.SagaStatus.IN_PROGRESS);
            assertThat(saga.getErrorMessage()).isNull();
        }

        @Test
        @DisplayName("Should not retry saga when max retries exceeded")
        void shouldNotRetrySagaWhenMaxRetriesExceeded() {
            // Given
            SagaInstance saga = createSaga(correlationId, SagaInstance.SagaStatus.FAILED);
            saga.setRetryCount(3);
            saga.setMaxRetries(3);

            when(sagaInstanceRepository.findByCorrelationId(correlationId)).thenReturn(Optional.of(saga));

            // When
            sagaOrchestrator.retrySaga(correlationId);

            // Then
            verify(sagaInstanceRepository, never()).save(any(SagaInstance.class));
        }
    }

    @Nested
    @DisplayName("Query Tests")
    class QueryTests {

        @Test
        @DisplayName("Should get timed out sagas")
        void shouldGetTimedOutSagas() {
            // Given
            LocalDateTime timeout = LocalDateTime.now().minusMinutes(5);
            List<SagaInstance> timedOutSagas = List.of(
                    createSaga(UUID.randomUUID(), SagaInstance.SagaStatus.IN_PROGRESS)
            );

            when(sagaInstanceRepository.findTimedOutSagas(timeout)).thenReturn(timedOutSagas);

            // When
            List<SagaInstance> result = sagaOrchestrator.getTimedOutSagas(timeout);

            // Then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should get retryable sagas")
        void shouldGetRetryableSagas() {
            // Given
            List<SagaInstance> retryableSagas = List.of(
                    createSaga(UUID.randomUUID(), SagaInstance.SagaStatus.FAILED)
            );

            when(sagaInstanceRepository.findRetryableSagas()).thenReturn(retryableSagas);

            // When
            List<SagaInstance> result = sagaOrchestrator.getRetryableSagas();

            // Then
            assertThat(result).hasSize(1);
        }
    }

    private SagaInstance createSaga(UUID correlationId, SagaInstance.SagaStatus status) {
        SagaInstance saga = SagaInstance.builder()
                .correlationId(correlationId)
                .sagaType("ORDER_PROCESSING")
                .status(status)
                .currentStep("VALIDATE")
                .payload("{}")
                .context("{}")
                .completedSteps("")
                .retryCount(0)
                .maxRetries(3)
                .startedAt(LocalDateTime.now())
                .build();
        saga.setId(UUID.randomUUID());
        return saga;
    }
}
