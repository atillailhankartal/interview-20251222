# Monitoring ve Observability

## Genel Bakis

Sistem, **LGTM Stack** (Loki, Grafana, Tempo, Mimir) kullanarak kapsamli izleme ve gozlemlenebilirlik saglar.

**Onemli:** OpenTelemetry log collection sorunlu oldugu icin loglar **Loki4j** ile direkt Loki'ye gonderilir.

---

## Observability Stack

```mermaid
flowchart TB
    subgraph Services["Mikroservisler"]
        OS[Order Service]
        AS[Asset Service]
        CS[Customer Service]
        OP[Order Processor]
        NS[Notification Service]
        AU[Audit Service]
    end

    subgraph LogCollection["Log Toplama"]
        LOKI4J[Loki4j Appender]
    end

    subgraph TraceMetricCollection["Trace ve Metrik Toplama"]
        OTEL[OpenTelemetry Collector]
    end

    subgraph Storage["Veri Depolama"]
        LOKI[(Loki<br/>Loglar)]
        TEMPO[(Tempo<br/>Trace'ler)]
        MIMIR[(Mimir<br/>Metrikler)]
    end

    subgraph Visualization["Gorsellestirme"]
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

### Neden Loki4j Direkt?

```mermaid
flowchart LR
    subgraph Problem["OpenTelemetry Log Sorunu"]
        P1["OTEL Log Collection<br/>henuz stabil degil"]
        P2["Bazi log kaybi<br/>yasanabiliyor"]
        P3["Complexity artisi"]
    end

    subgraph Solution["Cozum: Loki4j"]
        S1["Logback native"]
        S2["Direkt Loki'ye push"]
        S3["Async + reliable"]
        S4["Daha az complexity"]
    end

    Problem -->|Alternatif| Solution
```

### Sinyal Yollari

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

    subgraph Destination["Hedef"]
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

## Uc Temel Sinyal

```mermaid
flowchart LR
    subgraph Logs["Loglar"]
        L1[Uygulama Loglari]
        L2[Hata Detaylari]
        L3[Audit Trail]
    end

    subgraph Metrics["Metrikler"]
        M1[Istek Sayisi]
        M2[Yanit Suresi]
        M3[Hata Orani]
    end

    subgraph Traces["Trace'ler"]
        T1[Istek Akisi]
        T2[Servis Baglantilari]
        T3[Gecikme Analizi]
    end

    Logs --> WHAT[Ne Oldu?]
    Metrics --> HOW[Nasil Performans?]
    Traces --> WHERE[Nerede Zaman Harcandi?]
```

---

## Log Yonetimi (Loki)

### Log Akisi

```mermaid
sequenceDiagram
    participant A as Uygulama
    participant LB as Logback
    participant L as Loki
    participant G as Grafana

    A->>LB: log.info("Emir olusturuldu")
    LB->>LB: JSON Formatlama
    LB->>L: Push (async)
    L->>L: Label Index
    L->>L: Compressed Storage

    Note over G,L: Sorgulama

    G->>L: LogQL Query
    L-->>G: Log Sonuclari
```

### Yapilandirilmis Log Formati

```mermaid
flowchart LR
    subgraph LogEntry["Log Kaydi"]
        TS["timestamp<br/>2024-01-15T10:30:00"]
        LVL["level<br/>INFO"]
        MSG["message<br/>Emir olusturuldu"]
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

### Log Seviyeleri

```mermaid
flowchart TB
    subgraph Levels["Log Seviyeleri"]
        ERROR["ERROR<br/>Hata durumları"]
        WARN["WARN<br/>Dikkat gerektiren"]
        INFO["INFO<br/>Normal islemler"]
        DEBUG["DEBUG<br/>Detayli bilgi"]
    end

    subgraph Usage["Kullanim"]
        E1[Exception yakalandi]
        E2[Islem basarisiz]
        W1[Retry yapiliyor]
        W2[Limit yaklasti]
        I1[Emir olusturuldu]
        I2[Servis basladi]
        D1[SQL sorgusu]
        D2[HTTP detaylari]
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

### Trace Yapisi

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

### Servisler Arasi Trace Propagation

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

    Note over C,AS: Tum span'lar<br/>ayni trace altinda
```

