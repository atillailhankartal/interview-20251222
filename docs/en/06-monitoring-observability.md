# Monitoring and Observability

## Overview

The system provides comprehensive monitoring and observability using the **LGTM Stack** (Loki, Grafana, Tempo, Mimir).

**Important:** Since OpenTelemetry log collection is problematic, logs are sent directly to Loki using **Loki4j**.

---

## Observability Stack

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

    subgraph LogCollection["Log Collection"]
        LOKI4J[Loki4j Appender]
    end

    subgraph TraceMetricCollection["Trace and Metric Collection"]
        OTEL[OpenTelemetry Collector]
    end

    subgraph Storage["Data Storage"]
        LOKI[(Loki<br/>Logs)]
        TEMPO[(Tempo<br/>Traces)]
        MIMIR[(Mimir<br/>Metrics)]
    end

    subgraph Visualization["Visualization"]
        GRAFANA[Grafana]
    end

    Services -->|Logback + Loki4j| LOKI4J
    Services -->|OTLP| OTEL

    LOKI4J -->|HTTP Push| LOKI
    OTEL --> TEMPO
    OTEL --> MIMIR

    LOKI --> GRAFANA
    TEMPO --> GRAFANA
    MIMIR --> GRAFANA
```

### Why Direct Loki4j?

```mermaid
flowchart LR
    subgraph Problem["OpenTelemetry Log Issue"]
        P1["OTEL Log Collection<br/>not yet stable"]
        P2["Some log loss<br/>may occur"]
        P3["Complexity increase"]
    end

    subgraph Solution["Solution: Loki4j"]
        S1["Logback native"]
        S2["Direct push to Loki"]
        S3["Async + reliable"]
        S4["Less complexity"]
    end

    Problem -->|Alternative| Solution
```

### Signal Paths

```mermaid
flowchart TB
    subgraph App["Spring Boot Application"]
        LOG[Logger.info]
        TRACE[Span]
        METRIC[Counter/Gauge]
    end

    subgraph Transport["Transport"]
        LOKI4J["Loki4j<br/>HTTP Push"]
        OTEL_TRACE["OTEL Exporter<br/>gRPC 4317"]
        OTEL_METRIC["Micrometer OTLP<br/>gRPC 4317"]
    end

    subgraph Destination["Destination"]
        LOKI[(Loki 3100)]
        TEMPO[(Tempo via OTEL)]
        MIMIR[(Mimir via OTEL)]
    end

    LOG --> LOKI4J
    TRACE --> OTEL_TRACE
    METRIC --> OTEL_METRIC

    LOKI4J --> LOKI
    OTEL_TRACE --> TEMPO
    OTEL_METRIC --> MIMIR

    style LOKI4J fill:#9f9
    style OTEL_TRACE fill:#99f
    style OTEL_METRIC fill:#99f
