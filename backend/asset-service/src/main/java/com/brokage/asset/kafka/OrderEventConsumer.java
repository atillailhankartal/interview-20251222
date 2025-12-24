package com.brokage.asset.kafka;

import com.brokage.asset.service.AssetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventConsumer {

    private final AssetService assetService;
    private final ObjectMapper objectMapper;

    private static final String TRY_SYMBOL = "TRY";

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
        String assetSymbol = event.get("assetSymbol").asText();
        String orderSide = event.get("orderSide").asText();
        BigDecimal size = new BigDecimal(event.get("size").asText());
        BigDecimal totalValue = new BigDecimal(event.get("totalValue").asText());
        String orderId = event.get("orderId").asText();

        log.info("Processing OrderCreatedEvent: orderId={}, customerId={}, side={}, asset={}, size={}, totalValue={}",
                orderId, customerId, orderSide, assetSymbol, size, totalValue);

        try {
            if ("BUY".equals(orderSide)) {
                // BUY: Reserve TRY (money)
                assetService.reserveAsset(customerId, TRY_SYMBOL, totalValue);
                log.info("Reserved {} TRY for BUY order {}", totalValue, orderId);
            } else if ("SELL".equals(orderSide)) {
                // SELL: Reserve asset
                assetService.reserveAsset(customerId, assetSymbol, size);
                log.info("Reserved {} {} for SELL order {}", size, assetSymbol, orderId);
            }
        } catch (Exception e) {
            log.error("Failed to reserve balance for order {}: {}", orderId, e.getMessage());
            // TODO: Send rejection event back to Order Service
        }
    }

    /**
     * Handle OrderCancelledEvent - Release blocked balance
     */
    private void handleOrderCancelled(JsonNode event) {
        UUID customerId = UUID.fromString(event.get("customerId").asText());
        String assetSymbol = event.get("assetSymbol").asText();
        String orderSide = event.get("orderSide").asText();
        BigDecimal size = new BigDecimal(event.get("size").asText());
        BigDecimal totalValue = new BigDecimal(event.get("totalValue").asText());
        String orderId = event.get("orderId").asText();

        log.info("Processing OrderCancelledEvent: orderId={}, customerId={}, side={}",
                orderId, customerId, orderSide);

        try {
            if ("BUY".equals(orderSide)) {
                // Release blocked TRY
                assetService.releaseReservation(customerId, TRY_SYMBOL, totalValue);
                log.info("Released {} TRY for cancelled BUY order {}", totalValue, orderId);
            } else if ("SELL".equals(orderSide)) {
                // Release blocked asset
                assetService.releaseReservation(customerId, assetSymbol, size);
                log.info("Released {} {} for cancelled SELL order {}", size, assetSymbol, orderId);
            }
        } catch (Exception e) {
            log.error("Failed to release balance for cancelled order {}: {}", orderId, e.getMessage());
        }
    }

    /**
     * Handle OrderMatchedEvent - Settle transaction
     * BUY: Deduct TRY (blocked), Add asset
     * SELL: Deduct asset (blocked), Add TRY
     */
    private void handleOrderMatched(JsonNode event) {
        UUID customerId = UUID.fromString(event.get("customerId").asText());
        String assetSymbol = event.get("assetSymbol").asText();
        String orderSide = event.get("orderSide").asText();
        BigDecimal size = new BigDecimal(event.get("size").asText());
        BigDecimal totalValue = new BigDecimal(event.get("totalValue").asText());
        String orderId = event.get("orderId").asText();

        log.info("Processing OrderMatchedEvent: orderId={}, customerId={}, side={}, asset={}, size={}, totalValue={}",
                orderId, customerId, orderSide, assetSymbol, size, totalValue);

        try {
            if ("BUY".equals(orderSide)) {
                // BUY matched: Deduct blocked TRY, Add asset
                assetService.settleTransaction(customerId, TRY_SYMBOL, totalValue, assetSymbol, size);
                log.info("Settled BUY order {}: -{} TRY, +{} {}", orderId, totalValue, size, assetSymbol);
            } else if ("SELL".equals(orderSide)) {
                // SELL matched: Deduct blocked asset, Add TRY
                assetService.settleTransaction(customerId, assetSymbol, size, TRY_SYMBOL, totalValue);
                log.info("Settled SELL order {}: -{} {}, +{} TRY", orderId, size, assetSymbol, totalValue);
            }
        } catch (Exception e) {
            log.error("Failed to settle order {}: {}", orderId, e.getMessage());
            // TODO: Saga compensation logic
        }
    }
}
