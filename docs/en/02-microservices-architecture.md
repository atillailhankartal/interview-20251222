# Microservices Architecture

## Overview

The system is built on an event-driven microservices architecture. Each service has its own database and inter-service communication is handled through asynchronous messaging via Kafka.

---

## Service Boundaries

```mermaid
flowchart TB
    subgraph External["External World"]
        CLIENT[Clients]
    end

    subgraph Gateway["API Layer"]
        TRAEFIK[Traefik API Gateway]
    end

    subgraph Core["Core Services"]
        ORDER[Order Service]
        ASSET[Asset Service]
        CUSTOMER[Customer Service]
    end

    subgraph Processing["Processing Services"]
        PROCESSOR[Order Processor]
    end

    subgraph Support["Support Services"]
        NOTIFICATION[Notification Service]
        AUDIT[Audit Service]
    end

    subgraph Messaging["Messaging"]
        KAFKA[Apache Kafka]
    end

    subgraph Auth["Authentication"]
        KEYCLOAK[Keycloak]
    end

    CLIENT --> TRAEFIK
    TRAEFIK --> ORDER
    TRAEFIK --> ASSET
    TRAEFIK --> CUSTOMER
    TRAEFIK --> KEYCLOAK

    ORDER --> KAFKA
    ASSET --> KAFKA
    CUSTOMER --> KAFKA

    KAFKA --> PROCESSOR
    KAFKA --> NOTIFICATION
    KAFKA --> AUDIT

    PROCESSOR --> KAFKA
```

---

## Service Details

### Order Service

The main service responsible for order management.

```mermaid
flowchart LR
    subgraph OrderService["Order Service"]
        direction TB
        API[REST API]
        BL[Business Logic]
        REPO[Repository]
        OUTBOX[Outbox Table]
    end

    subgraph Operations["Operations"]
        CREATE[Create Order]
        LIST[List Orders]
        DELETE[Cancel Order]
    end

    subgraph Events["Produced Events"]
        E1[OrderCreatedEvent]
        E2[OrderCancelledEvent]
    end

    API --> BL
    BL --> REPO
    BL --> OUTBOX

    CREATE --> API
    LIST --> API
    DELETE --> API

    OUTBOX --> E1
    OUTBOX --> E2
```

### Asset Service

Responsible for asset and balance management.

```mermaid
flowchart LR
    subgraph AssetService["Asset Service"]
        direction TB
        API[REST API]
        BL[Business Logic]
        REPO[Repository]
    end

    subgraph Operations["Operations"]
        QUERY[Query Balance]
        RESERVE[Reservation]
        RELEASE[Release Reservation]
        TRANSFER[Transfer]
    end

    subgraph Events["Produced Events"]
        E1[AssetReservedEvent]
        E2[AssetReleasedEvent]
        E3[AssetTransferredEvent]
    end

    API --> BL
    BL --> REPO

    QUERY --> API
    RESERVE --> BL
    RELEASE --> BL
    TRANSFER --> BL

    BL --> E1
    BL --> E2
    BL --> E3
```

### Order Processor

Responsible for order matching and saga management.

```mermaid
flowchart LR
    subgraph OrderProcessor["Order Processor"]
        direction TB
        CONSUMER[Kafka Consumer]
        ENGINE[Matching Engine]
        SAGA[Saga Manager]
    end

    subgraph Operations["Operations"]
        MATCH[Match Order]
        SETTLE[Settlement]
        COMPENSATE[Compensate]
    end

    subgraph Events["Produced Events"]
        E1[OrderMatchedEvent]
        E2[OrderFailedEvent]
        E3[SettlementCompletedEvent]
    end

    CONSUMER --> ENGINE
    CONSUMER --> SAGA

    MATCH --> ENGINE
    SETTLE --> SAGA
    COMPENSATE --> SAGA

    ENGINE --> E1
    ENGINE --> E2
    SAGA --> E3
```

### Notification Service

Handles notification management and delivery.

```mermaid
flowchart LR
    subgraph NotificationService["Notification Service"]
        direction TB
        CONSUMER[Kafka Consumer]
        PROCESSOR[Notification Processor]
        CHANNELS[Channel Manager]
        STORE[MongoDB Store]
    end

    subgraph Channels["Channels"]
        EMAIL[Email]
        SMS[SMS]
        PUSH[Push Notification]
        WS[WebSocket]
    end

    subgraph Features["Features"]
        RETRY[Retry]
        HISTORY[History]
        PREFS[Preferences]
    end

    CONSUMER --> PROCESSOR
    PROCESSOR --> CHANNELS
    PROCESSOR --> STORE

    CHANNELS --> EMAIL
    CHANNELS --> SMS
    CHANNELS --> PUSH
    CHANNELS --> WS

    STORE --> RETRY
    STORE --> HISTORY
    STORE --> PREFS
```

### Customer Service

Manages customers and tiers.

```mermaid
flowchart LR
    subgraph CustomerService["Customer Service"]
        direction TB
        API[REST API]
        BL[Business Logic]
        REPO[Repository]
    end

    subgraph Operations["Operations"]
        CRUD[Customer CRUD]
        TIER[Tier Management]
        DEPOSIT[Deposit]
        WITHDRAW[Withdraw]
    end

    API --> BL
    BL --> REPO

    CRUD --> API
    TIER --> API
    DEPOSIT --> API
    WITHDRAW --> API
```

### Audit Service

Handles event logging and compliance reporting.

```mermaid
flowchart LR
    subgraph AuditService["Audit Service"]
        direction TB
        CONSUMER[Kafka Consumer]
        PROCESSOR[Event Processor]
        STORE[MongoDB Store]
    end

    subgraph Features["Features"]
        LOGGING[Event Logging]
        COMPLIANCE[Compliance]
        ANALYTICS[Analytics]
    end

    CONSUMER --> PROCESSOR
    PROCESSOR --> STORE

    STORE --> LOGGING
    STORE --> COMPLIANCE
    STORE --> ANALYTICS
```

