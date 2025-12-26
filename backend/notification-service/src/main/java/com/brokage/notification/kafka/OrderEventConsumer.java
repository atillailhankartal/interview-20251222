package com.brokage.notification.kafka;

import com.brokage.notification.service.NotificationService;
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
public class OrderEventConsumer {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "notification-service")
    public void consumeOrderEvent(ConsumerRecord<String, String> record) {
        log.info("Received order event: key={}", record.key());

        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();

            switch (eventType) {
                case "OrderCreatedEvent" -> handleOrderCreated(event);
                case "OrderCanceledEvent" -> handleOrderCanceled(event);
                case "OrderMatchedEvent" -> handleOrderMatched(event);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
        }
    }

    private void handleOrderCreated(JsonNode event) {
        try {
            UUID customerId = UUID.fromString(event.get("customerId").asText());
            UUID orderId = UUID.fromString(event.get("orderId").asText());
            String assetName = event.get("assetName").asText();
            String orderSide = event.get("orderSide").asText();
            String size = event.get("size").asText();

            notificationService.createOrderNotification(
                    customerId, orderId, "CREATED", assetName, orderSide, size);

            log.info("Created notification for order creation: {}", orderId);
        } catch (Exception e) {
            log.error("Error handling OrderCreatedEvent: {}", e.getMessage(), e);
        }
    }

    private void handleOrderCanceled(JsonNode event) {
        try {
            UUID customerId = UUID.fromString(event.get("customerId").asText());
            UUID orderId = UUID.fromString(event.get("orderId").asText());
            String assetName = event.get("assetName").asText();
            String orderSide = event.get("orderSide").asText();
            String size = event.get("size").asText();

            notificationService.createOrderNotification(
                    customerId, orderId, "CANCELED", assetName, orderSide, size);

            log.info("Created notification for order cancellation: {}", orderId);
        } catch (Exception e) {
            log.error("Error handling OrderCanceledEvent: {}", e.getMessage(), e);
        }
    }

    private void handleOrderMatched(JsonNode event) {
        try {
            UUID customerId = UUID.fromString(event.get("customerId").asText());
            UUID orderId = UUID.fromString(event.get("orderId").asText());
            String assetName = event.get("assetName").asText();
            String orderSide = event.get("orderSide").asText();
            String size = event.get("size").asText();

            notificationService.createOrderNotification(
                    customerId, orderId, "MATCHED", assetName, orderSide, size);

            log.info("Created notification for order match: {}", orderId);
        } catch (Exception e) {
            log.error("Error handling OrderMatchedEvent: {}", e.getMessage(), e);
        }
    }
}
