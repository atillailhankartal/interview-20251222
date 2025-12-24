# System Overview

## Project Purpose

This project develops a backend API for a **brokerage firm**. The system enables firm employees to place and manage stock buy/sell orders on behalf of customers.

---

## General Architecture

```mermaid
flowchart TB
    subgraph Clients["Clients"]
        WEB[Web Browser]
        MOBILE[Mobile App]
        API_CLIENT[API Client]
    end

    subgraph Gateway["API Gateway Layer"]
        TRAEFIK[Traefik Gateway]
        subgraph GW_Features["Gateway Features"]
            RL[Rate Limiting]
            JWT[JWT Validation]
            CB[Circuit Breaker]
        end
    end

    subgraph Services["Microservices"]
        ORDER[Order Service]
        ASSET[Asset Service]
        CUSTOMER[Customer Service]
        PROCESSOR[Order Processor]
        NOTIFICATION[Notification Service]
        AUDIT[Audit Service]
    end

    subgraph Messaging["Messaging Infrastructure"]
        KAFKA[Apache Kafka]
    end

    subgraph Data["Data Layer"]
        PG[(PostgreSQL)]
        MONGO[(MongoDB)]
        REDIS[(Redis Cache)]
    end

    subgraph Auth["Authentication"]
        KEYCLOAK[Keycloak]
    end

    subgraph Monitoring["Monitoring"]
        LGTM[LGTM Stack]
    end

    WEB --> TRAEFIK
    MOBILE --> TRAEFIK
    API_CLIENT --> TRAEFIK

    TRAEFIK --> RL
    TRAEFIK --> JWT
    TRAEFIK --> CB

    TRAEFIK --> ORDER
    TRAEFIK --> ASSET
    TRAEFIK --> CUSTOMER
    TRAEFIK --> KEYCLOAK

    ORDER --> KAFKA
    ASSET --> KAFKA
    PROCESSOR --> KAFKA

    KAFKA --> PROCESSOR
    KAFKA --> NOTIFICATION
    KAFKA --> AUDIT

    ORDER --> PG
    ASSET --> PG
    CUSTOMER --> PG
    PROCESSOR --> PG
    NOTIFICATION --> MONGO
    AUDIT --> MONGO

    ORDER --> REDIS
    ASSET --> REDIS

    JWT --> KEYCLOAK

    Services --> LGTM
```

---

## Core Concepts

### What is an Order?

When a customer wants to buy or sell stocks, they create an **order**. An order contains the following information:

```mermaid
erDiagram
    ORDER {
        Long id PK
        Long customerId FK
        String assetName
        String orderSide
        BigDecimal size
        BigDecimal price
        String status
        DateTime createDate
    }

    ORDER ||--o{ STATUS : has

    STATUS {
        String PENDING
        String MATCHED
        String CANCELED
        String REJECTED
    }
```

### What is an Asset?

Values owned by a customer are stored as **assets**. TRY (Turkish Lira) is also an asset.

```mermaid
erDiagram
    ASSET {
        Long id PK
        Long customerId FK
        String assetName
        BigDecimal size
        BigDecimal usableSize
    }
```

**Important:** `size` represents the total amount, `usableSize` represents the available (non-blocked) amount.

---

## Business Flow Summary

### BUY Order Flow

```mermaid
sequenceDiagram
    participant C as Customer
    participant API as API Gateway
    participant OS as Order Service
    participant K as Kafka
    participant AS as Asset Service
    participant P as Order Processor

    C->>API: POST /api/orders (BUY)
    API->>API: JWT Validation
    API->>OS: Create Order
    OS->>OS: Save Order (PENDING_RESERVATION)
    OS->>K: OrderCreatedEvent
    K->>AS: Reserve Balance
    AS->>AS: Decrease TRY usableSize
    AS->>K: AssetReservedEvent
    K->>OS: Update Status (ORDER_CONFIRMED)
    OS->>C: Order Created

    Note over C,P: Later when Admin matches...

    P->>OS: Match Order
    OS->>K: OrderMatchedEvent
    K->>AS: Decrease TRY size, Add Stock
    AS->>K: SettlementCompletedEvent
```

### SELL Order Flow

```mermaid
sequenceDiagram
    participant C as Customer
    participant API as API Gateway
    participant OS as Order Service
    participant K as Kafka
    participant AS as Asset Service
    participant P as Order Processor

    C->>API: POST /api/orders (SELL)
    API->>API: JWT Validation
    API->>OS: Create Order
    OS->>OS: Save Order (PENDING_RESERVATION)
    OS->>K: OrderCreatedEvent
    K->>AS: Reserve Stock
    AS->>AS: Decrease Stock usableSize
    AS->>K: AssetReservedEvent
    K->>OS: Update Status (ORDER_CONFIRMED)
    OS->>C: Order Created

    Note over C,P: Later when Admin matches...

    P->>OS: Match Order
    OS->>K: OrderMatchedEvent
    K->>AS: Decrease Stock size, Add TRY
    AS->>K: SettlementCompletedEvent
```