---

## Database Isolation

Each service has its own database (Database per Service pattern).

```mermaid
flowchart TB
    subgraph Services["Services"]
        OS[Order Service]
        AS[Asset Service]
        CS[Customer Service]
        OP[Order Processor]
        NS[Notification Service]
        AU[Audit Service]
    end

    subgraph PostgreSQL["PostgreSQL Cluster"]
        DB1[(brokage_orders)]
        DB2[(brokage_assets)]
        DB3[(brokage_customers)]
        DB4[(brokage_processor)]
    end

    subgraph MongoDB["MongoDB"]
        M1[(notifications)]
        M2[(audit_logs)]
    end

    OS --> DB1
    AS --> DB2
    CS --> DB3
    OP --> DB4
    NS --> M1
    AU --> M2
```

**Important Rule:** Services do not directly access each other's databases. All communication is done through events via Kafka.

---

## Inter-Service Communication

```mermaid
sequenceDiagram
    participant OS as Order Service
    participant K as Kafka
    participant AS as Asset Service
    participant OP as Order Processor
    participant NS as Notification Service

    Note over OS,NS: Asynchronous Event-Driven Communication

    OS->>K: OrderCreatedEvent
    K->>AS: OrderCreatedEvent
    AS->>AS: Check Balance & Reserve
    AS->>K: AssetReservedEvent

    K->>OS: AssetReservedEvent
    OS->>OS: Status = ORDER_CONFIRMED

    K->>OP: AssetReservedEvent
    OP->>OP: Wait for Matching

    Note over OP: Admin Match Command

    OP->>K: OrderMatchedEvent
    K->>AS: Execute Transfer
    K->>NS: Send Notification
    K->>OS: Update Status
```

---

## Service Dependencies

```mermaid
flowchart TB
    subgraph Infrastructure["Infrastructure Layer"]
        PG[PostgreSQL]
        MG[MongoDB]
        RD[Redis]
        KF[Kafka]
        ZK[Zookeeper]
        SR[Schema Registry]
    end

    subgraph Services["Application Layer"]
        OS[Order Service]
        AS[Asset Service]
        CS[Customer Service]
        OP[Order Processor]
        NS[Notification Service]
        AU[Audit Service]
    end

    subgraph Gateway["Gateway Layer"]
        TR[Traefik]
        KC[Keycloak]
    end

    subgraph Monitoring["Monitoring Layer"]
        LGTM[LGTM Stack]
    end

    %% Infrastructure dependencies
    KF --> ZK
    KF --> SR

    %% Service to Infrastructure
    OS --> PG
    OS --> KF
    OS --> RD

    AS --> PG
    AS --> KF

    CS --> PG
    CS --> KF

    OP --> PG
    OP --> KF

    NS --> MG
    NS --> KF

    AU --> MG
    AU --> KF

    %% Gateway dependencies
    TR --> OS
    TR --> AS
    TR --> CS
    TR --> KC

    %% Monitoring
    OS --> LGTM
    AS --> LGTM
    CS --> LGTM
    OP --> LGTM
    NS --> LGTM
    AU --> LGTM
```

---

## Startup Order

The startup order of services is critically important:

```mermaid
flowchart LR
    subgraph Phase1["Phase 1: Infrastructure"]
        PG[PostgreSQL]
        MG[MongoDB]
        RD[Redis]
        ZK[Zookeeper]
    end

    subgraph Phase2["Phase 2: Messaging"]
        KF[Kafka]
        SR[Schema Registry]
    end

    subgraph Phase3["Phase 3: Identity"]
        KC[Keycloak]
    end

    subgraph Phase4["Phase 4: Core Services"]
        AS[Asset Service]
        CS[Customer Service]
        OS[Order Service]
    end

    subgraph Phase5["Phase 5: Processing Services"]
        OP[Order Processor]
    end

    subgraph Phase6["Phase 6: Support Services"]
        NS[Notification Service]
        AU[Audit Service]
    end

    subgraph Phase7["Phase 7: Gateway"]
        TR[Traefik]
        LGTM[LGTM]
    end

    Phase1 --> Phase2
    Phase2 --> Phase3
    Phase3 --> Phase4
    Phase4 --> Phase5
    Phase5 --> Phase6
    Phase6 --> Phase7
```

---

## Scaling Strategy

```mermaid
flowchart TB
    subgraph LoadBalancer["Load Balancer"]
        TR[Traefik]
    end

    subgraph OrderServiceCluster["Order Service Cluster"]
        OS1[Order Service 1]
        OS2[Order Service 2]
        OS3[Order Service 3]
    end

    subgraph AssetServiceCluster["Asset Service Cluster"]
        AS1[Asset Service 1]
        AS2[Asset Service 2]
    end

    subgraph KafkaCluster["Kafka Cluster"]
        K1[Broker 1]
        K2[Broker 2]
        K3[Broker 3]
    end

    TR --> OS1
    TR --> OS2
    TR --> OS3

    TR --> AS1
    TR --> AS2

    OS1 --> K1
    OS2 --> K2
    OS3 --> K3
```

**Scaling Rules:**
- Kafka partition count = maximum consumer count
- Each consumer in a consumer group consumes from one partition
- Same customerId always goes to the same partition (ordering guarantee)

---

## Next Steps

For more detailed information:
- **[Event-Driven Flows](03-event-driven-flows.md)** - Kafka, Outbox, Saga
- **[Database Design](04-database-design.md)** - Polyglot persistence
