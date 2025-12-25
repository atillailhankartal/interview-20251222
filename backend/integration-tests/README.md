# Integration Tests

End-to-end integration tests for the Brokage trading platform.

## Overview

This module contains comprehensive integration tests that verify the complete flow across multiple microservices using Testcontainers.

## Test Categories

### 1. Order Flow Integration Tests (`OrderFlowIntegrationTest`)
- Order creation (BUY/SELL)
- Order retrieval
- Order cancellation
- Order status transitions through saga
- Order listing with pagination

### 2. Asset Balance Integration Tests
- Deposit operations
- Withdraw operations
- Balance blocking on order creation (via CustomerAsset.usableSize)
- Balance release on order cancellation
- Concurrent operations handling

### 3. Full Trading Cycle Integration Tests (`FullTradingCycleIntegrationTest`)
- Complete buy order lifecycle
- Order matching at same price
- Order matching with price crossing
- Partial fills
- Cancel order flow
- Error scenarios
- Batch trading operations

### 4. Kafka Event Flow Tests (`KafkaEventFlowTest`)
- Order event publishing
- Asset event flow
- Saga event flow
- Notification event flow
- Audit event flow
- Event ordering verification

## Prerequisites

- Docker (for Testcontainers)
- Java 17+
- Gradle

## Running Tests

### Using Gradle

```bash
# Run all integration tests
./gradlew :integration-tests:test

# Run specific test class
./gradlew :integration-tests:test --tests "OrderFlowIntegrationTest"

# Run with verbose output
./gradlew :integration-tests:test --info
```

### Using Docker Compose (for external services)

If you prefer running tests against external services:

```bash
# Start infrastructure
docker-compose -f docker-compose.integration.yml up -d

# Run tests with external services
SPRING_PROFILES_ACTIVE=integration ./gradlew :integration-tests:test

# Stop infrastructure
docker-compose -f docker-compose.integration.yml down
```

## Test Infrastructure

### Testcontainers

Tests use Testcontainers to spin up:
- **PostgreSQL 15** - Main database
- **MongoDB 6.0** - Notification and audit storage
- **Kafka** (Confluent) - Event streaming

### Configuration

Test configuration is in `src/test/resources/application-integration.yml`:
- Dynamic property binding for container URLs
- Kafka consumer/producer settings
- Service endpoints

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `ORDER_SERVICE_URL` | `http://localhost:8081` | Order Service URL |
| `ASSET_SERVICE_URL` | `http://localhost:8082` | Asset Service URL |
| `CUSTOMER_SERVICE_URL` | `http://localhost:8083` | Customer Service URL |

## Test Data

### Customer IDs
- `c0000000-0000-0000-0000-000000000001` - Buyer customer
- `c0000000-0000-0000-0000-000000000002` - Seller customer
- `c0000000-0000-0000-0000-000000000003` - General test customer

### Assets
- `AAPL`, `MSFT`, `GOOG` - Stock symbols
- `TRY` - Currency for balance

## Writing New Tests

Extend `BaseIntegrationTest` to get:
- Pre-configured Testcontainers
- REST client helpers
- Authentication token management

```java
@DisplayName("My Integration Test")
public class MyIntegrationTest extends BaseIntegrationTest {

    @Test
    void shouldDoSomething() {
        Response response = createOrder(
            TEST_CUSTOMER_ID,
            "AAPL",
            "BUY",
            "150.00",
            "10"
        );

        response.then()
            .statusCode(201);
    }
}
```

## CI/CD Integration

### GitHub Actions

```yaml
- name: Run integration tests
  run: ./gradlew :integration-tests:test
  env:
    TESTCONTAINERS_RYUK_DISABLED: true
```

### Notes

- Tests may take 2-5 minutes due to container startup
- Ensure Docker daemon is running
- Increase Docker memory if tests fail with OOM
