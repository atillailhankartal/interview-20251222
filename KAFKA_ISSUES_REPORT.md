# Kafka & Event Flow Issues Report

**Date**: 2025-12-26
**Tested By**: Automated Testing

---

## Summary

| Category | Issues Found |
|----------|-------------|
| Critical | 2 |
| Major | 2 |
| Minor | 1 |

---

## Critical Issues

### ISSUE-001: Asset Service Outbox Events Missing `eventType` Field

**Severity**: CRITICAL
**Component**: `asset-service`
**File**: `AssetService.java:296-302`

**Description**:
Asset service creates outbox events with payloads that don't include the `eventType` field. The `eventType` is stored in the OutboxEvent entity but NOT serialized into the JSON payload.

**Evidence**:
```sql
SELECT event_type, payload::json->>'eventType' FROM outbox_events;
-- AssetReservedEvent: NULL
-- SettlementCompletedEvent: NULL
-- OrderCreatedEvent: OrderCreatedEvent (correct)
```

**Impact**:
- Audit service skips these events: `"Event type is null, skipping"`
- Notification service fails: `Cannot invoke ... because return value of "JsonNode.get(String)" is null`

**Root Cause**:
```java
// asset-service/AssetService.java:296-302
private void createOutboxEvent(String eventType, UUID aggregateId, Map<String, String> payload) {
    OutboxEvent event = OutboxEvent.builder()
        .eventType(eventType)  // Stored in entity
        .payload(objectMapper.writeValueAsString(payload))  // But NOT in payload!
        ...
}
```

**Fix Required**:
Add `eventType` to the payload map before serialization:
```java
Map<String, String> enrichedPayload = new HashMap<>(payload);
enrichedPayload.put("eventType", eventType);
.payload(objectMapper.writeValueAsString(enrichedPayload))
```

---

### ISSUE-002: Order-Processor Outbox Publisher Uses Wrong Topic

**Severity**: CRITICAL
**Component**: `order-processor`
**File**: `OutboxPublisher.java:42-43`

**Description**:
The order-processor's OutboxPublisher reads the topic from `event.getTopic()` which contains values like `order.matched`, `order.created`. But consumers subscribe to `order-events`.

**Evidence**:
```
Topics in outbox_events table:
- order.created (3 messages)
- order.matched (3 messages)
- asset.assetreserved (1 message)

Consumers subscribe to:
- order-events (66 messages)
- asset-events (0 messages)
```

**Impact**:
- Events published by order-processor to specific topics are NOT consumed
- Audit, notification, and other downstream services miss these events

**Root Cause**:
```java
// order-processor/OutboxPublisher.java
kafkaTemplate.send(
    event.getTopic(),  // Uses stored topic (order.matched, etc.)
    ...
);

// vs order-service/OutboxPublisher.java
kafkaTemplate.send(ORDER_EVENTS_TOPIC, key, value);  // Always uses "order-events"
```

**Fix Required**:
Either:
1. Change order-processor to use hardcoded `order-events` topic like order-service
2. Or update consumers to subscribe to multiple topics

---

## Major Issues

### ISSUE-003: Notification Service Consumer NullPointerException

**Severity**: MAJOR
**Component**: `notification-service`
**File**: `OrderEventConsumer.java:28`

**Description**:
Notification service throws NPE when `eventType` field is missing from event payload.

**Log Evidence**:
```
ERROR c.b.n.kafka.OrderEventConsumer - Error processing order event:
Cannot invoke "JsonNode.asText()" because return value of "JsonNode.get(String)" is null
```

**Fix Required**:
Add null check before accessing `eventType`:
```java
JsonNode eventTypeNode = event.get("eventType");
if (eventTypeNode == null) {
    log.warn("Event type is null, skipping");
    return;
}
String eventType = eventTypeNode.asText();
```

---

### ISSUE-004: Order Processor Consumer Silently Skips Events

**Severity**: MAJOR
**Component**: `order-processor`
**File**: `OrderEventConsumer.java`

**Description**:
When order-processor receives an event without `eventType`, it logs a warning and skips. This is correct behavior but means events are lost silently.

**Log Evidence**:
```
INFO  Received order event: key=b4df8aec-99a4-46b0-980f-408aeb39927c
WARN  Event type is null, skipping
```

