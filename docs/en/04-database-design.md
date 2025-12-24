# Database Design

## Overview

The system uses a **Polyglot Persistence** approach. Relational data is stored in PostgreSQL, and document data is stored in MongoDB. Each microservice has its own database.

---

## Database Strategy

```mermaid
flowchart TB
    subgraph Services["Microservices"]
        OS[Order Service]
        AS[Asset Service]
        CS[Customer Service]
        OP[Order Processor]
        NS[Notification Service]
        AU[Audit Service]
    end

    subgraph PostgreSQL["PostgreSQL Cluster"]
        direction TB
        DB1[(brokage_orders)]
        DB2[(brokage_assets)]
        DB3[(brokage_customers)]
        DB4[(brokage_processor)]
    end

    subgraph MongoDB["MongoDB"]
        direction TB
        M1[(notifications)]
        M2[(audit_logs)]
    end

    subgraph Redis["Redis"]
        direction TB
        R1[Idempotency Cache]
        R2[Distributed Lock]
        R3[Session Cache]
    end

    OS --> DB1
    AS --> DB2
    CS --> DB3
    OP --> DB4
    NS --> M1
    AU --> M2

    OS --> R1
    AS --> R2
    CS --> R3
```

---

## Why Polyglot Persistence?

```mermaid
flowchart LR
    subgraph Relational["Relational Data (PostgreSQL)"]
        T1[Transactional Operations]
        T2[ACID Guarantees]
        T3[Relational Queries]
        T4[Referential Integrity]
    end

    subgraph Document["Document Data (MongoDB)"]
        D1[Flexible Schema]
        D2[High Write Throughput]
        D3[TimeSeries Data]
        D4[Nested Documents]
    end

    subgraph Cache["Cache (Redis)"]
        C1[Low Latency]
        C2[TTL Support]
        C3[Distributed Lock]
        C4[Pub/Sub]
    end

    Relational --> USE1[Order, Asset, Customer]
    Document --> USE2[Audit, Notification, Telemetry]
    Cache --> USE3[Idempotency, Session, Lock]
```

---

## PostgreSQL Schemas

### Order Database (brokage_orders)

```mermaid
erDiagram
    orders {
        uuid id PK
        varchar client_order_id UK
        bigint customer_id FK
        varchar asset_name
        varchar order_side
        decimal size
        decimal price
        varchar status
        varchar rejection_reason
        timestamp created_at
        timestamp updated_at
        int version
    }

    outbox_events {
        uuid id PK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        jsonb payload
        timestamp created_at
        timestamp published_at
        int retry_count
        varchar status
    }

    orders ||--o{ outbox_events : "publishes"
```

### Asset Database (brokage_assets)

```mermaid
erDiagram
    assets {
        bigint id PK
        bigint customer_id FK
        varchar asset_name
        decimal size
        decimal usable_size
        timestamp created_at
        timestamp updated_at
        int version
    }

    asset_reservations {
        uuid id PK
        bigint asset_id FK
        uuid order_id
        decimal amount
        varchar status
        timestamp created_at
        timestamp expires_at
    }

    outbox_events {
        uuid id PK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        jsonb payload
        timestamp created_at
        timestamp published_at
        int retry_count
        varchar status
    }

    assets ||--o{ asset_reservations : "has"
    assets ||--o{ outbox_events : "publishes"
```

### Customer Database (brokage_customers)

```mermaid
erDiagram
    customers {
        bigint id PK
        varchar username UK
        varchar password_hash
        varchar email UK
        varchar phone
        varchar tier
        boolean active
        timestamp created_at
        timestamp updated_at
    }

    customer_tiers {
        varchar tier_code PK
        varchar tier_name
        int priority
        int rate_limit
        decimal monthly_fee
    }

    notification_preferences {
        bigint id PK
        bigint customer_id FK
        boolean email_enabled
        boolean sms_enabled
        boolean push_enabled
        jsonb preferences
    }

    customers ||--o| customer_tiers : "belongs_to"
    customers ||--|| notification_preferences : "has"
```