---

## Metrikler (Mimir)

### Anahtar Performans Gostergeleri (KPI)

```mermaid
flowchart TB
    subgraph RED["RED Metodolojisi"]
        RATE["Rate<br/>Istek/saniye"]
        ERRORS["Errors<br/>Hata orani"]
        DURATION["Duration<br/>Yanit suresi"]
    end

    subgraph Business["Is Metrikleri"]
        ORDERS["orders_created_total"]
        MATCHED["orders_matched_total"]
        VOLUME["order_volume_total"]
    end

    subgraph Technical["Teknik Metrikler"]
        CPU["cpu_usage_percent"]
        MEMORY["memory_usage_bytes"]
        CONN["db_connections_active"]
    end
```

### Metrik Tipleri

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

    Counter -->|Artan| INC[Toplam Sayac]
    Gauge -->|Degisen| VAL[Anlik Deger]
    Histogram -->|Dagilim| DIST[Yuzdelik Dilim]
```

---

## Grafana Dashboard'lari

### Is Metrikleri Dashboard

```mermaid
flowchart TB
    subgraph Dashboard["Brokage Operations Dashboard"]
        subgraph Row1["Ozet Kartlar"]
            CARD1["Emir/sn<br/>156"]
            CARD2["Hata Orani<br/>0.02%"]
            CARD3["P99 Latency<br/>23ms"]
            CARD4["Aktif Emirler<br/>1,234"]
        end

        subgraph Row2["Zaman Serisi"]
            GRAPH1["Emir Sayisi Timeline"]
            GRAPH2["Hata Orani Timeline"]
        end

        subgraph Row3["Dagilimlar"]
            PIE1["Emir Status Dagilimi"]
            BAR1["En Aktif Varliklar"]
        end
    end
```

### Teknik Dashboard

```mermaid
flowchart TB
    subgraph TechDashboard["Technical Health Dashboard"]
        subgraph Services["Servis Sagligi"]
            SVC1["Order Service: UP"]
            SVC2["Asset Service: UP"]
            SVC3["Notification: UP"]
        end

        subgraph Resources["Kaynak Kullanimi"]
            CPU["CPU: 45%"]
            MEM["Memory: 62%"]
            DISK["Disk: 28%"]
        end

        subgraph Kafka["Kafka Metrikleri"]
            LAG["Consumer Lag"]
            THROUGHPUT["Message Throughput"]
            PARTITIONS["Partition Health"]
        end
    end
```

---

## Alerting

### Alert Kurallari

```mermaid
flowchart TB
    subgraph Rules["Alert Kurallari"]
        R1["Hata Orani > %5<br/>5 dakika boyunca"]
        R2["P99 Latency > 500ms<br/>3 dakika boyunca"]
        R3["Servis DOWN<br/>1 dakika boyunca"]
        R4["Consumer Lag > 1000<br/>5 dakika boyunca"]
    end

    subgraph Severity["Oncelik"]
        CRITICAL["Critical"]
        WARNING["Warning"]
        INFO["Info"]
    end

    subgraph Channels["Bildirim Kanallari"]
        SLACK[Slack]
        EMAIL[E-posta]
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

### Alert Akisi

```mermaid
sequenceDiagram
    participant M as Mimir
    participant A as Alertmanager
    participant S as Slack
    participant O as Ops Team

    M->>M: Metrik Kontrolu
    M->>A: Alert Trigger

    A->>A: Gruplama
    A->>A: Inhibition Check
    A->>A: Silence Check

    A->>S: Bildirim Gonder
    S->>O: Alert Mesaji

    O->>O: Inceleme
    O->>A: Acknowledge
```

---

## SLI / SLO

### Service Level Indicators

