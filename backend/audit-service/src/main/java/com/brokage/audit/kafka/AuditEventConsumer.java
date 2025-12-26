package com.brokage.audit.kafka;

import com.brokage.audit.service.AuditService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "order-events", groupId = "audit-service-orders")
    public void consumeOrderEvent(ConsumerRecord<String, String> record) {
        try {
            log.info("Received order event: key={}", record.key());
            Map<String, Object> event = objectMapper.readValue(record.value(), new TypeReference<>() {});

            String eventType = (String) event.get("eventType");
            if (eventType == null) {
                log.warn("Event type is null, skipping");
                return;
            }

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

    @KafkaListener(topics = "asset-events", groupId = "audit-service-assets")
    public void consumeAssetEvent(ConsumerRecord<String, String> record) {
        try {
            log.info("Received asset event: key={}", record.key());
            Map<String, Object> event = objectMapper.readValue(record.value(), new TypeReference<>() {});

            String eventType = (String) event.get("eventType");
            if (eventType == null) {
                log.warn("Event type is null, skipping");
                return;
            }

            switch (eventType) {
                case "AssetReservedEvent" -> handleAssetReserved(event);
                case "AssetReleasedEvent" -> handleAssetReleased(event);
                case "AssetDebitedEvent" -> handleAssetDebited(event);
                case "AssetCreditedEvent" -> handleAssetCredited(event);
                default -> log.debug("Ignoring event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("Error processing asset event: {}", e.getMessage(), e);
        }
    }

    private void handleOrderCreated(Map<String, Object> event) {
        UUID eventId = parseUUID(event.get("eventId"));
        UUID orderId = parseUUID(event.get("orderId"));
        UUID customerId = parseUUID(event.get("customerId"));

        Map<String, Object> newState = Map.of(
                "orderId", orderId != null ? orderId.toString() : "",
                "assetName", event.getOrDefault("assetName", ""),
                "orderSide", event.getOrDefault("orderSide", ""),
                "size", event.getOrDefault("size", ""),
                "price", event.getOrDefault("price", "")
        );

        auditService.createAuditLog(
                "ORDER", orderId, "CREATED",
                customerId, customerId, "CUSTOMER",
                null, newState,
                "Order created", "order-service", eventId
        );
    }

    private void handleOrderCanceled(Map<String, Object> event) {
        UUID eventId = parseUUID(event.get("eventId"));
        UUID orderId = parseUUID(event.get("orderId"));
        UUID customerId = parseUUID(event.get("customerId"));
        UUID cancelledBy = parseUUID(event.get("cancelledBy"));

        auditService.createAuditLog(
                "ORDER", orderId, "CANCELED",
                customerId, cancelledBy, (String) event.getOrDefault("cancelledByRole", "CUSTOMER"),
                null, Map.of("reason", event.getOrDefault("reason", "User requested")),
                "Order canceled", "order-service", eventId
        );
    }

    private void handleOrderMatched(Map<String, Object> event) {
        UUID eventId = parseUUID(event.get("eventId"));
        UUID orderId = parseUUID(event.get("orderId"));
        UUID customerId = parseUUID(event.get("customerId"));

        Map<String, Object> newState = Map.of(
                "matchedSize", event.getOrDefault("matchedSize", ""),
                "matchedPrice", event.getOrDefault("matchedPrice", ""),
                "tradeId", event.getOrDefault("tradeId", "")
        );

        auditService.createAuditLog(
                "ORDER", orderId, "MATCHED",
                customerId, null, "SYSTEM",
                null, newState,
                "Order matched", "order-processor", eventId
        );
    }

    private void handleAssetReserved(Map<String, Object> event) {
        UUID eventId = parseUUID(event.get("eventId"));
        UUID assetId = parseUUID(event.get("assetId"));
        UUID customerId = parseUUID(event.get("customerId"));
        UUID orderId = parseUUID(event.get("orderId"));

        auditService.createAuditLog(
                "ASSET", assetId, "RESERVED",
                customerId, null, "SYSTEM",
                null, Map.of(
                        "amount", event.getOrDefault("amount", ""),
                        "orderId", orderId != null ? orderId.toString() : ""
                ),
                "Asset reserved for order", "asset-service", eventId
        );
    }

    private void handleAssetReleased(Map<String, Object> event) {
        UUID eventId = parseUUID(event.get("eventId"));
        UUID assetId = parseUUID(event.get("assetId"));
        UUID customerId = parseUUID(event.get("customerId"));

        auditService.createAuditLog(
                "ASSET", assetId, "RELEASED",
                customerId, null, "SYSTEM",
                null, Map.of("amount", event.getOrDefault("amount", "")),
                "Asset reservation released", "asset-service", eventId
        );
    }

    private void handleAssetDebited(Map<String, Object> event) {
        UUID eventId = parseUUID(event.get("eventId"));
        UUID assetId = parseUUID(event.get("assetId"));
        UUID customerId = parseUUID(event.get("customerId"));

        auditService.createAuditLog(
                "ASSET", assetId, "DEBITED",
                customerId, null, "SYSTEM",
                null, Map.of("amount", event.getOrDefault("amount", "")),
                "Asset debited", "asset-service", eventId
        );
    }

    private void handleAssetCredited(Map<String, Object> event) {
        UUID eventId = parseUUID(event.get("eventId"));
        UUID assetId = parseUUID(event.get("assetId"));
        UUID customerId = parseUUID(event.get("customerId"));

        auditService.createAuditLog(
                "ASSET", assetId, "CREDITED",
                customerId, null, "SYSTEM",
                null, Map.of("amount", event.getOrDefault("amount", "")),
                "Asset credited", "asset-service", eventId
        );
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
}
