# Brokage Trading Platform

<div align="center">

```
██████╗ ██████╗  ██████╗ ██╗  ██╗ █████╗  ██████╗ ███████╗
██╔══██╗██╔══██╗██╔═══██╗██║ ██╔╝██╔══██╗██╔════╝ ██╔════╝
██████╔╝██████╔╝██║   ██║█████╔╝ ███████║██║  ███╗█████╗
██╔══██╗██╔══██╗██║   ██║██╔═██╗ ██╔══██║██║   ██║██╔══╝
██████╔╝██║  ██║╚██████╔╝██║  ██╗██║  ██║╚██████╔╝███████╗
╚═════╝ ╚═╝  ╚═╝ ╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝ ╚═════╝ ╚══════╝
```

**Stock Trading Platform Built with Event-Driven Microservices**

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Vue.js](https://img.shields.io/badge/Vue.js-3.4-4FC08D.svg)](https://vuejs.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-3.6-231F20.svg)](https://kafka.apache.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791.svg)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED.svg)](https://www.docker.com/)

**English** | [Turkce](README_TR.md)

</div>

---

## Dear Reviewer

> **This section is for you. Notes I'd like you to consider while evaluating this project:**

<!-- REVIEWER NOTES START -->

### First of All

Hello! Thank you for reviewing this project. Below are some points I'd like you to pay attention to while evaluating:

### Time and Scope

- This project was developed within **5 days**
- The goal is not a production-ready system, but to **demonstrate architectural capabilities**

### AI Tools Usage

In this project, **Claude Code** and similar AI tools were used as **productivity tools**:

| Usage Area | Description |
|------------|-------------|
| **Code Quality** | Code quality improvement and refactoring suggestions |
| **Clarification** | Concept and pattern explanations |
| **Documentation** | Documentation writing and editing |
| **Security Check** | Security vulnerability scanning and suggestions |
| **Review Helper** | Code review and best practice checks |
| **Quick Research** | Quick access to information instead of Google Search |

> **Note**: AI tools were used **not to write code**, but to **increase productivity**. Architectural decisions, design choices, and implementation are entirely mine.

### Points I'd Like You to Pay Special Attention To

1. **Event-Driven Architecture**: Async communication with Kafka, Outbox pattern, Saga orchestration
2. **Authorization Model**: 3-level role system (Admin/Broker/Customer)
3. **Code Quality**: Clean code principles, SOLID, test coverage
4. **DevOps**: Single command startup (`make start`), Docker Compose, CI/CD ready

### Known Limitations / TODO

> **Note**: The items below are features **planned** in the `docs/` folder but **not implemented** due to time constraints.

| Feature | In Docs | Implementation | Description |
|---------|---------|----------------|-------------|
| **Redis Cache** | ✅ Planned | ❌ None | Cache layer missing in Order/Asset services |
| **Avro Schemas** | ✅ Planned | ❌ JSON | Kafka messages use JSON, Avro + Schema Registry not integrated |
| **Schema Registry** | ✅ Planned | ❌ None | No schema versioning and compatibility checks |
| **SMS Notification** | ✅ Planned | ❌ None | Only email available |
| **Push Notification** | ✅ Planned | ❌ None | No mobile push notification |
| **WebSocket** | ✅ Planned | ❌ None | No real-time updates |
| **mTLS** | ✅ Planned | ❌ None | No service-to-service TLS |
| **Async Saga** | ✅ Planned | ⚠️ Sync | Asset reservation done synchronously (for simplicity) |
| **Partial Fills** | ✅ Planned | ❌ None | No partial order matching |
| **Circuit Breaker** | ✅ Planned | ⚠️ Config | Defined in Traefik but not tested |
| **Grafana Dashboards** | ✅ Planned | ❌ None | Custom dashboards planned but not implemented |

**Priority Order (if to be implemented):**
1. Redis Cache (performance)
2. Avro + Schema Registry (data integrity)
3. WebSocket (user experience)
4. Async Saga (resilience)

### Contact

If you have any questions, feel free to reach out.

**Email**: [email@example.com]
**LinkedIn**: [linkedin.com/in/...]

<!-- REVIEWER NOTES END -->

---

## Table of Contents

- [Dear Reviewer](#dear-reviewer)
- [Quick Start](#quick-start)
- [Screenshots](#screenshots)
- [Presentation Mode](#presentation-mode)
- [About the Project](#about-the-project)
- [Over-Engineering Note](#over-engineering-note)
- [PDF Requirements Compliance](#pdf-requirements-compliance)
- [Architecture](#architecture)
- [Features](#features)
- [Services](#services)
- [Technologies](#technologies)
- [Make Commands](#make-commands)
- [API Documentation](#api-documentation)
- [Demo Users](#demo-users)
- [Monitoring](#monitoring)
- [Testing](#testing)
- [Design Decisions](#design-decisions)
- [Detailed Documentation](#detailed-documentation)

---

## Quick Start

### Prerequisites

- **Docker Desktop** (includes Docker Compose V2)
- **Java 17+** (optional - can build with Docker)

### Start with One Command

```bash
# Clone the project
git clone <repo-url>
cd interview-20251222

# Start the system (single command!)
make start
```

> **Note:** First run may take 3-5 minutes as Docker images are downloaded.

### System Ready!

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:4000 | Vue.js Web Application |
| **API Gateway** | http://localhost:4500 | Traefik API Gateway |
| **Keycloak** | http://localhost:8180 | Identity Management (admin/admin123) |
| **Grafana** | http://localhost:3001 | Monitoring Dashboard (admin/admin123) |
| **Kafka UI** | http://localhost:8089 | Kafka Management Panel |
| **Mailpit** | http://localhost:8026 | Email Testing Tool |
| **pgAdmin** | http://localhost:5050 | PostgreSQL Management |
| **Mongo Express** | http://localhost:8027 | MongoDB Management |

---

## Screenshots

### Admin Dashboard
System-wide monitoring, customer/broker counts, order statistics, system health, and top traders.

![Admin Dashboard](docs/images/screenshots/03-admin-dashboard.png)

### Broker Dashboard
Managing assigned customers and their orders.

![Broker Dashboard](docs/images/screenshots/02-broker-dashboard.png)

### Customer Dashboard
Customer portfolio, balance status, pending orders, and recent transactions.

![Customer Dashboard](docs/images/screenshots/01-customer-dashboard.png)

### Customer - My Assets
TRY balance (size/usableSize), deposit/withdraw, PDF schema explanation.

![Customer Wallet](docs/images/screenshots/05-customer-wallet.png)

### Market Simulator (SSE)
Real-time stock market simulation with **Server-Sent Events**. BIST and NASDAQ stocks.

> **Technical Note**: This page was developed to experiment with SSE (Server-Sent Events) technology. Price updates from the backend are instantly reflected in the UI.

![Market Simulator](docs/images/screenshots/04-market-simulator.png)

---

## Presentation Mode

A special presentation mode has been prepared to showcase the project as a **live demo**. This mode:

- Automatically starts all services (if stopped)
- Shows PDF scenarios step by step
- Browser opens automatically and performs operations live
- Creates video recording

### Run

```bash
make present
```

### What Does It Do?

```
Phase 1: Docker check
Phase 2: Wait for services to be ready
Phase 3: Prepare Playwright E2E tests
Phase 4: Show PDF scenarios
Phase 5: Live demo starts (browser opens)
Phase 6: Report results
```

### Presentation Content

The following scenarios are demonstrated live during the demo:

| Scenario | Description |
|----------|-------------|
| **Login Flow** | Login with different roles |
| **Order Creation** | Create BUY/SELL orders |
| **Order Listing** | Filtering and listing |
| **Order Cancellation** | Cancel PENDING orders |
| **Asset Management** | View assets |
| **Admin Match** | Order matching (Admin) |
| **Email Notification** | Check emails in Mailpit |

### Outputs

- **Video Recording**: `frontend/web-client/test-results/`
- **HTML Report**: `frontend/web-client/presentation-report/index.html`

> **Tip**: HTML report opens automatically when the presentation ends.

---

## About the Project

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="docs/images/logo/logo_ob_white.png">
  <source media="(prefers-color-scheme: light)" srcset="docs/images/logo/logo_ob_colored.png">
  <img src="docs/images/logo/logo_ob_colored.png" alt="Orange Broker HUB" width="300"/>
</picture>

This project is a backend API system developed for a **brokerage firm**. The system allows firm employees to create and manage stock buy/sell orders on behalf of customers.

### Why Microservices?

Although H2 and a simple monolith would suffice for the PDF requirements, this implementation chose microservices architecture to:

- **Demonstrate real-world distributed system patterns** (Saga, Outbox, Event Sourcing)
- **Use production-grade infrastructure** (Kafka, PostgreSQL, Keycloak)
- **Show how a scalable trading platform would be architected**

### Core Capabilities

- **Event-Driven Architecture** - Async messaging with Kafka
- **Saga Pattern** - Distributed transaction management
- **Outbox Pattern** - Atomic event publishing guarantee
- **CQRS** - Command/query separation in order processing
- **Role-Based Access Control** - 3-level authorization with Keycloak

---

## Over-Engineering Note

> **Important**: This section explains why the project was designed this way.

### Conscious Choices

This project goes **far beyond** the PDF requirements. This is a conscious choice:

| PDF Requirements | This Project |
|-----------------|--------------|
| H2 in-memory database | PostgreSQL + MongoDB |
| Basic Auth | Keycloak OAuth2/OIDC |
| Monolith | 6+ Microservices |
| Synchronous operations | Event-Driven + Kafka |
| Simple CRUD | Saga, Outbox, CQRS patterns |

### Why?

1. **Capability Demonstration**: As an interview project, I wanted to show not just "it works" but "how I think".

2. **Production-Ready Mindset**: An answer to how such a trading system would be designed in the real world.

3. **Extensibility**: A solid foundation for adding new features (partial fills, market orders, real-time updates).

### Alternative: Minimal Solution

If I only wanted to meet the PDF requirements:

```java
// Single Spring Boot application
@RestController
public class OrderController {
    @Autowired private OrderRepository orderRepo;
    @Autowired private AssetRepository assetRepo;

    @PostMapping("/orders")
    public Order createOrder(@RequestBody OrderRequest req) {
        // Direct synchronous balance check
        // H2 database
        // Basic auth
    }
}
```

This could be solved with ~500 lines of code. However, this would not demonstrate my **architectural thinking ability**.

### I'm Aware of the Trade-offs

| Advantage | Disadvantage |
|-----------|--------------|
| Scalable | Operational complexity |
| Fault isolation | Network latency |
| Independent deploy | Distributed debugging |
| Polyglot persistence | More infrastructure |

### Conclusion

This project answers not **"Can I write a simple CRUD application?"** but **"Can I design a complex distributed system?"**

> I can produce simple solutions for simple requirements, scalable solutions for complex requirements.

Ultimately, this is a **sandbox project**. It was a great opportunity to both showcase my skills and refresh my **Java/Spring Boot ecosystem** knowledge that I hadn't used for a while. Having the time, I took advantage of this opportunity and tried to do my best.

---

## PDF Requirements Compliance

### Core Requirements

| Requirement | Status | Implementation |
|-------------|--------|----------------|
| `POST /api/orders` (Create) | ✅ | `OrderController.createOrder()` |
| `GET /api/orders` (List) | ✅ | `OrderController.listOrders()` + filters |
| `DELETE /api/orders/{id}` (Cancel) | ✅ | `OrderController.cancelOrder()` |
| `GET /api/assets` (Assets) | ✅ | `AssetController.getCustomerAssets()` |
| Admin Authorization | ✅ | JWT/Keycloak (upgraded from Basic Auth) |
| TRY as asset (not separate table) | ✅ | `CustomerAsset` with `assetName="TRY"` |
| size/usableSize balance logic | ✅ | `blockAmount()`, `releaseBlockedAmount()`, `deductBlockedAmount()` |
| Unit Tests | ✅ | 28+ test files |

### Bonus Features

| Feature | Status | Description |
|---------|--------|-------------|
| Customer Authorization | ✅ | Role-based access with Keycloak |
| Match Endpoint | ✅ | `POST /api/orders/{id}/match` (Admin/Broker) |
| Broker Role | ✅ | Order management for sub-customers |
| Customer Tier System | ✅ | VIP/Premium/Standard - Rate limiting |
| Email Notifications | ✅ | On order status changes |

### Database Schema (PDF Compliant)

```sql
-- Asset Table (as in PDF)
CREATE TABLE customer_assets (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    asset_name VARCHAR(20) NOT NULL,      -- TRY, AAPL, GOOGL, etc.
    size DECIMAL(19,4) DEFAULT 0,          -- Total amount
    usable_size DECIMAL(19,4) DEFAULT 0,   -- Available amount
    -- blocked = size - usable_size (calculated)
    UNIQUE(customer_id, asset_name)
);

-- Order Table
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    asset_name VARCHAR(20) NOT NULL,
    order_side VARCHAR(10) NOT NULL,       -- BUY, SELL
    size DECIMAL(19,4) NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL,           -- PENDING, MATCHED, CANCELED
    created_at TIMESTAMP NOT NULL
);
```

---

## Architecture

### System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENTS                                         │
│                    Web Browser / Mobile App / API                            │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         TRAEFIK API GATEWAY                                  │
│              Rate Limiting │ JWT Validation │ Circuit Breaker                │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│ Order Service │         │ Asset Service │         │Customer Service│
│    :8081      │         │    :8082      │         │    :8083      │
└───────────────┘         └───────────────┘         └───────────────┘
        │                           │                           │
        └───────────────────────────┼───────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            APACHE KAFKA                                      │
│                     order-events │ asset-events                              │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
        ┌───────────────────────────┼───────────────────────────┐
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│Order Processor│         │ Notification  │         │ Audit Service │
│    :8084      │         │    :8085      │         │    :8086      │
└───────────────┘         └───────────────┘         └───────────────┘
        │                           │                           │
        ▼                           ▼                           ▼
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│  PostgreSQL   │         │    MongoDB    │         │    MongoDB    │
└───────────────┘         └───────────────┘         └───────────────┘
```

### Order Flow

#### BUY Order
```
1. Customer creates BUY order for 10 AAPL at $150
2. System reserves TRY balance: usable_size -= 1500
3. Order enters matching queue
4. Match: TRY size -= 1500, AAPL size += 10, usable_size += 10
```

#### SELL Order
```
1. Customer creates SELL order for 10 AAPL at $150
2. System reserves AAPL: usable_size -= 10
3. Order enters matching queue
4. Match: AAPL size -= 10, TRY size += 1500, usable_size += 1500
```

#### Order Cancellation
```
1. Customer cancels PENDING order
2. System releases reservation: usable_size += reserved_amount
```

---

## Features

### 1. Event-Driven Architecture

- **Outbox Pattern**: Each service atomically records events with database transactions
- **Kafka Topics**: Async communication via `order-events`, `asset-events`
- **At-Least-Once Delivery**: Guarantee without event loss

### 2. Authorization System

```
┌─────────────────────────────────────────────────────────────────┐
│                     AUTHORIZATION MATRIX                         │
├─────────────────────┬───────────┬───────────┬───────────────────┤
│ Operation           │   ADMIN   │   BROKER  │     CUSTOMER      │
├─────────────────────┼───────────┼───────────┼───────────────────┤
│ Create Order        │ All cust. │ Sub-cust. │ Self only         │
│ List Orders         │ All cust. │ Sub-cust. │ Self only         │
│ Cancel Order        │ All cust. │ Sub-cust. │ Self + PENDING    │
│ Match Order         │ All cust. │ Sub-cust. │ No Access         │
│ View Assets         │ All cust. │ Sub-cust. │ Self only         │
│ Deposit             │ All cust. │ Sub-cust. │ No Access         │
│ Withdraw            │ All cust. │ No Access │ Own account       │
└─────────────────────┴───────────┴───────────┴───────────────────┘
```

### 3. Customer Tier System

| Tier | Priority | Rate Limit | Description |
|------|----------|------------|-------------|
| **VIP** | 1 (Highest) | 1000 req/min | Premium customers |
| **Premium** | 2 | 500 req/min | Mid-segment |
| **Standard** | 3 | 100 req/min | Default |

- Matching priority: Tier > Price > Time
- Rate limiting at Gateway + Application layer

### 4. Observability

- **LGTM Stack**: Loki (logs), Grafana (dashboards), Tempo (traces), Mimir (metrics)
- **OpenTelemetry**: Distributed tracing
- **Health Checks**: `/actuator/health` for all services

---

## Services

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| **order-service** | 8081 | PostgreSQL | Order CRUD, saga initiation, cancellation |
| **asset-service** | 8082 | PostgreSQL | Portfolio management, balance blocking |
| **customer-service** | 8083 | PostgreSQL | Customer CRUD, tier management |
| **order-processor** | 8084 | PostgreSQL | Matching engine, saga orchestrator |
| **notification-service** | 8085 | MongoDB | Multi-channel notifications |
| **audit-service** | 8086 | MongoDB | Audit logs and compliance |
| **web-api** | 8087 | - | Frontend BFF (aggregation) |

---

## Technologies

### Backend
- **Java 17** + **Spring Boot 3.2**
- **Spring Security** + OAuth2 Resource Server
- **Spring Data JPA** + Hibernate
- **Apache Kafka** 3.6
- **PostgreSQL 15** + **MongoDB 7**
- **Redis** (cache)

### Frontend
- **Vue.js 3.4** + Composition API
- **Vite** + TypeScript
- **Tailwind CSS** + shadcn/vue
- **Pinia** (state management)
- **Playwright** (E2E testing)

### Infrastructure
- **Traefik** - API Gateway
- **Keycloak** - Identity Provider
- **Docker Compose** - Container orchestration
- **LGTM Stack** - Observability

---

## Make Commands

The project includes a comprehensive `Makefile` for single-command operation:

### Basic Commands

```bash
make              # Show help menu
make start        # Start entire system (one-click demo)
make stop         # Stop all services
make restart      # Restart services
make status       # Show service status
make logs         # Show all logs
make clean        # Remove all containers and data
```

### Development Commands

```bash
make start-backend    # Start backend only (without frontend)
make dev              # Development mode (local frontend)
make build            # Build backend services
make test             # Run unit tests
```

### Presentation Commands

```bash
make present      # Live E2E demo (automatic browser)
make check        # Check prerequisites
```

### Command Details

#### `make start`
- Checks prerequisites (Docker, Java, ports)
- Starts infrastructure services (Kafka, PostgreSQL, MongoDB, Redis)
- Builds and starts backend services
- Starts frontend
- Verifies all services are ready
- Shows access URLs

#### `make present`
- Automatic E2E demo mode
- Shows PDF scenarios live
- Browser opens automatically
- Creates video recording

#### `make check`
```
Checking prerequisites...

  Docker............. OK (24.0.7)
  Docker Compose..... OK (v2.23.3)
  Docker Running..... OK
  Java 17+........... OK (17.0.9)
  Port 4000.......... FREE
  Port 4500.......... FREE
  Port 8180.......... FREE

All checks passed!
```

---

## API Documentation

### Swagger UI Access

| Service | URL |
|---------|-----|
| Order Service | http://localhost:7081/swagger-ui.html |
| Asset Service | http://localhost:7082/swagger-ui.html |
| Customer Service | http://localhost:7083/swagger-ui.html |
| Notification Service | http://localhost:7085/swagger-ui.html |
| Audit Service | http://localhost:7086/swagger-ui.html |

### Order Service API

```http
# Create order
POST /api/orders
Authorization: Bearer {token}
Content-Type: application/json

{
  "customerId": "uuid",
  "assetName": "AAPL",
  "orderSide": "BUY",
  "orderType": "LIMIT",
  "price": "150.00",
  "size": "10"
}

# List orders (filtered)
GET /api/orders?customerId={uuid}&status=PENDING&startDate=2025-01-01&endDate=2025-12-31

# Order detail
GET /api/orders/{orderId}

# Cancel order
DELETE /api/orders/{orderId}

# Match order (Admin/Broker)
POST /api/orders/{orderId}/match
```

### Asset Service API

```http
# List customer assets
GET /api/assets?customerId={uuid}

# Get specific asset
GET /api/assets/{customerId}/{assetName}

# Deposit (Admin/Broker)
POST /api/assets/deposit
{
  "customerId": "uuid",
  "assetName": "TRY",
  "amount": "10000.00"
}

# Withdraw (Admin/Customer-self)
POST /api/assets/withdraw
{
  "customerId": "uuid",
  "assetName": "TRY",
  "amount": "500.00"
}
```

---

## Demo Users

The system comes with pre-configured demo users:

| User | Email | Password | Role |
|------|-------|----------|------|
| Nick Fury | nick.fury@brokage.com | admin123 | ADMIN |
| Tony Stark | tony.stark@brokage.com | broker123 | BROKER |
| Peter Parker | peter.parker@brokage.com | customer123 | CUSTOMER |

### Getting Token from Keycloak

```bash
# Admin token
curl -X POST "http://localhost:8180/realms/brokage/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=brokage-api" \
  -d "client_secret=brokage-api-secret" \
  -d "username=nick.fury" \
  -d "password=admin123" \
  -d "grant_type=password"
```

---

## Monitoring

### Grafana - LGTM Stack

- **URL**: http://localhost:3001
- **User**: admin / admin123

The system is monitored with **LGTM Stack** (Loki, Grafana, Tempo, Mimir):

#### Tempo - Distributed Tracing

Request tracking and performance analysis across services:

![Tempo Tracing](docs/images/grafana/01-tempo-tracing.png)

#### Metrics - OpenTelemetry

Metrics collected via OTel exporter:

![Metrics](docs/images/grafana/02-metrics-otel.png)

#### Loki - Centralized Logs

All service logs can be monitored from one place:

![Loki Logs](docs/images/grafana/03-loki-logs.png)

#### Pyroscope - Continuous Profiling

CPU, memory, and allocation profiling:

![Pyroscope Profiles](docs/images/grafana/04-pyroscope-profiles.png)

### Kafka UI

- **URL**: http://localhost:8089
- Monitor topics, consumer groups, and messages

### Mailpit (Email Test)

- **URL**: http://localhost:8026
- View emails sent on order status changes

![Mailpit Notifications](docs/images/grafana/05-mailpit-notifications.png)

### Database Tools

| Tool | URL | User |
|------|-----|------|
| pgAdmin | http://localhost:5050 | admin@brokage.com / admin123 |
| Mongo Express | http://localhost:8027 | admin / admin123 |

---

## Testing

### Unit Tests

```bash
cd backend
./gradlew test
```

### Integration Tests

```bash
# Services must be running
cd backend
./gradlew :integration-tests:test
```

### E2E Tests (Playwright)

```bash
cd frontend/web-client
npm install
npx playwright install
npx playwright test
```

### Stress Tests (k6)

```bash
cd stress-tests/k6
./run.sh order-flow
```

---

## Design Decisions

### 1. Why Microservices?

**Advantages:**
- Independent deploy and scaling
- Polyglot persistence (PostgreSQL + MongoDB)
- Fault isolation
- Team-based development

**Trade-offs:**
- Increased operational complexity
- Network latency
- Distributed transaction management

### 2. Why Saga Pattern?

Since there's no ACID guarantee in distributed systems:
- Choreography-based saga was used
- Compensation action defined for each step
- Audit trail with event sourcing

### 3. Why Outbox Pattern?

To solve the dual-write problem:
- Events are written to database in the same transaction
- Background worker publishes to Kafka
- At-least-once delivery guarantee

### 4. Asset vs CustomerAsset Separation

Although PDF specifies a single `Asset` table:
- `CustomerAsset`: Customer portfolio (size, usableSize)
- `Asset`: Master asset data (min lot, active status)
- 3NF normalization - prevents data duplication

---

## Detailed Documentation

> **Note**: The documents below contain deeper technical details. Review them if the information in README is not sufficient.

Detailed technical documentation is available in `docs/en/` folder:

| Document | Description |
|----------|-------------|
| [System Overview](docs/en/01-system-overview.md) | Architecture and concepts |
| [Microservices Architecture](docs/en/02-microservices-architecture.md) | Service details |
| [Event-Driven Flows](docs/en/03-event-driven-flows.md) | Kafka and Saga |
| [Database Design](docs/en/04-database-design.md) | Schema and relationships |
| [API Gateway and Security](docs/en/05-api-gateway-security.md) | Auth and rate limiting |
| [Monitoring and Observability](docs/en/06-monitoring-observability.md) | LGTM stack |

---

## Project Structure

```
interview-20251222/
├── backend/
│   ├── common/                 # Shared DTOs, exceptions, utilities
│   ├── order-service/          # Order management
│   ├── asset-service/          # Portfolio and balance management
│   ├── customer-service/       # Customer management
│   ├── order-processor/        # Matching engine
│   ├── notification-service/   # Notifications (MongoDB)
│   ├── audit-service/          # Audit logs (MongoDB)
│   ├── web-api/                # Frontend BFF
│   └── integration-tests/      # E2E tests
├── frontend/
│   └── web-client/             # Vue.js application
├── deployment/
│   ├── docker-compose.yml      # Main compose file
│   └── config/                 # Configuration files
├── docs/
│   ├── en/                     # English documentation
│   └── tr/                     # Turkish documentation
├── postman/                    # Postman collection
├── stress-tests/               # k6 stress tests
├── scripts/                    # Helper scripts
└── Makefile                    # One-click start
```

---

## Postman Collection

The project includes a ready Postman collection:

```
postman/
├── Brokage-API.postman_collection.json
├── Brokage-Environment.postman_environment.json
└── README.md
```

### Import and Usage

1. Click **Import** button in Postman
2. Import both JSON files
3. Select "Brokage Local Environment" as Environment
4. Get token from **Authentication** folder
5. Use other requests

---

## License

This project is for interview/evaluation purposes.

---

<div align="center">

<img src="docs/images/logo/icon_ob.png" alt="Orange Broker HUB" width="80"/>

**Orange Broker HUB** - Built with Event-Driven Microservices

Made with ❤️ for the interview

</div>