```mermaid
flowchart LR
    subgraph SLIs["Service Level Indicators"]
        SLI1["Availability<br/>Servis erisilebilirligi"]
        SLI2["Latency<br/>Yanit suresi"]
        SLI3["Error Rate<br/>Hata orani"]
        SLI4["Throughput<br/>Islem kapasitesi"]
    end

    subgraph Measurement["Olcum"]
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
    subgraph SLOs["Hedefler"]
        SLO1["Availability > %99.9<br/>Aylik 43dk downtime"]
        SLO2["P99 Latency < 200ms<br/>%99 istekler"]
        SLO3["Error Rate < %0.1<br/>%99.9 basari"]
    end

    subgraph Budget["Hata Butcesi"]
        B1["Aylik: 43 dakika"]
        B2["Haftalik: ~10 dakika"]
        B3["Gunluk: ~1.4 dakika"]
    end

    SLO1 --> B1
    B1 --> B2
    B2 --> B3
```

---

## Hata Ayiklama Akisi

### Problem Tespit

```mermaid
flowchart TB
    subgraph Trigger["Tetikleyici"]
        ALERT[Alert]
        USER[Kullanici Sikayeti]
        METRIC[Metrik Anomalisi]
    end

    subgraph Investigation["Inceleme"]
        DASH[Dashboard Kontrolu]
        TRACE[Trace Analizi]
        LOG[Log Inceleme]
    end

    subgraph RCA["Kok Neden"]
        DB[Veritabani Yavas]
        NET[Ag Sorunu]
        CODE[Kod Hatasi]
        RES[Kaynak Yetersiz]
    end

    subgraph Action["Aksiyon"]
        HOTFIX[Hotfix]
        SCALE[Olcekleme]
        ROLLBACK[Rollback]
        CONFIG[Konfigurasyon]
    end

    Trigger --> DASH
    DASH --> TRACE
    TRACE --> LOG
    LOG --> RCA
    RCA --> Action
```

### Trace'den Log'a Gecis

```mermaid
sequenceDiagram
    participant G as Grafana
    participant T as Tempo
    participant L as Loki

    Note over G: Yavas istek tespit edildi

    G->>T: Trace'i getir (abc-123)
    T-->>G: Trace detaylari

    Note over G: Yavas span bulundu<br/>Asset Service - 5 saniye

    G->>L: Loglari getir<br/>traceId=abc-123
    L-->>G: Log kayitlari

    Note over G: Log'da hata bulundu<br/>"Database connection timeout"
```

---

## LGTM All-in-One

Interview icin basitlesilmis yapilandirma:

```mermaid
flowchart LR
    subgraph Services["Servisler"]
        SVC[Mikroservisler]
    end

    subgraph LGTM["LGTM Container"]
        L[Loki]
        G[Grafana]
        T[Tempo]
        M[Mimir]
        OTEL[OTEL Collector]
    end

    SVC -->|4317/4318| LGTM
    G -->|3000| USER[Kullanici]
```

**Avantajlari:**
- Tek container
- Hizli kurulum
- Interview icin yeterli

---

## Onemli Metrikler

```mermaid
mindmap
    root((Monitoring))
        Business
            Emir sayisi
            Eslestirme orani
            Islem hacmi
            Iptal orani
        Technical
            CPU kullanimi
            Memory kullanimi
            Disk I/O
            Network
        Application
            Istek/saniye
            Hata orani
            Yanit suresi
            Kuyruk boyutu
        Infrastructure
            Container sagligi
            Pod sayisi
            DB baglantilari
            Kafka lag
```

---

## Sonuc

Bu dokuman serisinde asagidaki konular ele alindi:

1. **Sistem Genel Bakis** - Mimari ve kavramlar
2. **Mikroservis Mimarisi** - Servis sinirlari ve iletisim
3. **Event-Driven Akislar** - Kafka, Saga, Idempotency
4. **Veritabani Tasarimi** - Polyglot persistence
5. **API Gateway ve Guvenlik** - Traefik, Keycloak
6. **Monitoring ve Observability** - LGTM Stack

 