### Processor Database (brokage_processor)

```mermaid
erDiagram
    saga_instances {
        uuid saga_id PK
        varchar saga_type
        varchar aggregate_id
        varchar current_state
        jsonb payload
        timestamp created_at
        timestamp updated_at
        int retry_count
    }

    processed_events {
        uuid event_id PK
        varchar event_type
        timestamp processed_at
        jsonb result
    }

    matching_queue {
        uuid id PK
        uuid order_id
        varchar customer_tier
        decimal price
        timestamp created_at
        int priority
    }
```

---

## MongoDB Schemas

### Notification Collection

```mermaid
erDiagram
    notifications {
        objectid _id PK
        string notification_id UK
        long customer_id
        string type
        string title
        string message
        object channels
        string read_status
        date read_at
        string priority
        string category
        object related_entity
        date created_at
        date updated_at
        date expires_at
    }
```

**Channels Structure:**

```mermaid
flowchart TB
    subgraph Channels["channels object"]
        EMAIL["email<br/>status, sentAt, recipient, retryCount"]
        SMS["sms<br/>status, reason"]
        PUSH["push<br/>status, sentAt, deviceTokens"]
        WS["websocket<br/>status, deliveredAt"]
    end
```

### Audit Logs Collection

```mermaid
erDiagram
    audit_logs {
        objectid _id PK
        date timestamp
        string action
        long user_id
        string entity_type
        string entity_id
        object old_value
        object new_value
        object metadata
    }

    telemetry {
        objectid _id PK
        date timestamp
        string endpoint
        string method
        int response_time
        int status_code
        long user_id
        object headers
    }
```

---

## Relationships and Data Flow

### Inter-Service Data Relationships

```mermaid
flowchart TB
    subgraph OrderService["Order Service"]
        O_DB[(orders)]
    end

    subgraph AssetService["Asset Service"]
        A_DB[(assets)]
    end

    subgraph CustomerService["Customer Service"]
        C_DB[(customers)]
    end

    subgraph Kafka["Kafka Events"]
        E1[OrderCreatedEvent]
        E2[AssetReservedEvent]
        E3[CustomerUpdatedEvent]
    end

    O_DB -->|customerId reference| C_DB
    O_DB -->|assetName reference| A_DB
    A_DB -->|customerId reference| C_DB

    O_DB --> E1
    A_DB --> E2
    C_DB --> E3

    Note1[Services cannot access<br/>each other's DB]
    Note2[References are<br/>ID-based]
```

**Important Rule:** There is no direct database access between services. All data sharing is done through events.

---

## Indexes and Performance

### PostgreSQL Indexes

```mermaid
flowchart TB
    subgraph OrderIndexes["Order Table Indexes"]
        I1["idx_orders_customer_date<br/>(customer_id, created_at DESC)"]
        I2["idx_orders_status<br/>(status) WHERE status = 'PENDING'"]
        I3["idx_orders_client_order_id<br/>(client_order_id) UNIQUE"]
    end

    subgraph AssetIndexes["Asset Table Indexes"]
        I4["idx_assets_customer_name<br/>(customer_id, asset_name) UNIQUE"]
        I5["idx_assets_low_balance<br/>(customer_id) WHERE usable_size < size * 0.1"]
    end

    subgraph OutboxIndexes["Outbox Table Indexes"]
        I6["idx_outbox_pending<br/>(status, created_at) WHERE status = 'PENDING'"]
    end
```

### MongoDB Indexes

```mermaid
flowchart TB
    subgraph NotificationIndexes["Notification Indexes"]
        M1["{ customerId: 1, createdAt: -1 }"]
        M2["{ customerId: 1, readStatus: 1 }"]
        M3["{ channels.email.status: 1 }"]
        M4["{ expiresAt: 1 } TTL"]
    end

    subgraph AuditIndexes["Audit Indexes"]
        M5["{ timestamp: 1 } TimeSeries"]
        M6["{ userId: 1, timestamp: -1 }"]
        M7["{ entityType: 1, entityId: 1 }"]
    end
```

