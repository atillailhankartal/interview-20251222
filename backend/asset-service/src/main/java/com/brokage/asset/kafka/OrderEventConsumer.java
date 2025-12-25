package com.brokage.asset.kafka;

import com.brokage.asset.service.AssetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final AssetService assetService;
    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String TRY_ASSET = "TRY";
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    @KafkaListener(topics = "order-events", groupId = "asset-service")
    public void consumeOrderEvent(ConsumerRecord<String, String> record) {
        log.info("Received order event: key={}, partition={}, offset={}",
                record.key(), record.partition(), record.offset());

        try {
            JsonNode event = objectMapper.readTree(record.value());
            String eventType = event.get("eventType").asText();

            switch (eventType) {
                case "OrderCreatedEvent" -> handleOrderCreated(event);
                case "OrderCancelledEvent" -> handleOrderCancelled(event);
                case "OrderMatchedEvent" -> handleOrderMatched(event);
                default -> log.warn("Unknown event type: {}", eventType);
            }

        } catch (JsonProcessingException e) {
            log.error("Failed to parse order event: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing order event: {}", e.getMessage(), e);
            throw e; // Rethrow for retry mechanism
        }
    }

    /**
     * Handle OrderCreatedEvent - Reserve balance
     * BUY order: Block TRY (totalValue)
     * SELL order: Block asset (size)
     */
    private void handleOrderCreated(JsonNode event) {
        UUID customerId = UUID.fromString(event.get("customerId").asText());
        String assetName = event.get("assetName").asText();
        String orderSide = event.get("orderSide").asText();
        BigDecimal size = new BigDecimal(event.get("size").asText());
        BigDecimal totalValue = new BigDecimal(event.get("totalValue").asText());
        String orderId = event.get("orderId").asText();
        BigDecimal price = event.has("price") ? new BigDecimal(event.get("price").asText()) : BigDecimal.ZERO;
        Integer tierPriority = event.has("tierPriority") ? event.get("tierPriority").asInt() : 0;

        log.info("Processing OrderCreatedEvent: orderId={}, customerId={}, side={}, asset={}, size={}, totalValue={}",
                orderId, customerId, orderSide, assetName, size, totalValue);

        try {
            if ("BUY".equals(orderSide)) {
                // BUY: Reserve TRY (money)
                assetService.reserveAsset(customerId, TRY_ASSET, totalValue);
                log.info("Reserved {} TRY for BUY order {}", totalValue, orderId);
            } else if ("SELL".equals(orderSide)) {
                // SELL: Reserve asset
                assetService.reserveAsset(customerId, assetName, size);
                log.info("Reserved {} {} for SELL order {}", size, assetName, orderId);
            }

            // Publish AssetReservedEvent to order-events topic for saga to continue
            publishAssetReservedEvent(orderId, customerId.toString(), assetName, orderSide,
                    price.toString(), size.toString(), tierPriority);

        } catch (Exception e) {
            log.error("Failed to reserve balance for order {}: {}", orderId, e.getMessage());
            // Publish rejection event
            publishAssetReservationFailedEvent(orderId, customerId.toString(), e.getMessage());
        }
    }

    private void publishAssetReservedEvent(String orderId, String customerId, String assetName,
                                            String orderSide, String price, String size, Integer tierPriority) {
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("eventType", "AssetReservedEvent");
            eventPayload.put("eventId", UUID.randomUUID().toString());
            eventPayload.put("orderId", orderId);
            eventPayload.put("customerId", customerId);
            eventPayload.put("assetName", assetName);
            eventPayload.put("orderSide", orderSide);
            eventPayload.put("price", price);
            eventPayload.put("size", size);
            eventPayload.put("tierPriority", tierPriority);
            eventPayload.put("timestamp", java.time.Instant.now().toString());

            String payload = objectMapper.writeValueAsString(eventPayload);
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, orderId, payload);
            log.info("Published AssetReservedEvent for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish AssetReservedEvent for order {}: {}", orderId, e.getMessage());
        }
    }

    private void publishAssetReservationFailedEvent(String orderId, String customerId, String reason) {
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("eventType", "AssetReservationFailedEvent");
            eventPayload.put("eventId", UUID.randomUUID().toString());
            eventPayload.put("orderId", orderId);
            eventPayload.put("customerId", customerId);
            eventPayload.put("reason", reason);
            eventPayload.put("timestamp", java.time.Instant.now().toString());

            String payload = objectMapper.writeValueAsString(eventPayload);
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, orderId, payload);
            log.info("Published AssetReservationFailedEvent for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish AssetReservationFailedEvent for order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Handle OrderCancelledEvent - Release blocked balance
     */
    private void handleOrderCancelled(JsonNode event) {
        UUID customerId = UUID.fromString(event.get("customerId").asText());
        String assetName = event.get("assetName").asText();
        String orderSide = event.get("orderSide").asText();
        BigDecimal size = new BigDecimal(event.get("size").asText());
        BigDecimal totalValue = new BigDecimal(event.get("totalValue").asText());
        String orderId = event.get("orderId").asText();

        log.info("Processing OrderCancelledEvent: orderId={}, customerId={}, side={}",
                orderId, customerId, orderSide);

        try {
            if ("BUY".equals(orderSide)) {
                // Release blocked TRY
                assetService.releaseReservation(customerId, TRY_ASSET, totalValue);
                log.info("Released {} TRY for cancelled BUY order {}", totalValue, orderId);
            } else if ("SELL".equals(orderSide)) {
                // Release blocked asset
                assetService.releaseReservation(customerId, assetName, size);
                log.info("Released {} {} for cancelled SELL order {}", size, assetName, orderId);
            }
        } catch (Exception e) {
            log.error("Failed to release balance for cancelled order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Handle OrderMatchedEvent - Settle transaction
     * BUY: Deduct TRY (blocked), Add asset
     * SELL: Deduct asset (blocked), Add TRY
     *
     * For partial fills, use filledSize/filledValue if provided,
     * otherwise use full order size/totalValue (full match scenario per PDF).
     */
    private void handleOrderMatched(JsonNode event) {
        UUID customerId = UUID.fromString(event.get("customerId").asText());
        String assetName = event.get("assetName").asText();
        String orderSide = event.get("orderSide").asText();
        String orderId = event.get("orderId").asText();

        // Support for partial fills: use filledSize if available, else use full size
        BigDecimal matchedSize = event.has("filledSize")
                ? new BigDecimal(event.get("filledSize").asText())
                : new BigDecimal(event.get("size").asText());

        BigDecimal matchedValue = event.has("filledValue")
                ? new BigDecimal(event.get("filledValue").asText())
                : new BigDecimal(event.get("totalValue").asText());

        log.info("Processing OrderMatchedEvent: orderId={}, customerId={}, side={}, asset={}, matchedSize={}, matchedValue={}",
                orderId, customerId, orderSide, assetName, matchedSize, matchedValue);

        try {
            if ("BUY".equals(orderSide)) {
                // BUY matched: Deduct blocked TRY, Add asset
                assetService.settleTransaction(customerId, TRY_ASSET, matchedValue, assetName, matchedSize);
                log.info("Settled BUY order {}: -{} TRY, +{} {}", orderId, matchedValue, matchedSize, assetName);
            } else if ("SELL".equals(orderSide)) {
                // SELL matched: Deduct blocked asset, Add TRY
                assetService.settleTransaction(customerId, assetName, matchedSize, TRY_ASSET, matchedValue);
                log.info("Settled SELL order {}: -{} {}, +{} TRY", orderId, matchedSize, assetName, matchedValue);
            }

            // Publish settlement success event
            publishSettlementCompletedEvent(orderId, customerId.toString(), assetName, orderSide,
                    matchedSize.toString(), matchedValue.toString());

        } catch (Exception e) {
            log.error("Failed to settle order {}: {}", orderId, e.getMessage());
            // Saga compensation: publish failure event for order service to handle rollback
            publishSettlementFailedEvent(orderId, customerId.toString(), e.getMessage());
        }
    }

    private void publishSettlementCompletedEvent(String orderId, String customerId, String assetName,
                                                   String orderSide, String matchedSize, String matchedValue) {
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("eventType", "SettlementCompletedEvent");
            eventPayload.put("eventId", UUID.randomUUID().toString());
            eventPayload.put("orderId", orderId);
            eventPayload.put("customerId", customerId);
            eventPayload.put("assetName", assetName);
            eventPayload.put("orderSide", orderSide);
            eventPayload.put("matchedSize", matchedSize);
            eventPayload.put("matchedValue", matchedValue);
            eventPayload.put("timestamp", java.time.Instant.now().toString());

            String payload = objectMapper.writeValueAsString(eventPayload);
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, orderId, payload);
            log.info("Published SettlementCompletedEvent for order {}", orderId);
        } catch (Exception e) {
            log.error("Failed to publish SettlementCompletedEvent for order {}: {}", orderId, e.getMessage());
        }
    }

    private void publishSettlementFailedEvent(String orderId, String customerId, String reason) {
        try {
            Map<String, Object> eventPayload = new HashMap<>();
            eventPayload.put("eventType", "SettlementFailedEvent");
            eventPayload.put("eventId", UUID.randomUUID().toString());
            eventPayload.put("orderId", orderId);
            eventPayload.put("customerId", customerId);
            eventPayload.put("reason", reason);
            eventPayload.put("timestamp", java.time.Instant.now().toString());

            String payload = objectMapper.writeValueAsString(eventPayload);
            kafkaTemplate.send(ORDER_EVENTS_TOPIC, orderId, payload);
            log.info("Published SettlementFailedEvent for order {} - Saga compensation triggered", orderId);
        } catch (Exception e) {
            log.error("Failed to publish SettlementFailedEvent for order {}: {}", orderId, e.getMessage());
        }
    }
}
