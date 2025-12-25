# Brokage Firm - Trading Platform Backend

A microservices-based trading platform for stock brokerage operations.

## About This Implementation

This project approaches the case study with a **production-ready mindset**, leveraging modern development practices to deliver a comprehensive solution.

### PDF Compliance

All core requirements from the PDF are fully implemented:

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| POST /api/orders (Create) | ✅ | OrderController.createOrder() |
| GET /api/orders (List) | ✅ | OrderController.listOrders() with filters |
| DELETE /api/orders/{id} (Cancel) | ✅ | OrderController.cancelOrder() |
| GET /api/assets (List) | ✅ | AssetController.getCustomerAssets() |
| Admin Authorization | ✅ | JWT/Keycloak (enhanced from Basic Auth) |
| TRY as Asset (not separate table) | ✅ | CustomerAsset with assetName="TRY" |
| size/usableSize balance logic | ✅ | blockAmount(), releaseBlockedAmount(), deductBlockedAmount() |
| Unit Tests | ✅ | 28+ test files |
| **Bonus: Customer Auth** | ✅ | Keycloak with role-based access |
| **Bonus: Match Endpoint** | ✅ | POST /api/orders/{id}/match (Admin only) |

### Design Rationale

**Why microservices instead of a monolith?**

The PDF allows H2 and a simple monolith. This implementation uses microservices to demonstrate:
- Real-world distributed system patterns (Saga, Outbox, Event Sourcing)
- Production-grade infrastructure (Kafka, PostgreSQL, Keycloak)
- How I would architect a scalable trading platform

**For a simpler requirement set**, a modular monolith with Spring Boot + H2 would be the pragmatic choice.

**Asset vs CustomerAsset separation:**

The PDF specifies a single `Asset` table. We use `CustomerAsset` which contains exactly the same fields (`customerId, assetName, size, usableSize`). The separation from master `Asset` data follows 3NF normalization - a standard database design practice that prevents data redundancy.

## Architecture Overview

This system consists of 6 microservices:

| Service | Port | Description |
|---------|------|-------------|
| **order-service** | 8081 | Order CRUD, saga initiation, cancellation |
| **asset-service** | 8082 | Portfolio management, balance blocking, deposit/withdraw |
| **customer-service** | 8083 | Customer CRUD, tier management, Keycloak integration |
| **order-processor** | 8084 | Matching engine, saga orchestrator |
| **notification-service** | 8085 | Multi-channel notifications (MongoDB) |
| **audit-service** | 8086 | Audit logging and compliance (MongoDB) |

### Key Patterns

- **Event-Driven Architecture**: Kafka for asynchronous messaging
- **Saga Pattern**: Distributed transaction management for order lifecycle
- **Outbox Pattern**: Atomic event publishing with transactional guarantees
- **CQRS**: Command/Query separation in order processing

## Prerequisites

- **Java 17+** (OpenJDK or GraalVM)
- **Docker & Docker Compose** (for infrastructure)
- **Gradle 8+** (wrapper included)

## Quick Start

### 1. Start Infrastructure

```bash
cd deployment
docker-compose up -d postgres mongodb redis kafka zookeeper schema-registry keycloak
```

Wait for all services to be healthy (~2 minutes):
```bash
docker-compose ps
```

### 2. Build All Services

```bash
cd backend
./gradlew clean build -x test
```

### 3. Run Unit Tests

```bash
./gradlew test
```

### 4. Run Integration Tests

```bash
./gradlew :integration-tests:test
```

> **Note**: Integration tests use Testcontainers - Docker must be running.

### 5. Start All Services

Option A - With Docker Compose (recommended):
```bash
cd deployment
docker-compose up -d
```

Option B - Locally (for development):
```bash
# Terminal 1
./gradlew :order-service:bootRun

# Terminal 2
./gradlew :asset-service:bootRun

# Terminal 3
./gradlew :customer-service:bootRun

# Terminal 4
./gradlew :order-processor:bootRun

# Terminal 5
./gradlew :notification-service:bootRun

# Terminal 6
./gradlew :audit-service:bootRun
```

## API Endpoints