---

## Profile-Based Configuration

```mermaid
flowchart TB
    subgraph Test["test Profile"]
        T_PG[H2 In-Memory]
        T_MG[Embedded MongoDB]
        T_RD[Embedded Redis]
    end

    subgraph Dev["dev Profile"]
        D_PG[PostgreSQL Docker]
        D_MG[MongoDB Docker]
        D_RD[Redis Docker]
    end

    subgraph Docker["docker Profile"]
        DO_PG[PostgreSQL Container]
        DO_MG[MongoDB Container]
        DO_RD[Redis Container]
    end

    subgraph Prod["prod Profile"]
        P_PG[PostgreSQL Cluster]
        P_MG[MongoDB Replica Set]
        P_RD[Redis Cluster]
    end

    T_PG -->|Fast Testing| CI[CI Pipeline]
    Dev -->|Local Development| DEV[Developer]
    Docker -->|Integration| INT[Integration Test]
    Prod -->|Live Environment| LIVE[Production]
```

---

## Optimistic Locking

Uses version field to prevent concurrent update conflicts.

```mermaid
sequenceDiagram
    participant T1 as Transaction 1
    participant DB as Database
    participant T2 as Transaction 2

    T1->>DB: SELECT * FROM assets WHERE id=1
    Note right of DB: version = 1

    T2->>DB: SELECT * FROM assets WHERE id=1
    Note right of DB: version = 1

    T1->>DB: UPDATE assets SET usable_size=80, version=2<br/>WHERE id=1 AND version=1
    DB-->>T1: 1 row updated

    T2->>DB: UPDATE assets SET usable_size=90, version=2<br/>WHERE id=1 AND version=1
    DB-->>T2: 0 rows updated

    Note over T2: OptimisticLockException!
    T2->>T2: Retry or Error
```

---

## Data Lifecycle

### Order Lifecycle

```mermaid
stateDiagram-v2
    [*] --> CREATED: Order Created
    CREATED --> PENDING_RESERVATION: Outbox Event

    PENDING_RESERVATION --> ASSET_RESERVED: Balance Blocked
    PENDING_RESERVATION --> REJECTED: Insufficient Balance

    ASSET_RESERVED --> ORDER_CONFIRMED: Confirmed

    ORDER_CONFIRMED --> MATCHED: Admin Matched
    ORDER_CONFIRMED --> CANCELED: Cancelled

    MATCHED --> ARCHIVED: After 90 days
    CANCELED --> ARCHIVED: After 90 days
    REJECTED --> ARCHIVED: After 30 days

    ARCHIVED --> [*]: Deleted
```

### Audit Log Lifecycle

```mermaid
stateDiagram-v2
    [*] --> CREATED: Event Received

    CREATED --> HOT: 0-7 days<br/>Fast access

    HOT --> WARM: 7-30 days<br/>Normal access

    WARM --> COLD: 30-365 days<br/>Archive

    COLD --> [*]: After 365 days<br/>Deleted by TTL
```

---

## Backup Strategy

```mermaid
flowchart TB
    subgraph Daily["Daily Backup"]
        PG_FULL[PostgreSQL Full Backup]
        MG_SNAP[MongoDB Snapshot]
    end

    subgraph Hourly["Hourly"]
        PG_WAL[PostgreSQL WAL]
        MG_OPLOG[MongoDB Oplog]
    end

    subgraph Storage["Storage"]
        S3[Object Storage]
        LOCAL[Local Disk]
    end

    PG_FULL --> S3
    MG_SNAP --> S3
    PG_WAL --> LOCAL
    MG_OPLOG --> LOCAL

    S3 -->|30 days| ARCHIVE[Archive]
    LOCAL -->|7 days| ROTATE[Rotation]
```

---

## Next Steps

- **[API Gateway and Security](05-api-gateway-security.md)** - Traefik and Keycloak
- **[Monitoring and Observability](06-monitoring-observability.md)** - LGTM Stack