**Impact**:
- AssetReservedEvent from asset-service is skipped
- Order matching never starts for that order

---

## Minor Issues

### ISSUE-005: Outbox Retry Mechanism Logs Errors

**Severity**: MINOR
**Component**: Multiple services

**Description**:
5 events in outbox had `Send failed` errors before being successfully retried.

**Evidence**:
```sql
SELECT COUNT(*) FROM outbox_events WHERE error_message IS NOT NULL;
-- 5 rows with "Send failed" error but retry_count=1 and processed=true
```

**Impact**:
- No data loss (retries succeeded)
- But indicates occasional Kafka connectivity issues during bursts

---

## Test Results Summary

### Kafka Infrastructure
- [x] Topics created correctly (18 topics)
- [x] Consumer groups active (7 groups)
- [x] No message lag (LAG=0 for all partitions)
- [x] Outbox pattern working (45/45 events processed)

### Event Flow Testing
- [x] OrderCreatedEvent: Published correctly with eventType
- [x] OrderMatchedEvent: Published correctly with eventType
- [x] AssetReservedEvent: **FAILED** - missing eventType in payload
- [x] SettlementCompletedEvent: **FAILED** - missing eventType in payload

### Consumer Testing
- Audit Service: **PARTIAL** - Skips asset events (Issue-001)
- Notification Service: **PARTIAL** - NPE on asset events (Issue-003)
- Order Processor: **PARTIAL** - Skips asset events (Issue-004)
- Asset Service: Working (direct Kafka publish with eventType)

---

## Recommended Priority

1. **Immediate**: Fix ISSUE-001 (asset-service payload missing eventType)
2. **High**: Fix ISSUE-002 (topic mismatch in order-processor)
3. **Medium**: Fix ISSUE-003 (notification service null handling)
4. **Low**: ISSUE-005 monitoring only

---

## Files Requiring Changes

| Service | File | Issue |
|---------|------|-------|
| asset-service | `AssetService.java` | ISSUE-001 |
| order-processor | `OutboxPublisher.java` | ISSUE-002 |
| notification-service | `OrderEventConsumer.java` | ISSUE-003 |

---

## Integration Test Issues

### ISSUE-006: Integration Tests Configuration Mismatch

**Severity**: MAJOR
**Component**: `integration-tests`
**File**: `BaseIntegrationTest.java`

**Description**:
Integration tests have hardcoded credentials that don't match the actual Keycloak configuration.

**Mismatches**:
| Configuration | Test Value | Actual Value |
|---------------|-----------|--------------|
| CLIENT_ID | `brokage-api` | `brokage-web` |
| ADMIN_USERNAME | `admin` | `nick.fury` |
| CUSTOMER_USERNAME | `customer1` | `peter.parker` (or similar) |
| BROKER_USERNAME | `broker1` | `tony.stark` |

**Impact**:
- All 34 integration tests fail
- Cannot authenticate with Keycloak

**Fix Required**:
Update `BaseIntegrationTest.java` credentials to match realm-export.json

---

## Stress Test Notes

### k6 Stress Tests
**Location**: `stress-tests/k6/`

**Prerequisites**:
- k6 must be installed: `brew install k6`
- Services must be running

**Available Tests**:
- `order-flow` - Order creation and cancellation
- `asset-operations` - Deposit, withdraw, balance checks
- `full-trading` - Complete trading scenario
- `spike` - Spike test (sudden load)

**Port Configuration Issues**:
The stress test scripts expect default ports (8081, 8082) but Docker services use mapped ports (7081, 7082).

**Run Command**:
```bash
cd stress-tests/k6
ORDER_SERVICE_URL=http://localhost:7081 \
ASSET_SERVICE_URL=http://localhost:7082 \
KEYCLOAK_URL=http://localhost:8180 \
./run.sh order-flow
```

---

## Testing Status Summary

| Test Type | Status | Issues |
|-----------|--------|--------|
| Kafka Topics | PASS | None |
| Outbox Pattern | PASS | 5 retried events |
| Event Consumers | PARTIAL | Issues 001-004 |
| Integration Tests | FAIL | Issue 006 |
| Stress Tests | NOT RUN | k6 not installed |
