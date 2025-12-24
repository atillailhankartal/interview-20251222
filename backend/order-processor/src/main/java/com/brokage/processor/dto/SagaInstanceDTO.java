package com.brokage.processor.dto;

import com.brokage.processor.entity.SagaInstance.SagaStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaInstanceDTO {

    private UUID id;
    private UUID correlationId;
    private String sagaType;
    private SagaStatus status;
    private String currentStep;
    private String completedSteps;
    private String failedStep;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
