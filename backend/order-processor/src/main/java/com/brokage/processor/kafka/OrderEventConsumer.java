package com.brokage.processor.kafka;

import com.brokage.common.enums.OrderSide;
import com.brokage.processor.dto.MatchOrderRequest;
import com.brokage.processor.entity.ProcessedEvent;
import com.brokage.processor.repository.ProcessedEventRepository;
import com.brokage.processor.service.MatchingEngineService;
import com.brokage.processor.service.SagaOrchestrator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final MatchingEngineService matchingEngineService;
    private final SagaOrchestrator sagaOrchestrator;
    private final ProcessedEventRepository processedEventRepository;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String ORDER_STATUS_TOPIC = "order-status-updates";

    @KafkaListener(topics = "order-events", groupId = "order-processor")
    @Transactional
    public void consumeOrderEvent(ConsumerRecord<String, String> record) {
        try {
            log.info("Received order event: key={}", record.key());
            Map<String, Object> event = objectMapper.readValue(record.value(), new TypeReference<>() {});

            String eventType = (String) event.get("eventType");
            UUID eventId = parseUUID(event.get("eventId"));

            if (eventId != null && processedEventRepository.existsByEventId(eventId)) {
                log.info("Event {} already processed, skipping", eventId);
                return;
            }

            if (eventType == null) {
                log.warn("Event type is null, skipping");
                return;
            }

            switch (eventType) {
                case "OrderCreatedEvent" -> handleOrderCreated(event, eventId);
                case "OrderCancelledEvent" -> handleOrderCancelled(event, eventId);
                case "AssetReservedEvent" -> handleAssetReserved(event, eventId);
                case "AssetReservationFailedEvent" -> handleAssetReservationFailed(event, eventId);
                default -> log.debug("Ignoring event type: {}", eventType);
            }

            if (eventId != null) {
                processedEventRepository.save(ProcessedEvent.of(
                        eventId, eventType, parseUUID(event.get("orderId"))
                ));
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }

    private void handleOrderCreated(Map<String, Object> event, UUID eventId) {
        UUID orderId = parseUUID(event.get("orderId"));
        UUID customerId = parseUUID(event.get("customerId"));

        log.info("Processing OrderCreatedEvent for order {}", orderId);

        sagaOrchestrator.startSaga(orderId, "ORDER_PROCESSING", event);
        sagaOrchestrator.advanceSaga(orderId, "VALIDATE");

        log.info("Saga started for order {}", orderId);
    }

    private void handleAssetReserved(Map<String, Object> event, UUID eventId) {
        UUID orderId = parseUUID(event.get("orderId"));
        UUID customerId = parseUUID(event.get("customerId"));
        String assetName = (String) event.get("assetName");
        String orderSideStr = (String) event.get("orderSide");
        BigDecimal price = parseBigDecimal(event.get("price"));
        BigDecimal size = parseBigDecimal(event.get("size"));
        Integer tierPriority = parseInteger(event.get("tierPriority"));

        log.info("Processing AssetReservedEvent for order {}", orderId);

        sagaOrchestrator.advanceSaga(orderId, "RESERVE_ASSETS");

        // Notify order-service about status change
        publishOrderStatusUpdate(orderId, "ASSET_RESERVED", null);

        if (assetName != null && orderSideStr != null && price != null && size != null) {
            MatchOrderRequest request = MatchOrderRequest.builder()
                    .orderId(orderId)
                    .customerId(customerId)
                    .assetName(assetName)
                    .orderSide(OrderSide.valueOf(orderSideStr))
                    .price(price)
                    .size(size)
                    .tierPriority(tierPriority)
                    .build();

            matchingEngineService.addToQueue(request);
            sagaOrchestrator.advanceSaga(orderId, "QUEUE_ORDER");

            // Notify order-service that order is confirmed and queued for matching
            publishOrderStatusUpdate(orderId, "ORDER_CONFIRMED", null);
        }
    }

    private void handleAssetReservationFailed(Map<String, Object> event, UUID eventId) {
        UUID orderId = parseUUID(event.get("orderId"));
        String reason = (String) event.getOrDefault("reason", "Asset reservation failed");

        log.info("Processing AssetReservationFailedEvent for order {}", orderId);

        sagaOrchestrator.failSaga(orderId, "RESERVE_ASSETS", reason);

        // Notify order-service about rejection
        publishOrderStatusUpdate(orderId, "REJECTED", reason);
    }

    private void publishOrderStatusUpdate(UUID orderId, String newStatus, String reason) {
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("eventType", "OrderStatusUpdateEvent");
            eventPayload.put("eventId", UUID.randomUUID().toString());
            eventPayload.put("orderId", orderId.toString());
            eventPayload.put("newStatus", newStatus);
            if (reason != null) {
                eventPayload.put("reason", reason);
            }
            eventPayload.put("timestamp", java.time.Instant.now().toString());

            String payload = objectMapper.writeValueAsString(eventPayload);
            kafkaTemplate.send(ORDER_STATUS_TOPIC, orderId.toString(), payload);
            log.info("Published OrderStatusUpdateEvent for order {}, status={}", orderId, newStatus);
        } catch (Exception e) {
            log.error("Failed to publish OrderStatusUpdateEvent for order {}: {}", orderId, e.getMessage());
        }
    }

    private void handleOrderCancelled(Map<String, Object> event, UUID eventId) {
        UUID orderId = parseUUID(event.get("orderId"));
        String reason = (String) event.getOrDefault("reason", "User requested cancellation");

        log.info("Processing OrderCancelledEvent for order {}", orderId);

        matchingEngineService.cancelOrder(orderId, reason);

        sagaOrchestrator.getSaga(orderId).ifPresent(saga -> {
            if (saga.isInProgress()) {
                sagaOrchestrator.failSaga(orderId, "CANCELLED", reason);
            }
        });
    }

    private UUID parseUUID(Object value) {
        if (value == null) return null;
        if (value instanceof UUID) return (UUID) value;
        try {
            return UUID.fromString(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        try {
            return new BigDecimal(value.toString());
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
