package com.brokage.order.kafka;

import com.brokage.common.enums.OrderStatus;
import com.brokage.order.service.OrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusEventConsumer {

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-status-updates", groupId = "order-service")
    public void consumeOrderStatusUpdate(ConsumerRecord<String, String> record) {
        log.info("Received order status update event: key={}", record.key());

        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();

            switch (eventType) {
                case "OrderStatusUpdateEvent" -> handleOrderStatusUpdate(event);
                case "SagaCompletedEvent" -> handleSagaCompleted(event);
                case "SagaFailedEvent" -> handleSagaFailed(event);
                default -> log.debug("Ignoring event type: {}", eventType);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse status update event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing status update event: {}", e.getMessage(), e);
        }
    }

    private void handleOrderStatusUpdate(JsonNode event) {
        String orderId = event.get("orderId").asText();
        String newStatus = event.get("newStatus").asText();
        String reason = event.has("reason") ? event.get("reason").asText() : null;

        log.info("Processing OrderStatusUpdateEvent: orderId={}, newStatus={}", orderId, newStatus);

        try {
            OrderStatus status = OrderStatus.valueOf(newStatus);
            orderService.updateOrderStatus(UUID.fromString(orderId), status, reason);
        } catch (IllegalArgumentException e) {
            log.error("Invalid order status: {}", newStatus);
        }
    }

    private void handleSagaCompleted(JsonNode event) {
        String orderId = event.get("orderId").asText();
        log.info("Processing SagaCompletedEvent: orderId={}", orderId);

        orderService.updateOrderStatus(UUID.fromString(orderId), OrderStatus.ORDER_CONFIRMED, null);
    }

    private void handleSagaFailed(JsonNode event) {
        String orderId = event.get("orderId").asText();
        String reason = event.has("reason") ? event.get("reason").asText() : "Saga processing failed";

        log.info("Processing SagaFailedEvent: orderId={}, reason={}", orderId, reason);

        orderService.updateOrderStatus(UUID.fromString(orderId), OrderStatus.REJECTED, reason);
    }
}