```

---

## Three Core Signals

```mermaid
flowchart LR
    subgraph Logs["Logs"]
        L1[Application Logs]
        L2[Error Details]
        L3[Audit Trail]
    end

    subgraph Metrics["Metrics"]
        M1[Request Count]
        M2[Response Time]
        M3[Error Rate]
    end

    subgraph Traces["Traces"]
        T1[Request Flow]
        T2[Service Dependencies]
        T3[Latency Analysis]
    end

    Logs --> WHAT[What Happened?]
    Metrics --> HOW[How's Performance?]
    Traces --> WHERE[Where Time Spent?]
```

---

## Log Management (Loki)

### Log Flow

```mermaid
sequenceDiagram
    participant A as Application
    participant LB as Logback
    participant L as Loki
    participant G as Grafana

    A->>LB: log.info("Order created")
    LB->>LB: JSON Formatting
    LB->>L: Push (async)
    L->>L: Label Index
    L->>L: Compressed Storage

    Note over G,L: Querying

    G->>L: LogQL Query
    L-->>G: Log Results
```

### Structured Log Format

```mermaid
flowchart LR
    subgraph LogEntry["Log Entry"]
        TS["timestamp<br/>2024-01-15T10:30:00"]
        LVL["level<br/>INFO"]
        MSG["message<br/>Order created"]
        TID["traceId<br/>abc123"]
        SID["spanId<br/>def456"]
        CTX["context<br/>orderId, customerId"]
    end

    subgraph Labels["Loki Labels"]
        APP["app=order-service"]
        ENV["env=production"]
        LEV["level=INFO"]
    end

    LogEntry --> Labels
```

### Log Levels

```mermaid
flowchart TB
    subgraph Levels["Log Levels"]
        ERROR["ERROR<br/>Error conditions"]
        WARN["WARN<br/>Warning conditions"]
        INFO["INFO<br/>Normal operations"]
        DEBUG["DEBUG<br/>Detailed information"]
    end

    subgraph Usage["Usage"]
        E1[Exception caught]
        E2[Operation failed]
        W1[Retry in progress]
        W2[Limit approaching]
        I1[Order created]
        I2[Service started]
        D1[SQL query]
        D2[HTTP details]
    end

    ERROR --> E1
    ERROR --> E2
    WARN --> W1
    WARN --> W2
    INFO --> I1
    INFO --> I2
    DEBUG --> D1
    DEBUG --> D2
```

---

## Distributed Tracing (Tempo)

### Trace Structure

```mermaid
flowchart TB
    subgraph Trace["Trace (abc-123)"]
        subgraph Span1["Span: API Gateway"]
            S1["POST /api/orders<br/>12ms"]
        end

        subgraph Span2["Span: Order Service"]
            S2["createOrder()<br/>8ms"]
        end

        subgraph Span3["Span: Kafka Producer"]
            S3["send(OrderCreatedEvent)<br/>3ms"]
        end

        subgraph Span4["Span: Asset Service"]
            S4["reserveBalance()<br/>5ms"]
        end
    end

    S1 --> S2
    S2 --> S3
    S3 --> S4
```

### Cross-Service Trace Propagation

```mermaid
sequenceDiagram
    participant C as Client
    participant G as Gateway
    participant OS as Order Service
    participant K as Kafka
    participant AS as Asset Service

    Note over C,AS: TraceId: abc-123

    C->>G: POST /api/orders<br/>traceparent: abc-123
    activate G
    G->>OS: Forward<br/>traceparent: abc-123
    activate OS
    OS->>K: Publish Event<br/>traceparent header
    deactivate OS
    deactivate G

    K->>AS: Consume Event<br/>traceparent from header
    activate AS
    AS->>AS: Process
    deactivate AS

    Note over C,AS: All spans<br/>under same trace
```

---

## Metrics (Mimir)

### Key Performance Indicators (KPI)

```mermaid
flowchart TB
    subgraph RED["RED Methodology"]
        RATE["Rate<br/>Requests/second"]
        ERRORS["Errors<br/>Error rate"]
        DURATION["Duration<br/>Response time"]
    end

    subgraph Business["Business Metrics"]
        ORDERS["orders_created_total"]
        MATCHED["orders_matched_total"]
        VOLUME["order_volume_total"]
    end

    subgraph Technical["Technical Metrics"]
        CPU["cpu_usage_percent"]
        MEMORY["memory_usage_bytes"]
        CONN["db_connections_active"]
    end
```

### Metric Types

```mermaid
flowchart LR
    subgraph Counter["Counter"]
        C1["orders_created_total"]
        C2["errors_total"]
        C3["http_requests_total"]
    end

    subgraph Gauge["Gauge"]
        G1["orders_pending_count"]
        G2["memory_usage_bytes"]
        G3["db_connections_active"]
    end

    subgraph Histogram["Histogram"]
        H1["http_request_duration_seconds"]
        H2["order_processing_time"]
    end

    Counter -->|Increasing| INC[Total Counter]
    Gauge -->|Changing| VAL[Current Value]
    Histogram -->|Distribution| DIST[Percentile]
```

---

## Grafana Dashboards

### Business Metrics Dashboard

```mermaid
flowchart TB
    subgraph Dashboard["Brokage Operations Dashboard"]
        subgraph Row1["Summary Cards"]
            CARD1["Orders/s<br/>156"]
            CARD2["Error Rate<br/>0.02%"]
            CARD3["P99 Latency<br/>23ms"]
            CARD4["Active Orders<br/>1,234"]
        end

        subgraph Row2["Time Series"]
            GRAPH1["Order Count Timeline"]
            GRAPH2["Error Rate Timeline"]
        end

        subgraph Row3["Distributions"]
            PIE1["Order Status Distribution"]
            BAR1["Most Active Assets"]
        end
    end
```

### Technical Dashboard

```mermaid
flowchart TB
    subgraph TechDashboard["Technical Health Dashboard"]
        subgraph Services["Service Health"]
            SVC1["Order Service: UP"]
            SVC2["Asset Service: UP"]
            SVC3["Notification: UP"]
        end

        subgraph Resources["Resource Usage"]
            CPU["CPU: 45%"]
            MEM["Memory: 62%"]
            DISK["Disk: 28%"]
        end

        subgraph Kafka["Kafka Metrics"]
            LAG["Consumer Lag"]
            THROUGHPUT["Message Throughput"]
            PARTITIONS["Partition Health"]
        end
    end
```

---

## Alerting

### Alert Rules

```mermaid
flowchart TB
    subgraph Rules["Alert Rules"]
        R1["Error Rate > 5%<br/>for 5 minutes"]
        R2["P99 Latency > 500ms<br/>for 3 minutes"]
        R3["Service DOWN<br/>for 1 minute"]
        R4["Consumer Lag > 1000<br/>for 5 minutes"]
    end

    subgraph Severity["Severity"]
        CRITICAL["Critical"]
        WARNING["Warning"]
        INFO["Info"]
    end

    subgraph Channels["Notification Channels"]
        SLACK[Slack]
        EMAIL[Email]
        PAGER[PagerDuty]
    end

    R1 --> CRITICAL
    R2 --> WARNING
    R3 --> CRITICAL
    R4 --> WARNING

    CRITICAL --> PAGER
    CRITICAL --> SLACK
    WARNING --> SLACK
    WARNING --> EMAIL
```

### Alert Flow

```mermaid
sequenceDiagram
    participant M as Mimir
    participant A as Alertmanager
    participant S as Slack
    participant O as Ops Team

    M->>M: Metric Check
    M->>A: Alert Trigger

    A->>A: Grouping
    A->>A: Inhibition Check
    A->>A: Silence Check

    A->>S: Send Notification
    S->>O: Alert Message

    O->>O: Investigation
    O->>A: Acknowledge
```

---

## SLI / SLO

### Service Level Indicators

```mermaid
flowchart LR
    subgraph SLIs["Service Level Indicators"]
        SLI1["Availability<br/>Service accessibility"]
        SLI2["Latency<br/>Response time"]
        SLI3["Error Rate<br/>Error rate"]
        SLI4["Throughput<br/>Transaction capacity"]
    end

    subgraph Measurement["Measurement"]
        M1["uptime / total_time"]
        M2["p99(response_time)"]
        M3["errors / requests"]
        M4["requests / second"]
    end

    SLI1 --> M1
    SLI2 --> M2
    SLI3 --> M3
    SLI4 --> M4
```

### Service Level Objectives

```mermaid
flowchart TB
    subgraph SLOs["Objectives"]
        SLO1["Availability > 99.9%<br/>43min downtime/month"]
        SLO2["P99 Latency < 200ms<br/>99% of requests"]
        SLO3["Error Rate < 0.1%<br/>99.9% success"]
    end

    subgraph Budget["Error Budget"]
        B1["Monthly: 43 minutes"]
        B2["Weekly: ~10 minutes"]
        B3["Daily: ~1.4 minutes"]
    end

    SLO1 --> B1
    B1 --> B2
    B2 --> B3
```

---

## Debugging Flow

### Problem Detection

```mermaid
flowchart TB
    subgraph Trigger["Trigger"]
        ALERT[Alert]
        USER[User Complaint]
        METRIC[Metric Anomaly]
    end

    subgraph Investigation["Investigation"]
        DASH[Dashboard Check]
        TRACE[Trace Analysis]
        LOG[Log Inspection]
    end

    subgraph RCA["Root Cause"]
        DB[Database Slow]
        NET[Network Issue]
        CODE[Code Bug]
        RES[Resource Shortage]
    end

    subgraph Action["Action"]
        HOTFIX[Hotfix]
        SCALE[Scaling]
        ROLLBACK[Rollback]
        CONFIG[Configuration]
    end

    Trigger --> DASH
    DASH --> TRACE
    TRACE --> LOG
    LOG --> RCA
    RCA --> Action
```

### Trace to Log Navigation

```mermaid
sequenceDiagram
    participant G as Grafana
    participant T as Tempo
    participant L as Loki

    Note over G: Slow request detected

    G->>T: Get trace (abc-123)
    T-->>G: Trace details

    Note over G: Found slow span<br/>Asset Service - 5 seconds

    G->>L: Get logs<br/>traceId=abc-123
    L-->>G: Log records

    Note over G: Error found in log<br/>"Database connection timeout"
```

---

## LGTM All-in-One

Simplified configuration for interview:

```mermaid
flowchart LR
    subgraph Services["Services"]
        SVC[Microservices]
    end

    subgraph LGTM["LGTM Container"]
        L[Loki]
        G[Grafana]
        T[Tempo]
        M[Mimir]
        OTEL[OTEL Collector]
    end

    SVC -->|4317/4318| LGTM
    G -->|3000| USER[User]
```

**Advantages:**
- Single container
- Quick setup
- Sufficient for interview

---

## Important Metrics

```mermaid
mindmap
    root((Monitoring))
        Business
            Order count
            Match rate
            Transaction volume
            Cancellation rate
        Technical
            CPU usage
            Memory usage
            Disk I/O
            Network
        Application
            Requests/second
            Error rate
            Response time
            Queue size
        Infrastructure
            Container health
            Pod count
            DB connections
            Kafka lag
```

---

## Conclusion

This documentation series covered the following topics:

1. **System Overview** - Architecture and concepts
2. **Microservice Architecture** - Service boundaries and communication
3. **Event-Driven Flows** - Kafka, Saga, Idempotency
4. **Database Design** - Polyglot persistence
5. **API Gateway and Security** - Traefik, Keycloak
6. **Monitoring and Observability** - LGTM Stack