### Order Cancellation Flow

```mermaid
sequenceDiagram
    participant C as Customer
    participant API as API Gateway
    participant OS as Order Service
    participant K as Kafka
    participant AS as Asset Service

    C->>API: DELETE /api/orders/{id}
    API->>API: JWT + Authorization Check
    API->>OS: Cancel Order
    OS->>OS: Status = PENDING?

    alt Order Not PENDING
        OS->>C: Error: Only PENDING orders can be cancelled
    else Order is PENDING
        OS->>OS: Status = CANCELED
        OS->>K: OrderCancelledEvent
        K->>AS: Release Block
        AS->>AS: Increase usableSize
        AS->>K: AssetReleasedEvent
        OS->>C: Order Cancelled
    end
```

---

## Service Responsibilities

```mermaid
flowchart LR
    subgraph OrderService["Order Service"]
        O1[Order Creation]
        O2[Order Listing]
        O3[Order Cancellation]
        O4[Outbox Pattern]
    end

    subgraph AssetService["Asset Service"]
        A1[Balance Query]
        A2[Balance Reservation]
        A3[Balance Transfer]
        A4[Block Release]
    end

    subgraph CustomerService["Customer Service"]
        C1[Customer Management]
        C2[Tier Management]
        C3[Deposit]
        C4[Withdrawal]
    end

    subgraph OrderProcessor["Order Processor"]
        P1[Order Matching]
        P2[Settlement]
        P3[Saga Management]
    end

    subgraph NotificationService["Notification Service"]
        N1[Email Notification]
        N2[SMS Notification]
        N3[Push Notification]
        N4[WebSocket]
    end

    subgraph AuditService["Audit Service"]
        AU1[Event Logging]
        AU2[Compliance Reports]
        AU3[Analytics]
    end
```

---

## Customer Tier System (Not in task requirements, added as a product feature)

The system segments customers into tiers offering different service levels:

```mermaid
flowchart TB
    subgraph TierSystem["Customer Tier System"]
        VIP["VIP Tier<br/>Highest Priority<br/>1000 req/min"]
        PREMIUM["Premium Tier<br/>Medium Priority<br/>500 req/min"]
        STANDARD["Standard Tier<br/>Normal Priority<br/>100 req/min"]
    end

    subgraph Benefits["Benefits"]
        B1[Transaction Priority]
        B2[Rate Limit]
        B3[Dedicated Support]
    end

    VIP --> B1
    VIP --> B2
    VIP --> B3

    PREMIUM --> B1
    PREMIUM --> B2

    STANDARD --> B1
```

**Business Logic:** When matching orders, sorting is done first by tier, then by price, finally by time.

---

## Authorization Matrix

```mermaid
flowchart TB
    subgraph Roles["Roles"]
        ADMIN[Admin]
        CUSTOMER[Customer]
    end

    subgraph Endpoints["Endpoints"]
        CREATE[POST /api/orders]
        LIST[GET /api/orders]
        DELETE[DELETE /api/orders]
        MATCH[POST /api/orders/.../match]
        ASSETS[GET /api/assets]
    end

    ADMIN -->|Full Access| CREATE
    ADMIN -->|Full Access| LIST
    ADMIN -->|Full Access| DELETE
    ADMIN -->|Exclusive| MATCH
    ADMIN -->|Full Access| ASSETS

    CUSTOMER -->|Own Data| CREATE
    CUSTOMER -->|Own Data| LIST
    CUSTOMER -->|Own Order + PENDING| DELETE
    CUSTOMER -.->|No Access| MATCH
    CUSTOMER -->|Own Data| ASSETS

    style MATCH fill:#f96,stroke:#333
```

---

## Technology Stack

```mermaid
mindmap
    root((Brokage System))
        Backend
            Spring Boot 3
            Java 21
            Gradle
        Database
            PostgreSQL
                Order DB
                Asset DB
                Customer DB
            MongoDB
                Notifications
                Audit Logs
            Redis
                Cache
                Idempotency
        Messaging
            Apache Kafka
            Schema Registry
            Avro
        Security
            Keycloak
            JWT/OAuth2
            mTLS
        API Gateway
            Traefik
            Rate Limiting
            Circuit Breaker
        Monitoring
            Grafana
            Loki
            Tempo
            Prometheus
        Testing
            JUnit 5
            Testcontainers
            K6 Stress Test
```

---

## Next Steps

For more detailed architecture information, refer to the following documents:

1. **[Microservices Architecture](02-microservices-architecture.md)** - Detailed service structure
2. **[Event-Driven Flows](03-event-driven-flows.md)** - Kafka and Saga pattern
3. **[Database Design](04-database-design.md)** - Polyglot persistence
4. **[API Gateway and Security](05-api-gateway-security.md)** - Traefik and Keycloak
5. **[Monitoring and Observability](06-monitoring-observability.md)** - LGTM Stack