### Order Service (port 8081)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| POST | `/api/orders` | Create order | CUSTOMER, ADMIN |
| GET | `/api/orders` | List orders | CUSTOMER, ADMIN |
| GET | `/api/orders/{id}` | Get order details | CUSTOMER, ADMIN |
| DELETE | `/api/orders/{id}` | Cancel order | CUSTOMER, ADMIN |
| POST | `/api/orders/{id}/match` | Match order (admin only) | ADMIN |

### Asset Service (port 8082)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/assets` | List customer assets | CUSTOMER, ADMIN |
| GET | `/api/assets/{assetName}` | Get specific asset | CUSTOMER, ADMIN |
| POST | `/api/assets/deposit` | Deposit funds | ADMIN |
| POST | `/api/assets/withdraw` | Withdraw funds | ADMIN |

### Customer Service (port 8083)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| GET | `/api/customers` | List customers | ADMIN |
| GET | `/api/customers/{id}` | Get customer details | CUSTOMER, ADMIN |
| POST | `/api/customers` | Create customer | ADMIN |
| PUT | `/api/customers/{id}` | Update customer | ADMIN |
| DELETE | `/api/customers/{id}` | Delete customer | ADMIN |

## Database Schema

### Asset Table (PDF Compliant)
```sql
CREATE TABLE customer_assets (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    asset_name VARCHAR(20) NOT NULL,
    size DECIMAL(19,4) NOT NULL DEFAULT 0,        -- Total amount
    usable_size DECIMAL(19,4) NOT NULL DEFAULT 0, -- Available amount
    -- blocked = size - usable_size (calculated)
    UNIQUE(customer_id, asset_name)
);
```

### Order Table
```sql
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    asset_name VARCHAR(20) NOT NULL,
    order_side VARCHAR(10) NOT NULL,  -- BUY, SELL
    size DECIMAL(19,4) NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,      -- PENDING, MATCHED, CANCELLED
    created_at TIMESTAMP NOT NULL
);
```

## Order Flow

### BUY Order
1. Customer creates BUY order for AAPL at $150, size 10
2. System reserves TRY balance: `usable_size -= 1500`
3. Order enters matching queue
4. When matched: TRY `size -= 1500`, AAPL `size += 10, usable_size += 10`

### SELL Order
1. Customer creates SELL order for AAPL at $150, size 10
2. System reserves AAPL: `usable_size -= 10`
3. Order enters matching queue
4. When matched: AAPL `size -= 10`, TRY `size += 1500, usable_size += 1500`

### Cancel Order
1. Customer cancels PENDING order
2. System releases reservation: `usable_size += reserved_amount`

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SPRING_PROFILES_ACTIVE` | `local` | Active profile (local/docker) |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/db` | Database URL |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWK_SET_URI` | Keycloak URL | JWT verification |

### Keycloak Configuration

Default admin credentials:
- URL: http://localhost:8180
- Admin: `admin` / `admin123`
- Realm: `brokage`

Test users:
- Admin: `admin@brokage.com` / `admin123`
- Customer: `customer@test.com` / `customer123`

## Testing

### Unit Tests
```bash
./gradlew test
```

### Integration Tests (Testcontainers)
```bash
./gradlew :integration-tests:test
```

### Stress Tests (K6)
```bash
cd stress-tests
docker-compose -f docker-compose.k6.yml up
```

## Monitoring

- **Grafana**: http://localhost:3001 (admin/admin123)
- **Kafka UI**: http://localhost:8089
- **Traefik Dashboard**: http://localhost:8090

## Project Structure

```
backend/
├── common/               # Shared DTOs, exceptions, utilities
├── order-service/        # Order management
├── asset-service/        # Portfolio & balance management
├── customer-service/     # Customer management
├── order-processor/      # Matching engine & saga orchestrator
├── notification-service/ # Notifications (MongoDB)
├── audit-service/        # Audit logging (MongoDB)
└── integration-tests/    # End-to-end tests
```

## Design Decisions

### Why Microservices?

While a monolith would suffice for the core requirements, this architecture demonstrates:
- Event-driven design with Kafka
- Distributed transaction handling (Saga pattern)
- Service isolation and independent scaling
- Polyglot persistence (PostgreSQL + MongoDB)

For a production system with simpler requirements, consider starting with a modular monolith.

### Why Separate Matching Engine?

The order-processor service contains the matching engine as a separate component to:
- Enable independent scaling during high-volume trading
- Isolate complex matching logic
- Support future enhancements (partial fills, market orders)

## License

This project is for interview/assessment purposes.
